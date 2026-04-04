package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.TransactionRequestDto;
import Project.paymentgatewaysystem.dto.TransactionResponseDto;
import Project.paymentgatewaysystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponseDto> processTransaction(
            @Valid @RequestBody TransactionRequestDto request, @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("Unauthorized transaction attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Processing transaction for user {}", userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.processTransaction(userDetails.getUsername(),request));
    }
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> getById(
            @PathVariable Long transactionId,@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("Unauthorized access to transaction {}", transactionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Fetching transaction {} for user {}", transactionId, userDetails.getUsername());
        return ResponseEntity.ok(transactionService.getById(userDetails.getUsername(),transactionId));
    }
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<TransactionResponseDto>> getByPaymentId(
            @PathVariable Long paymentId,@AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("Unauthorized access to transactions for payment {}", paymentId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Fetching transactions for payment {} by user {}", paymentId, userDetails.getUsername());
        return ResponseEntity.ok(transactionService.getByPaymentId(userDetails.getUsername(),paymentId));
    }
}