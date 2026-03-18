package Project.paymentgatewaysystem.repository;

import Project.paymentgatewaysystem.entity.MerchantUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantUserRepository extends JpaRepository<MerchantUser, Long> {
    Optional<MerchantUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
