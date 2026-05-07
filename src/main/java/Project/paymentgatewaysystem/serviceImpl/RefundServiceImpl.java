package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.constants.RefundStatus;
import Project.paymentgatewaysystem.dto.RefundRequestDto;
import Project.paymentgatewaysystem.dto.RefundResponseDto;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.entity.Payment;
import Project.paymentgatewaysystem.entity.Refund;
import Project.paymentgatewaysystem.exception.InvalidRefundException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.exception.UnauthorizedException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.repository.PaymentRepository;
import Project.paymentgatewaysystem.repository.RefundRepository;
import Project.paymentgatewaysystem.service.PaymentGateway;
import Project.paymentgatewaysystem.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class  RefundServiceImpl implements RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final PaymentGateway paymentGateway;
    private MerchantUser getUser(String email) {
        return merchantUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public RefundResponseDto createRefund(String email, RefundRequestDto request) {

        if(request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()){
            throw new InvalidRefundException("IdempotencyKey is required");
        }

        Optional<Refund> existing =
               refundRepository.findByPayment_PaymentIdAndIdempotencyKey(
                        request.getPaymentId(),
                        request.getIdempotencyKey()
                );
        if(existing.isPresent()){
            return toDto(existing.get());
        }

        MerchantUser user = getUser(email);

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));


        if (!payment.getOrder().getMerchant().getMerchantId()
                .equals(user.getMerchant().getMerchantId())) {
            throw new UnauthorizedException("Access denied");
        }

        if (!(payment.getStatus() == PaymentStatus.SUCCESS ||
                payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED)) {
            throw new InvalidRefundException("Refund not allowed");
        }


        BigDecimal requestAmount = calculateRefundAmount(payment, request.getReason());
        BigDecimal totalRefund = Optional.ofNullable(
                refundRepository.sumRefundedAmount(payment.getPaymentId())
                ).orElse(BigDecimal.ZERO);

        BigDecimal newTotal = totalRefund.add(requestAmount);

        if (newTotal.compareTo(payment.getAmount()) > 0) {
            throw new InvalidRefundException("Refund exceeds payment amount");
        }



        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(requestAmount);
        refund.setStatus(RefundStatus.PENDING);
        refund.setReason(request.getReason());
        refund.setIdempotencyKey(request.getIdempotencyKey());
        refund.setRefundReference(UUID.randomUUID().toString());
        refund.setCreatedAt(LocalDateTime.now());

        refundRepository.save(refund);

        log.info("Refund initiated for payment {}", payment.getPaymentId());


        boolean success = paymentGateway.refund(payment, requestAmount);

        refund.setStatus(success ? RefundStatus.SUCCESS : RefundStatus.FAILED);
        refundRepository.save(refund);

        if (success) {
            if (newTotal.compareTo(payment.getAmount()) == 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
        } else {
            log.error("Refund failed for payment {}", payment.getPaymentId());
        }
        paymentRepository.save(payment);

        return toDto(refund);
    }
    private BigDecimal calculateRefundAmount(Payment payment, String reason) {

        if (reason == null) {
            throw new InvalidRefundException("Refund reason required");
        }

        switch (reason.toUpperCase()) {

            case "DAMAGED":
            case "DEFECTIVE":
                return payment.getAmount();

            case "MINOR_DAMAGE":
                return payment.getAmount().multiply(new BigDecimal("0.10"));

            case "CUSTOMER_REQUEST":
                return payment.getAmount();

            default:
                throw new InvalidRefundException("Invalid refund reason");
        }
    }
    private RefundResponseDto toDto(Refund refund){
        RefundResponseDto dto = new RefundResponseDto();
        dto.setRefundId(refund.getRefundId());
        dto.setPaymentId(refund.getPayment().getPaymentId());
        dto.setAmount(refund.getAmount());
        dto.setStatus(refund.getStatus().name());
        dto.setReason(refund.getReason());
        dto.setIdempotencyKey(refund.getIdempotencyKey());
        dto.setCreatedAt(refund.getCreatedAt());
        return dto;

    }

}

