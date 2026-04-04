package Project.paymentgatewaysystem.entity;

import Project.paymentgatewaysystem.constants.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="orders",uniqueConstraints = {
        @UniqueConstraint(
                name = "unique_idempotency_per_merchant",
                columnNames = {"idempotency_key", "merchant_id"}
        )
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    @ManyToOne
    @JoinColumn(name = "merchant_id")
     private Merchant merchant;
     private BigDecimal amount;
     private String currency;
     @Enumerated(EnumType.STRING)
     @Column(nullable = false)
     private OrderStatus status;
    @Column(unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}
