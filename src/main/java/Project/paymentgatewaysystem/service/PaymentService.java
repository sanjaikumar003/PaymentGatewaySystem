package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.dto.PaymentRequestDto;
import Project.paymentgatewaysystem.dto.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto createPayment(PaymentRequestDto request);
    PaymentResponseDto getById(Long paymentId);
    PaymentResponseDto getByOrderId(Long orderId);
}
