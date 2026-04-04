package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.constants.PaymentStatus;
import Project.paymentgatewaysystem.constants.TransactionStatus;
import Project.paymentgatewaysystem.dto.TransactionRequestDto;
import Project.paymentgatewaysystem.dto.TransactionResponseDto;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.entity.Order;
import Project.paymentgatewaysystem.entity.Payment;
import Project.paymentgatewaysystem.entity.Transaction;
import Project.paymentgatewaysystem.exception.InvalidStateException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
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
    private  final MerchantUserRepository merchantUserRepository;

    @Override
    @Transactional
    public TransactionResponseDto processTransaction(String email,TransactionRequestDto request) {
        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(()->new ResourceNotFoundException("User not found"));
         Payment payment = paymentRepository.findById(request.getPaymentId())
                 .orElseThrow(()-> new ResourceNotFoundException("Payment not found: " + request.getPaymentId()));
         if(!payment.getOrder().getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())){
             throw new InvalidStateException("Access denied");
         }
         if(payment.getStatus() != PaymentStatus.PENDING){
             throw new InvalidStateException("Payment cannot be process: " + payment.getStatus());
         }
        Order order = payment.getOrder();


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
    public TransactionResponseDto getById(String email,Long transactionId) {
        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(()->new ResourceNotFoundException("User not found"));
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + transactionId));
        if(!tx.getPayment().getOrder().getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())){
            throw new RuntimeException("Access denied");
        }
        return toDto(tx);
    }


    @Override
    public List<TransactionResponseDto> getByPaymentId(String email,Long paymentId) {
        MerchantUser user = merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(()->new ResourceNotFoundException("User not found"));
        List<Transaction> transactions=transactionRepository.findByPayment_PaymentId(paymentId);
        return transactions.stream()
                .filter((tx->tx.getPayment().getOrder().getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())))
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