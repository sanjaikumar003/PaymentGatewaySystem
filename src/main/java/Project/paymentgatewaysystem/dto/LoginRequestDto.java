package Project.paymentgatewaysystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
    @Email(message = "Invalid email formation")
    @NotBlank(message = "Email is required")
    private  String email;

    @NotBlank(message = "Password is required")
    private String password;
}
