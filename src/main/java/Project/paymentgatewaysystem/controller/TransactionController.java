package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.TransactionRequestDto;
import Project.paymentgatewaysystem.dto.TransactionResponseDto;
import Project.paymentgatewaysystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponseDto> processTransaction(
            @Valid @RequestBody TransactionRequestDto request, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.processTransaction(userDetails.getUsername(),request));
    }
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> getById(
            @PathVariable Long transactionId,@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionService.getById(userDetails.getUsername(),transactionId));
    }
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<TransactionResponseDto>> getByPaymentId(
            @PathVariable Long paymentId,@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionService.getByPaymentId(userDetails.getUsername(),paymentId));
    }
}