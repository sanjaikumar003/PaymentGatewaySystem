package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.TransactionRequestDto;
import Project.paymentgatewaysystem.dto.TransactionResponseDto;
import Project.paymentgatewaysystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponseDto> processTransaction(
            @Valid @RequestBody TransactionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.processTransaction(request));
    }
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> getById(
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getById(transactionId));
    }
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<TransactionResponseDto>> getByPaymentId(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(transactionService.getByPaymentId(paymentId));
    }
}