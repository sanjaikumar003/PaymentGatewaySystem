package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.dto.PaymentRequestDto;
import Project.paymentgatewaysystem.dto.PaymentResponseDto;
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
import Project.paymentgatewaysystem.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final PaymentGateway gatewayService;
    private MerchantUser getUser(String email) {
        return merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    @Override
    @Transactional
    public PaymentResponseDto createPayment(String email,PaymentRequestDto request) {
        if(request.getOrderId()==null){
            throw new IllegalArgumentException("OrderId is required");
        }
        if(request.getPaymentMethod()==null){
            throw new IllegalArgumentException("Payment method is required");
        }
        MerchantUser user = getUser(email);
        Order order = orderRepository.findById((request.getOrderId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
        if(!order.getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())){
            throw new UnauthorizedException("Access denied");
        }
        if(request.getIdempotencyKey()!=null) {
            Payment existingPayment = paymentRepository.findByOrder_OrderIdAndIdempotencyKey(order.getOrderId(),request.getIdempotencyKey()).orElse(null);
            if (existingPayment != null) {
                log.warn("Duplicate payment request: {}", request.getIdempotencyKey());
                return toDto(existingPayment);
            }
        }
        if(order.getStatus()==OrderStatus.PAID){
            throw new InvalidStateException("Order already paid:" +  order.getStatus());

        }
        Payment payment=new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setAmount(order.getAmount());
        payment = paymentRepository.save(payment);
        log.info("Payment created (PENDING) for order {}", order.getOrderId());
        if (request.getPaymentMethod()== PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.CONFIRMED);
            payment=paymentRepository.save(payment);
            log.info("COD order confirmed {}", order.getOrderId());
        }
        return toDto(payment);
    }
    @Override
    public PaymentResponseDto getById(String email,Long paymentId){
        MerchantUser user = getUser(email);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getOrder().getMerchant().getMerchantId()
                .equals(user.getMerchant().getMerchantId())) {
            throw new UnauthorizedException("Access denied");
        }

        return toDto(payment);
    }
    @Override
    public PaymentResponseDto getByOrderId(String email,Long orderId){
        MerchantUser user = getUser(email);
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("No payment found"));
        if(!payment.getOrder().getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())){
            throw new UnauthorizedException("Access denied");
        }
        return toDto(payment);
    }
    @Override
    @Transactional
    public PaymentResponseDto retryPayment(String email, Long paymentId) {

        MerchantUser user = getUser(email);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!payment.getOrder().getMerchant().getMerchantId()
                .equals(user.getMerchant().getMerchantId())) {
            throw new UnauthorizedException("Access denied");
        }

        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidStateException("Only failed payments can be retried");
        }

        log.info("Retrying payment {}", paymentId);

        payment.setStatus(PaymentStatus.PENDING);
        PaymentStatus result = gatewayService.process(payment.getPaymentMethod());
        payment.setStatus(result);
        if(result == PaymentStatus.SUCCESS) {
            Order order = payment.getOrder();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        }
        return toDto(paymentRepository.save(payment));
    }

    public PaymentResponseDto toDto(Payment Num){
        return new PaymentResponseDto(
                Num.getPaymentId(),
                Num.getOrder().getOrderId(),
                Num.getPaymentMethod(),
                Num.getStatus(),
                Num.getCreatedAt()

        );
    }
}
