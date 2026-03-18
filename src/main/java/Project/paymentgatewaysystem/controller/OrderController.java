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
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final MerchantUserRepository merchantUserRepository;
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request, @AuthenticationPrincipal UserDetails userDetails){
        Long merchantId=resolveMerchantId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(merchantId, request));
    }
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getById(orderId));
    }
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long merchantId = resolveMerchantId(userDetails);
        return ResponseEntity.ok(orderService.getByMerchant(merchantId));
    }
    private Long resolveMerchantId(UserDetails userDetails) {
        MerchantUser user = merchantUserRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return user.getMerchant().getMerchantId();
    }
}
