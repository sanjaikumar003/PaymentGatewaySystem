package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.OrderStatus;
import Project.paymentgatewaysystem.constants.PaymentMethod;
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
import Project.paymentgatewaysystem.exception.UnauthorizedException;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.repository.OrderRepository;
import Project.paymentgatewaysystem.repository.PaymentRepository;
import Project.paymentgatewaysystem.repository.TransactionRepository;
import Project.paymentgatewaysystem.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MerchantUserRepository merchantUserRepository;

    private MerchantUser getUser(String email) {
        return merchantUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public TransactionResponseDto processTransaction(String email, TransactionRequestDto request) {
        MerchantUser user = getUser(email);
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + request.getPaymentId()));
        if (!payment.getOrder().getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())) {
            throw new UnauthorizedException("Access denied");
        }
        if (request.getIdempotencyKey() != null) {
            Transaction existing = transactionRepository
                    .findByPayment_PaymentIdAndIdempotencyKey(payment.getPaymentId(),request.getIdempotencyKey())
                    .orElse(null);

            if (existing != null) {
                log.warn("Duplicate transaction request: {}", request.getIdempotencyKey());

                 return toDto(existing);
            }
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidStateException("Payment already completed: " + payment.getStatus());
        }

        if (payment.getStatus() != PaymentStatus.PENDING &&
                payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidStateException("Invalid payment state");
        }

        Order order = payment.getOrder();
        log.info("Processing transaction for payment {}", payment.getPaymentId());
        Transaction transaction = new Transaction();
        transaction.setPayment(payment);
        transaction.setAmount(order.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setIdempotencyKey(request.getIdempotencyKey());

        log.info("Transaction created (PENDING) for payment {}", payment.getPaymentId());

        boolean success = simulatePaymentProcessing(payment.getPaymentMethod());
        transaction.setStatus(success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);


        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        if (success) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        }
        paymentRepository.save(payment);
        transaction = transactionRepository.save(transaction);
        log.info("Transaction {} processed with status {}", transaction.getTransactionId(), transaction.getStatus());

        return toDto(transaction);
    }

    @Override
    public TransactionResponseDto getById(String email, Long transactionId) {
        MerchantUser user = getUser(email);
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + transactionId));
        if (!tx.getPayment().getOrder().getMerchant().getMerchantId().equals(user.getMerchant().getMerchantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return toDto(tx);
    }


    @Override
    public List<TransactionResponseDto> getByPaymentId(String email, Long paymentId) {

        MerchantUser user = getUser(email);
        List<Transaction> transactions = transactionRepository
                .findByPayment_PaymentId(paymentId);

        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found for payment: " + paymentId);
        }


        return transactionRepository.findByPayment_PaymentId(paymentId)
                .stream()
                .filter(tx -> tx.getPayment().getOrder().getMerchant()
                        .getMerchantId().equals(user.getMerchant().getMerchantId()))
                .map(this::toDto)
                .toList();
    }

    private boolean simulatePaymentProcessing(PaymentMethod method) {
        return Math.random() > 0.2;
    }

    private TransactionResponseDto toDto(Transaction end) {
        return new TransactionResponseDto(
                end.getTransactionId(),
                end.getPayment().getPaymentId(),
                end.getPayment().getOrder().getOrderId(),
                end.getAmount(),
                end.getIdempotencyKey(),
                end.getStatus(),
                end.getCreatedAt()
        );
    }
}