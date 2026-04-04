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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MerchantUserRepository merchantUserRepository;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(String email,PaymentRequestDto request) {
        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(()->new ResourceNotFoundException("User not found"));
        Order order = orderRepository.findById((request.getOrderId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
        if(!order.getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())){
            throw new RuntimeException("Access denied");
        }
        if(order.getStatus()!=OrderStatus.CREATED){
            throw new InvalidStateException("Order is not payable:" +  order.getStatus());
        }
        if(paymentRepository.findByOrder_OrderId(order.getOrderId()).isPresent()){
            throw new DuplicateResourceException("Already paid: " + order.getOrderId());

        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        return toDto(paymentRepository.save(payment));
    }
    @Override
    public PaymentResponseDto getById(String email,Long paymentId){
        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(()->new ResourceNotFoundException("User not found"));
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("No payment found"));
        if(!payment.getOrder().getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())){
            throw new RuntimeException("Access denied");
        }
        return toDto(payment);
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
