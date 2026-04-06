package Project.paymentgatewaysystem.repository;

import Project.paymentgatewaysystem.entity.Payment;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder_OrderId(Long orderId);
    Optional<Payment> findByIdempotencyKey(String idempotency);
    Optional<Payment> findByOrder_OrderIdAndIdempotencyKey(Long orderId, String idempotencyKey);
}
