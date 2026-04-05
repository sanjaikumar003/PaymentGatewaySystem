package Project.paymentgatewaysystem.repository;

import Project.paymentgatewaysystem.entity.Transaction;
import org.aspectj.apache.bcel.classfile.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByPayment_PaymentId(Long paymentId);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
