package Project.TestService;

import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.constants.RefundStatus;
import Project.paymentgatewaysystem.dto.RefundRequestDto;
import Project.paymentgatewaysystem.dto.RefundResponseDto;
import Project.paymentgatewaysystem.entity.*;
import Project.paymentgatewaysystem.exception.InvalidRefundException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.exception.UnauthorizedException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.repository.PaymentRepository;
import Project.paymentgatewaysystem.repository.RefundRepository;
import Project.paymentgatewaysystem.service.PaymentGateway;
import Project.paymentgatewaysystem.serviceImpl.RefundServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MerchantUserRepository merchantUserRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private RefundServiceImpl refundServiceImpl;
    private MerchantUser merchantUser;
    private Merchant merchant;
    private Payment payment;
    private Order order;

    private static final String Email = "test@example.com;";
    private static final String ikey = "ikey-123";
    private static final Long Payment_Id = 1L;
    private static final Long Merchant_Id = 10L;

    @BeforeEach
    void setUp() {
        merchant = new Merchant();
        merchant.setMerchantId(Merchant_Id);

        merchantUser = new MerchantUser();
        merchantUser.setMerchant(merchant);

        order = new Order();
        order.setMerchant(merchant);

        payment = new Payment();
        payment.setPaymentId(Payment_Id);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setOrder(order);


    }

    private RefundRequestDto buildRequest(String reason) {
        RefundRequestDto req = new RefundRequestDto();
        req.setIdempotencyKey(ikey);
        req.setPaymentId(Payment_Id);
        req.setReason(reason);
        return req;
    }

    @Test
    void createRefund_nullIdempotency() {
        RefundRequestDto request = buildRequest("DAMAGED");
        request.setIdempotencyKey(null);

        assertThrows(InvalidRefundException.class, () -> refundServiceImpl.createRefund(Email, request));
        verifyNoInteractions(refundRepository, paymentRepository, paymentGateway);
    }

    @Test
    void createRefund_blankIdempotency() {
        RefundRequestDto request = buildRequest("DAMAGED");
        request.setIdempotencyKey(null);

        assertThrows(InvalidRefundException.class, () -> refundServiceImpl.createRefund(Email, request));
    }

    @Test
    void createRefund_duplicateIdempotency() {
        Refund existing = new Refund();
        existing.setRefundId(99L);
        existing.setPayment(payment);
        existing.setAmount(new BigDecimal("100.00"));
        existing.setReason("DAMAGED");
        existing.setStatus(RefundStatus.SUCCESS);
        existing.setIdempotencyKey(ikey);
        existing.setCreatedAt(LocalDateTime.now());

        when(refundRepository.findByPayment_PaymentIdAndIdempotencyKey(Payment_Id, ikey)).thenReturn(Optional.of(existing));


        RefundResponseDto result = refundServiceImpl.createRefund(Email, buildRequest("DAMAGED"));

        assertThat(result.getRefundId()).isEqualTo(99L);
        verifyNoInteractions(paymentRepository, paymentGateway);
    }

    @Test
    void createRefund_paymentNotFound() {
        when(refundRepository.findByPayment_PaymentIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(merchantUserRepository.findByEmail(Email)).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(Payment_Id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> refundServiceImpl.createRefund(Email, buildRequest("DAMAGED")));
    }

    @Test
    void createRefund_unauthorizedMerchant() {
        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(999L);
        MerchantUser otherUser = new MerchantUser();
        otherUser.setMerchant(otherMerchant);

        when(refundRepository.findByPayment_PaymentIdAndIdempotencyKey(any(), any()))
                .thenReturn(Optional.empty());
        when(merchantUserRepository.findByEmail(Email)).thenReturn(Optional.of(otherUser));
        when(paymentRepository.findById(Payment_Id)).thenReturn(Optional.of(payment));

        assertThrows(UnauthorizedException.class,
                () -> refundServiceImpl.createRefund(Email, buildRequest("DAMAGED")));
    }

    @Test
    void createRefund_paymentStatusPending() {
        payment.setStatus(PaymentStatus.PENDING);

        stubCommonLookups();

        assertThrows(InvalidRefundException.class,
                () -> refundServiceImpl.createRefund(Email, buildRequest("DAMAGED")));
    }


    @Test
    void createRefund_paymentPartiallyRefunded() {
        payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);

        stubCommonLookups();
        when(refundRepository.sumRefundedAmount(Payment_Id)).thenReturn(BigDecimal.ZERO);
        when(paymentGateway.refund(any(), any())).thenReturn(true);
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefundResponseDto result = refundServiceImpl.createRefund(Email, buildRequest("DAMAGED"));

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void createRefund_reasonDamaged_calculatesFullAmount() {
        stubCommonLookups();
        stubZeroExistingRefunds();
        when(paymentGateway.refund(any(), eq(new BigDecimal("100.00")))).thenReturn(true);
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefundResponseDto result = refundServiceImpl.createRefund(Email, buildRequest("DAMAGED"));

        assertThat(result.getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void createRefund_reasonMinorDamage_calculatesTenPercent() {
        stubCommonLookups();
        stubZeroExistingRefunds();
        when(paymentGateway.refund(any(), any())).thenReturn(true);
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefundResponseDto result = refundServiceImpl.createRefund(Email, buildRequest("MINOR_DAMAGE"));

        assertThat(result.getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void createRefund_reasonCustomerRequest_calculatesFullAmount() {
        stubCommonLookups();
        stubZeroExistingRefunds();
        when(paymentGateway.refund(any(), eq(new BigDecimal("100.00")))).thenReturn(true);
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefundResponseDto result = refundServiceImpl.createRefund(Email, buildRequest("CUSTOMER_REQUEST"));

        assertThat(result.getAmount()).isEqualByComparingTo("100.00");

    }


    @Test
    void createRefund_nullReason_throwsInvalidRefundException() {
        stubCommonLookups();
        stubZeroExistingRefunds();

        assertThrows(InvalidRefundException.class,
                () -> refundServiceImpl.createRefund(Email, buildRequest(null)));
    }


    @Test
    void createRefund_unknownReason_throwsInvalidRefundException() {
        stubCommonLookups();
        stubZeroExistingRefunds();

        assertThrows(InvalidRefundException.class,
                () -> refundServiceImpl.createRefund(Email, buildRequest("WRONG_REASON")));
    }

    @Test
    void createRefund_exceedsPaymentAmount_throwsInvalidRefundException() {
        stubCommonLookups();
        when(refundRepository.sumRefundedAmount(Payment_Id))
                .thenReturn(new BigDecimal("90.00"));

        assertThrows(InvalidRefundException.class,
                () -> refundServiceImpl.createRefund(Email, buildRequest("DAMAGED")));
    }

    @Test
    void createRefund_sumRefundedAmountNull_treatedAsZero() {
        stubCommonLookups();
        when(refundRepository.sumRefundedAmount(Payment_Id)).thenReturn(null);
        when(paymentGateway.refund(any(), any())).thenReturn(true);
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> refundServiceImpl.createRefund(Email, buildRequest("DAMAGED")));
    }

    @Test
    void createRefund_fullRefundSuccess_setsPaymentStatusRefunded() {
        stubCommonLookups();
        stubZeroExistingRefunds();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        List<Refund> savedRefunds = new ArrayList<>();

        when(refundRepository.save(any()))
                .thenAnswer(inv -> {
                    Refund original = inv.getArgument(0);

                    Refund copy = new Refund();
                    copy.setStatus(original.getStatus());
                    copy.setAmount(original.getAmount());
                    copy.setReason(original.getReason());
                    copy.setIdempotencyKey(original.getIdempotencyKey());
                    copy.setPayment(original.getPayment());

                    savedRefunds.add(copy);

                    return original;
                });

        when(paymentGateway.refund(any(), any())).thenReturn(true);

        refundServiceImpl.createRefund(Email, buildRequest("DAMAGED"));

        verify(paymentRepository).save(paymentCaptor.capture());

        assertThat(paymentCaptor.getValue().getStatus())
                .isEqualTo(PaymentStatus.REFUNDED);

        assertThat(savedRefunds).hasSize(2);
        assertThat(savedRefunds.get(0).getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(savedRefunds.get(1).getStatus()).isEqualTo(RefundStatus.SUCCESS);
    }
    @Test
    void createRefund_partialRefundSuccess_setsPaymentStatusPartiallyRefunded() {
        stubCommonLookups();
        when(refundRepository.sumRefundedAmount(Payment_Id)).thenReturn(BigDecimal.ZERO);
        when(paymentGateway.refund(any(), any())).thenReturn(true);
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);

        refundServiceImpl.createRefund(Email, buildRequest("MINOR_DAMAGE"));

        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }
    @Test
    void createRefund_gatewayFailure_refundStatusFailed() {
        stubCommonLookups();
        stubZeroExistingRefunds();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        List<Refund> savedRefunds = new ArrayList<>();

        when(refundRepository.save(any()))
                .thenAnswer(inv -> {
                    Refund original = inv.getArgument(0);

                    Refund copy = new Refund();
                    copy.setStatus(original.getStatus());
                    copy.setAmount(original.getAmount());
                    copy.setReason(original.getReason());
                    copy.setIdempotencyKey(original.getIdempotencyKey());
                    copy.setPayment(original.getPayment());

                    savedRefunds.add(copy);

                    return original;
                });


        when(paymentGateway.refund(any(), any())).thenReturn(false);

        refundServiceImpl.createRefund(Email, buildRequest("DAMAGED"));

        verify(paymentRepository).save(paymentCaptor.capture());

        assertThat(paymentCaptor.getValue().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(savedRefunds).hasSize(2);
        assertThat(savedRefunds.get(0).getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(savedRefunds.get(1).getStatus()).isEqualTo(RefundStatus.FAILED);
    }
    @Test
    void createRefund_success_dtoFieldsMappedCorrectly() {
        stubCommonLookups();
        stubZeroExistingRefunds();
        when(paymentGateway.refund(any(), any())).thenReturn(true);
        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefundResponseDto dto = refundServiceImpl.createRefund(Email, buildRequest("DAMAGED"));

        assertThat(dto.getPaymentId()).isEqualTo(Payment_Id);
        assertThat(dto.getAmount()).isEqualByComparingTo("100.00");
        assertThat(dto.getStatus()).isEqualTo("SUCCESS");
        assertThat(dto.getReason()).isEqualTo("DAMAGED");
        assertThat(dto.getIdempotencyKey()).isEqualTo(ikey);
        assertThat(dto.getCreatedAt()).isNotNull();
    }

    private void stubCommonLookups() {
        when(refundRepository.findByPayment_PaymentIdAndIdempotencyKey(Payment_Id, ikey))
                .thenReturn(Optional.empty());
        when(merchantUserRepository.findByEmail(Email))
                .thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(Payment_Id))
                .thenReturn(Optional.of(payment));
    }

    private void stubZeroExistingRefunds() {
        lenient().when(refundRepository.sumRefundedAmount(Payment_Id))
                .thenReturn(BigDecimal.ZERO);
    }



}
