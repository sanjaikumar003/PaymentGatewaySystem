package Project.paymentgatewaysystem.service;


import Project.paymentgatewaysystem.dto.RefundRequestDto;
import Project.paymentgatewaysystem.dto.RefundResponseDto;

public interface RefundService {
   RefundResponseDto createRefund(String email, RefundRequestDto request);
}
