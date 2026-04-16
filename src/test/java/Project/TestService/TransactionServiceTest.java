package Project.TestService;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.constants.TransactionStatus;
import Project.paymentgatewaysystem.dto.TransactionRequestDto;
import Project.paymentgatewaysystem.dto.TransactionResponseDto;
import Project.paymentgatewaysystem.entity.*;
import Project.paymentgatewaysystem.exception.InvalidStateException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.exception.UnauthorizedException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.repository.OrderRepository;
import Project.paymentgatewaysystem.repository.PaymentRepository;
import Project.paymentgatewaysystem.repository.TransactionRepository;
import Project.paymentgatewaysystem.serviceImpl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MerchantUserRepository merchantUserRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private TransactionServiceImpl transactionServiceImpl;
    private MerchantUser merchantUser;
    private Merchant merchant;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp(){
        merchant = new Merchant();
        merchant.setMerchantId(1L);

        merchantUser = new MerchantUser();
        merchantUser.setEmail("test@example.com");
        merchantUser.setMerchant(merchant);

        order = new Order();
        order.setOrderId(10L);
        order.setAmount(new BigDecimal("100.00"));
        order.setMerchant(merchant);
        order.setStatus(OrderStatus.PENDING);

        payment = new Payment();
        payment.setPaymentId(100L);
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.NETBANKING);
    }

    private Transaction buildTransaction(TransactionStatus status){
        Transaction tx = new Transaction();
        tx.setTransactionId(99L);
        tx.setPayment(payment);
        tx.setAmount(order.getAmount());
        tx.setStatus(status);
        tx.setIdempotencyKey("ikey-123");
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }

    private TransactionRequestDto buildRequest(String idempotencyKey){
        TransactionRequestDto req = new TransactionRequestDto();
        req.setPaymentId(100L);
        req.setIdempotencyKey(idempotencyKey);
        return req;

    }

    @Test
    void processTransaction_duplicateIdempotencyKey(){
        Transaction existing = buildTransaction(TransactionStatus.SUCCESS);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        when(transactionRepository.findByPayment_PaymentIdAndIdempotencyKey(100L, "ikey-123")).thenReturn(Optional.of(existing));

        TransactionResponseDto result = transactionServiceImpl.processTransaction("test@example.com", buildRequest("ikey-123"));

        assertThat(result.getTransactionId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

        verify(transactionRepository, never()).save(any());

    }

    @Test
    void processPayment_differentMerchant(){
        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(99L);
        order.setMerchant(otherMerchant);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));

       assertThatThrownBy( () -> transactionServiceImpl.processTransaction("test@example.com", buildRequest(null)))
               .isInstanceOf(UnauthorizedException.class)
               .hasMessageContaining("Access denied");
    }

    @Test
    void processPayment_paymentAlreadySuccess(){
        payment.setStatus(PaymentStatus.SUCCESS);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        assertThatThrownBy(() ->
                transactionServiceImpl.processTransaction("test@example.com", buildRequest(null)))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Payment already completed");

    }

    @Test
    void processPayment_invalidPayment(){
        payment.setStatus(PaymentStatus.CANCELLED);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> transactionServiceImpl.processTransaction("test@example.com", buildRequest(null)))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Invalid payment state");
    }

    @Test
    void processTransaction_pendingPayment() {
        Transaction saved = buildTransaction(TransactionStatus.SUCCESS);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponseDto result = transactionServiceImpl
                .processTransaction("test@example.com", buildRequest("idem-key-123"));

        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(100L);
        assertThat(result.getOrderId()).isEqualTo(10L);
        assertThat(result.getAmount()).isEqualByComparingTo("100.00");

        verify(paymentRepository).save(payment);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void processTransaction_failedPayment(){
        payment.setStatus(PaymentStatus.FAILED);
        Transaction saved = buildTransaction(TransactionStatus.SUCCESS);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponseDto result = transactionServiceImpl.processTransaction("test@example.com", buildRequest(null));
        assertThat(result).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));

    }

    @Test
    void processPayment_simulationSuccess(){
        Transaction saved = buildTransaction(TransactionStatus.SUCCESS);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        transactionServiceImpl.processTransaction("test@example.com", buildRequest(null));
        verify(paymentRepository).save(payment);

    }

    @Test
    void processPayment_paymentIdNotFound(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transactionServiceImpl.processTransaction("test@example.com", buildRequest(null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void processPayment_nullIdempotencyKey(){
        Transaction saved = buildTransaction(TransactionStatus.SUCCESS);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        transactionServiceImpl.processTransaction("test@example.com", buildRequest(null));

        verify(transactionRepository, never()).findByPayment_PaymentIdAndIdempotencyKey(anyLong(), any());

    }
    @Test
    void getById_validTransaction(){
        Transaction tx = buildTransaction(TransactionStatus.SUCCESS);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(transactionRepository.findById(999L)).thenReturn(Optional.of(tx));

        TransactionResponseDto result = transactionServiceImpl.getById("test@example.com", 999L);
        assertThat(result.getTransactionId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

    }

    @Test
    void getById_notFound(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionServiceImpl.getById("test@example.com", 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void getById_differentMerchant(){
        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(99L);

        Order otherOrder = new Order();
        otherOrder.setOrderId(20L);
        otherOrder.setAmount(new BigDecimal("100.00"));
        otherOrder.setMerchant(otherMerchant);
        otherOrder.setStatus(OrderStatus.PENDING);

        Payment otherPayment = new Payment();
        otherPayment.setPaymentId(100L);
        otherPayment.setOrder(otherOrder);
        otherPayment.setStatus(PaymentStatus.PENDING);
        otherPayment.setPaymentMethod(PaymentMethod.NETBANKING);

        Transaction tx = new Transaction();
        tx.setTransactionId(99L);
        tx.setPayment(otherPayment);
        tx.setAmount(otherOrder.getAmount());
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setIdempotencyKey("ikey-123");
        tx.setCreatedAt(LocalDateTime.now());
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(transactionRepository.findById(999L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionServiceImpl.getById("test@example.com", 999L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getByPaymentId_validPayment(){
        Transaction tx = buildTransaction(TransactionStatus.SUCCESS);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(transactionRepository.findByPayment_PaymentId(100L))
                .thenReturn(List.of(tx));
        List<TransactionResponseDto> result =
                transactionServiceImpl.getByPaymentId("test@example.com", 100L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionId()).isEqualTo(99L);
    }
    @Test
    void getByPaymentId_noTransactions_shouldThrowException() {

        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(merchantUser));

        when(transactionRepository.findByPayment_PaymentId(100L))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() ->
                transactionServiceImpl.getByPaymentId("test@example.com", 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No transactions found");
    }
    @Test
    void getByPaymentId_differentMerchant_shouldReturnEmptyList() {

        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(999L);

        Order otherOrder = new Order();
        otherOrder.setOrderId(20L);
        otherOrder.setAmount(new BigDecimal("100.00"));
        otherOrder.setMerchant(otherMerchant);
        otherOrder.setStatus(OrderStatus.PENDING);

        Payment otherPayment = new Payment();
        otherPayment.setPaymentId(100L);
        otherPayment.setOrder(otherOrder);
        otherPayment.setStatus(PaymentStatus.PENDING);
        otherPayment.setPaymentMethod(PaymentMethod.NETBANKING);

        Transaction tx = new Transaction();
        tx.setTransactionId(88L);
        tx.setPayment(otherPayment);
        tx.setAmount(otherOrder.getAmount());
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setIdempotencyKey("ikey-456");
        tx.setCreatedAt(LocalDateTime.now());

        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(merchantUser));

        when(transactionRepository.findByPayment_PaymentId(100L))
                .thenReturn(List.of(tx));

        List<TransactionResponseDto> result =
                transactionServiceImpl.getByPaymentId("test@example.com", 100L);

        assertThat(result).isEmpty();
    }

    @Test
    void getByPaymentId_mixedTransactions() {

        Transaction validTx = buildTransaction(TransactionStatus.SUCCESS);


        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(999L);

        Order otherOrder = new Order();
        otherOrder.setOrderId(20L);
        otherOrder.setAmount(new BigDecimal("100.00"));
        otherOrder.setMerchant(otherMerchant);
        otherOrder.setStatus(OrderStatus.PENDING);

        Payment otherPayment = new Payment();
        otherPayment.setPaymentId(100L);
        otherPayment.setOrder(otherOrder);
        otherPayment.setStatus(PaymentStatus.PENDING);
        otherPayment.setPaymentMethod(PaymentMethod.NETBANKING);

        Transaction invalidTx = new Transaction();
        invalidTx.setTransactionId(88L);
        invalidTx.setPayment(otherPayment);
        invalidTx.setAmount(otherOrder.getAmount());
        invalidTx.setStatus(TransactionStatus.SUCCESS);
        invalidTx.setIdempotencyKey("ikey-456");
        invalidTx.setCreatedAt(LocalDateTime.now());

        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(merchantUser));

        when(transactionRepository.findByPayment_PaymentId(100L))
                .thenReturn(List.of(validTx, invalidTx));

        List<TransactionResponseDto> result =
                transactionServiceImpl.getByPaymentId("test@example.com", 100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionId()).isEqualTo(validTx.getTransactionId());
    }
    @Test
    void getByPaymentId_userNotFound() {
        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transactionServiceImpl.getByPaymentId("test@example.com", 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }



}
