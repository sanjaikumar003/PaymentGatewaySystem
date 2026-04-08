package Project.paymentgatewaysystem.repository;

import Project.paymentgatewaysystem.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByPayment_PaymentIdAndIdempotencyKey(Long paymentId, String IdempotencyKey);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.payment.paymentId = :paymentId AND r.status = 'SUCCESS'")
    BigDecimal sumRefundedAmount(Long paymentId);
}

