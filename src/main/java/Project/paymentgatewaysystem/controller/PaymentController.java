package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.PaymentResponseDto;
import Project.paymentgatewaysystem.dto.PaymentRequestDto;
import Project.paymentgatewaysystem.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto request, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(userDetails.getUsername(),request));
    }
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getById(@PathVariable Long paymentId, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getById(userDetails.getUsername(),paymentId));
    }
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDto> getByOrderId(@PathVariable Long orderId,@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getByOrderId(userDetails.getUsername(),orderId));
    }
}
