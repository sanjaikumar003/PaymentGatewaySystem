package Project.paymentgatewaysystem.dto;

import Project.paymentgatewaysystem.constants.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
public class MerchantResponseDto {
    private Long merchantId;
    private String name;
    private String email;
    private String apiKey;
    private MerchantStatus status;
    private String secretKey;
    private LocalDateTime createdAt;
}
