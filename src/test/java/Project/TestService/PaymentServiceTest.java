package Project.TestService;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.dto.PaymentRequestDto;
import Project.paymentgatewaysystem.dto.PaymentResponseDto;
import Project.paymentgatewaysystem.entity.Merchant;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.entity.Order;
import Project.paymentgatewaysystem.entity.Payment;
import Project.paymentgatewaysystem.exception.InvalidStateException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.exception.UnauthorizedException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.repository.OrderRepository;
import Project.paymentgatewaysystem.repository.PaymentRepository;
import Project.paymentgatewaysystem.service.PaymentGateway;
import Project.paymentgatewaysystem.serviceImpl.OrderServiceImpl;
import Project.paymentgatewaysystem.serviceImpl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MerchantUserRepository merchantUserRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway gatewayService;


    @InjectMocks
    private PaymentServiceImpl paymentServiceImpl;
    private MerchantUser merchantuser;
    private Merchant merchant;
    private Order order;
    private Payment payment;
    private PaymentRequestDto paymentRequestDto;

    @BeforeEach
    void setUp(){
        merchant = new Merchant();
        merchant.setMerchantId(1L);

        merchantuser = new MerchantUser();
        merchantuser.setEmail("test@example.com");
        merchantuser.setMerchant(merchant);

        order = new Order();
        order.setOrderId(1L);
        order.setMerchant(merchant);
        order.setAmount(new BigDecimal("500.00"));
        order.setStatus(OrderStatus.PENDING);

        payment = new Payment();
        payment.setPaymentId(100L);
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setCreatedAt(LocalDateTime.now());


    }

    @Test
    void createPayment_success_forNonCOD(){
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(1L);
        request.setPaymentMethod(PaymentMethod.CARD);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponseDto response = paymentServiceImpl.createPayment("test@example.com",request);

        assertNotNull(response);
        assertEquals(100L, response.getPaymentId());
        assertEquals(1L, response.getOrderId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
    @Test
    void createPayment_success_forCOD(){
        Payment codPayment = new Payment();
        codPayment.setPaymentId(100L);
        codPayment.setOrder(order);
        codPayment.setPaymentMethod(PaymentMethod.COD);
        codPayment.setStatus(PaymentStatus.SUCCESS);
        codPayment.setAmount(order.getAmount());
        codPayment.setCreatedAt(LocalDateTime.now());

        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(1L);
        request.setPaymentMethod(PaymentMethod.COD);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment).thenReturn(codPayment);

        PaymentResponseDto response = paymentServiceImpl.createPayment("test@example.com", request);
        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS,response.getStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(paymentRepository,times(2)).save(any(Payment.class));

    }
    @Test
    void createPayment_idempotencyKey(){
        payment.setIdempotencyKey("ikey-123");
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(1L);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setIdempotencyKey("ikey-123");

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_OrderIdAndIdempotencyKey(1L,"ikey-123")).thenReturn(Optional.of(payment));

        PaymentResponseDto response = paymentServiceImpl.createPayment("test@example.com",request);

        assertNotNull(response);
        assertEquals(100L,response.getPaymentId());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
    @Test
    void createPayment_orderIdNull(){
        PaymentRequestDto request = new PaymentRequestDto();
        request.setPaymentMethod(PaymentMethod.COD);

        assertThrows(IllegalArgumentException.class, () -> paymentServiceImpl.createPayment("test@example.com",request));
    }
    @Test
    void createPayment_paymentIdNull(){
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(1L);

        assertThrows(IllegalArgumentException.class, () -> paymentServiceImpl.createPayment("test@example.com",request));
    }
    @Test
    void createPayment_userNotFound(){
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(1L);
        request.setPaymentMethod(PaymentMethod.COD);

        when(merchantUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentServiceImpl.createPayment("unknown@merchant.com", request));
    }
    @Test
    void createPayment_whenOrderNotFound() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(99L);
        request.setPaymentMethod(PaymentMethod.CARD);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentServiceImpl.createPayment("test@example.com", request));
    }
    @Test
    void createPayment_whenOrderBelongsToDifferentMerchant() {
        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(99L);
        order.setMerchant(otherMerchant);

        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(10L);
        request.setPaymentMethod(PaymentMethod.CARD);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedException.class,
                () -> paymentServiceImpl.createPayment("test@example.com", request));
    }
    @Test
    void createPayment_whenOrderAlreadyPaid() {
        order.setStatus(OrderStatus.PAID);

        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(10L);
        request.setPaymentMethod(PaymentMethod.CARD);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(InvalidStateException.class,
                () -> paymentServiceImpl.createPayment("test@example.com", request));
    }

    @Test
    void getById_success() {

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));

        PaymentResponseDto response = paymentServiceImpl.getById("test@example.com", 100L);

        assertNotNull(response);
        assertEquals(100L, response.getPaymentId());
    }

    @Test
    void getById_throwsResourceNotFound_whenPaymentMissing() {
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentServiceImpl.getById("test@example.com", 99L));
    }

    @Test
    void getById_throwsUnauthorized_whenDifferentMerchant() {
        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(99L);

        order.setMerchant(otherMerchant);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(UnauthorizedException.class,
                () -> paymentServiceImpl.getById("test@example.com", 1L));
    }
    @Test
    void getByOrderId_success(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(paymentRepository.findByOrder_OrderId(1L)).thenReturn(Optional.of(payment));

        PaymentResponseDto response = paymentServiceImpl.getByOrderId("test@example.com", 1L);
        assertNotNull(response);
        assertEquals(1L, response.getOrderId());

    }
    @Test
    void getByOrderId_paymentNotFound(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(paymentRepository.findByOrder_OrderId(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> paymentServiceImpl.getByOrderId("test@example.com", 1L));
    }
    @Test
    void getByOrderId_differentMerchant(){
        Merchant otherMerchant = new Merchant();
        otherMerchant.setMerchantId(99L);
        order.setMerchant(otherMerchant);

        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantuser));
        when(paymentRepository.findByOrder_OrderId(1L)).thenReturn(Optional.of(payment));

        assertThrows(UnauthorizedException.class, () -> paymentServiceImpl.getByOrderId("test@example.com", 1L));
    }

}
