package Project.paymentgatewaysystem.dto;

import Project.paymentgatewaysystem.constants.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private OrderStatus status;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
