package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.dto.PaymentRequestDto;
import Project.paymentgatewaysystem.dto.PaymentResponseDto;
import Project.paymentgatewaysystem.entity.Order;
import Project.paymentgatewaysystem.entity.Payment;
import Project.paymentgatewaysystem.exception.DuplicateResourceException;
import Project.paymentgatewaysystem.exception.InvalidStateException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
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

    @Override
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        Order order = orderRepository.findById((request.getOrderId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
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
    public PaymentResponseDto getById(Long paymentId){
        return toDto(paymentRepository.findById(paymentId)
                .orElseThrow(()-> new ResourceNotFoundException("Payment not found: " + paymentId)));

    }
    @Override
    public PaymentResponseDto getByOrderId(Long orderId){
        return toDto(paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(()-> new ResourceNotFoundException("No payment found")));
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
