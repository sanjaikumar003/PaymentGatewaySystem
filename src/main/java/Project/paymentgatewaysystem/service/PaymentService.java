package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.dto.PaymentRequestDto;
import Project.paymentgatewaysystem.dto.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto createPayment(String email,PaymentRequestDto request);
    PaymentResponseDto getById(String email,Long paymentId);
    PaymentResponseDto getByOrderId(String email,Long orderId);
    PaymentResponseDto retryPayment(String email, Long paymentId);
}
