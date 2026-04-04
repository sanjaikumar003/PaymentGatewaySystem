package Project.paymentgatewaysystem.service;
import Project.paymentgatewaysystem.dto.LoginRequestDto;
import Project.paymentgatewaysystem.dto.LoginResponseDto;
import Project.paymentgatewaysystem.dto.MerchantRequestDto;
import Project.paymentgatewaysystem.dto.MerchantResponseDto;

public interface MerchantService {
    MerchantResponseDto register(MerchantRequestDto request);
    LoginResponseDto login (LoginRequestDto request);
    MerchantResponseDto getByEmail(String email);

}
