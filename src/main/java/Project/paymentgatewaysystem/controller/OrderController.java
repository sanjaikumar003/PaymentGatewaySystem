package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.OrderRequestDto;
import Project.paymentgatewaysystem.dto.OrderResponseDto;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.repository.MerchantRepository;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request, @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(userDetails.getUsername(),request));
    }
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable Long orderId, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getById(userDetails.getUsername(),orderId));
    }
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getByMerchant(userDetails.getUsername()));
    }

}
