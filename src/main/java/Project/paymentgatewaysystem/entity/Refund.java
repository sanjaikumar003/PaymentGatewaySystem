package Project.paymentgatewaysystem.entity;

import Project.paymentgatewaysystem.constants.RefundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refundId;
    private String refundReference;
    private BigDecimal amount;
    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;
    @Enumerated(EnumType.STRING)
    private RefundStatus status;
    private String reason;
    @Column(unique = true)
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
