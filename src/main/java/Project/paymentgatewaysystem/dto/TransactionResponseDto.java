package Project.paymentgatewaysystem.dto;

import Project.paymentgatewaysystem.constants.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionResponseDto {
    private Long transactionId;
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime createdAt;

}
