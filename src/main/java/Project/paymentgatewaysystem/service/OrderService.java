package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.dto.OrderRequestDto;
import Project.paymentgatewaysystem.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {
    OrderResponseDto createOrder(String email, OrderRequestDto request);
    OrderResponseDto getById(String email,Long orderId);
    List<OrderResponseDto> getByMerchant(String email);
}
