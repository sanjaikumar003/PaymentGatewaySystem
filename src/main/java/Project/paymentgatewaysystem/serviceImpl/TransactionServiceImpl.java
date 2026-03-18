package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.constants.TransactionStatus;
import Project.paymentgatewaysystem.dto.TransactionRequestDto;
import Project.paymentgatewaysystem.dto.TransactionResponseDto;
import Project.paymentgatewaysystem.entity.Order;
import Project.paymentgatewaysystem.entity.Payment;
import Project.paymentgatewaysystem.entity.Transaction;
import Project.paymentgatewaysystem.exception.InvalidStateException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.repository.OrderRepository;
import Project.paymentgatewaysystem.repository.PaymentRepository;
import Project.paymentgatewaysystem.repository.TransactionRepository;
import Project.paymentgatewaysystem.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public TransactionResponseDto processTransaction(TransactionRequestDto request) {
         Payment payment = paymentRepository.findById(request.getPaymentId())
                 .orElseThrow(()-> new ResourceNotFoundException("Payment not found: " + request.getPaymentId()));
         if(payment.getStatus() != PaymentStatus.PENDING){
             throw new InvalidStateException("Payment cannot be process: " + payment.getStatus());
         }
        Order order = payment.getOrder();
        if (order == null) {
            throw new InvalidStateException("Order not found for this payment");
        }

        boolean success = simulatePaymentProcessing(payment.getPaymentMethod());
         TransactionStatus txStatus= success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED;

         Transaction transaction = new Transaction();
         transaction.setPayment(payment);
         transaction.setAmount(order.getAmount());
         transaction.setStatus(txStatus);
         Transaction saved = transactionRepository.save(transaction);

         payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
         paymentRepository.save(payment);


         order.setStatus(success ? OrderStatus.PAID : OrderStatus.FAILED);
         orderRepository.save(order);
         return toDto(saved);
    }
    @Override
    public TransactionResponseDto getById(Long transactionId) {
        return toDto(transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + transactionId)));
    }


    @Override
    public List<TransactionResponseDto> getByPaymentId(Long paymentId) {
        return transactionRepository.findByPayment_PaymentId(paymentId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    private boolean simulatePaymentProcessing(String paymentMethod){
        return ! "DECLINED".equalsIgnoreCase(paymentMethod);
    }
    private TransactionResponseDto toDto(Transaction end){
        return new TransactionResponseDto(
                end.getTransactionId(),
                end.getPayment().getPaymentId(),
                end.getPayment().getOrder().getOrderId(),
                end.getAmount(),
                end.getStatus(),
                end.getCreatedAt()
        );
    }
}