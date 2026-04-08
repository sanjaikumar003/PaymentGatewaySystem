package Project.paymentgatewaysystem.entity;

import Project.paymentgatewaysystem.constants.PaymentMethod;
import Project.paymentgatewaysystem.constants.PaymentStatus;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="payments", uniqueConstraints = {
        @UniqueConstraint(
                name = "unique_payment_per_order",
                columnNames = "order_id"
        )
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    @Column(unique = true)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    @Column(nullable = false)
    private BigDecimal amount;
   @CreationTimestamp
   @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;

}
