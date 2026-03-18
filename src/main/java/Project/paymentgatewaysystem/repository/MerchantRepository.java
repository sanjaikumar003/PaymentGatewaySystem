package Project.paymentgatewaysystem.repository;

import Project.paymentgatewaysystem.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByEmail(String email);
    Optional<Merchant> findByApiKey(String apiKey);
    boolean existsByEmail(String email);
}
