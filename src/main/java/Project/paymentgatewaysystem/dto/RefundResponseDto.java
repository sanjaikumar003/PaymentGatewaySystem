package Project.paymentgatewaysystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundResponseDto {
    private Long refundId;
    private Long paymentId;
    private BigDecimal amount;
    private String status;
    private String reason;
    private  String idempotencyKey;
    private LocalDateTime createdAt;
}
