package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.PaymentResponseDto;
import Project.paymentgatewaysystem.dto.PaymentRequestDto;
import Project.paymentgatewaysystem.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto request, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("Unauthorized payment creation attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Creating payment for user {}", userDetails.getUsername());


        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(userDetails.getUsername(),request));
    }
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getById(@PathVariable Long paymentId, @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("Unauthorized access to payment {}", paymentId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Fetching payment {} for user {}", paymentId, userDetails.getUsername());
        return ResponseEntity.ok(paymentService.getById(userDetails.getUsername(),paymentId));
    }
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDto> getByOrderId(@PathVariable Long orderId,@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("Unauthorized access to payment by order {}", orderId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Fetching payment for order {} by user {}", orderId, userDetails.getUsername());
        return ResponseEntity.ok(paymentService.getByOrderId(userDetails.getUsername(),orderId));
    }
}
