package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.dto.OrderRequestDto;
import Project.paymentgatewaysystem.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {
    OrderResponseDto createOrder(Long merchantId, OrderRequestDto request);
    OrderResponseDto getById(Long orderId);
    List<OrderResponseDto> getByMerchant(Long merchantId);
}
