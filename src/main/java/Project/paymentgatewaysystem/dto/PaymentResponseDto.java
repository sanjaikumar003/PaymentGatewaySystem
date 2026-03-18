package Project.paymentgatewaysystem.dto;

import Project.paymentgatewaysystem.constants.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDto {
    private Long paymentId;
    private Long orderId;
    private String paymentMethod;
    private PaymentStatus status;
    private LocalDateTime createdAt;
}
