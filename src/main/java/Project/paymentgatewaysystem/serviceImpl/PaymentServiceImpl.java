package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.dto.PaymentRequestDto;
import Project.paymentgatewaysystem.dto.PaymentResponseDto;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.entity.Order;
import Project.paymentgatewaysystem.entity.Payment;
import Project.paymentgatewaysystem.exception.DuplicateResourceException;
import Project.paymentgatewaysystem.exception.InvalidStateException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.repository.OrderRepository;
import Project.paymentgatewaysystem.repository.PaymentRepository;
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
    private MerchantUser getUser(String email) {
        return merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    @Override
    @Transactional
    public PaymentResponseDto createPayment(String email,PaymentRequestDto request) {
        MerchantUser user = getUser(email);
        Order order = orderRepository.findById((request.getOrderId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
        if(!order.getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())){
            throw new RuntimeException("Access denied");
        }
        if(order.getStatus()!=OrderStatus.CREATED){
            throw new InvalidStateException("Order is not payable:" +  order.getStatus());
        }
        return paymentRepository.findByOrder_OrderId(order.getOrderId())
                .map(this::toDto)
                .orElseGet(() -> {
                    Payment payment = new Payment();
                    payment.setOrder(order);
                    payment.setPaymentMethod(request.getPaymentMethod());
                    payment.setStatus(PaymentStatus.PENDING);

                    log.info("Creating payment for order {}", order.getOrderId());
                    try {
                        return toDto(paymentRepository.save(payment));
                    } catch (Exception ex) {
                        Payment existing = paymentRepository
                                .findByOrder_OrderId(order.getOrderId())
                                .orElseThrow();
                        return toDto(existing);
                    }
                });
    }
    @Override
    public PaymentResponseDto getById(String email,Long paymentId){
        MerchantUser user = getUser(email);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getOrder().getMerchant().getMerchantId()
                .equals(user.getMerchant().getMerchantId())) {
            throw new RuntimeException("Access denied");
        }

        return toDto(payment);
    }
    @Override
    public PaymentResponseDto getByOrderId(String email,Long orderId){
        MerchantUser user = getUser(email);
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("No payment found"));
        if(!payment.getOrder().getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())){
            throw new RuntimeException("Access denied");
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
            throw new InvalidStateException("Access denied");
        }

        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidStateException("Only failed payments can be retried");
        }

        payment.setStatus(PaymentStatus.PENDING);

        log.info("Retrying payment {}", paymentId);

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
