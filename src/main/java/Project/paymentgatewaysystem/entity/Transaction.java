package Project.paymentgatewaysystem.entity;

import Project.paymentgatewaysystem.constants.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="transactions",uniqueConstraints = {
        @UniqueConstraint(
                name = "unique_tx_per_payment_key",
                columnNames = {"payment_id", "idempotency_key"}
        )
}
)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private String idempotencyKey;
    @Column(nullable = false)
    private TransactionStatus status;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}
