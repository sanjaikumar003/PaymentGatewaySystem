package Project.paymentgatewaysystem.repository;

import Project.paymentgatewaysystem.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByMerchant_MerchantId(Long merchantId);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
