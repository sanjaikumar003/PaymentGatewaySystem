package Project.paymentgatewaysystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundRequestDto {
    private Long paymentId;
    private String reason;
    private String idempotencyKey;
}
