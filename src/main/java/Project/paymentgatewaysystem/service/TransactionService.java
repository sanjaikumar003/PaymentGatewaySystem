package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.dto.TransactionRequestDto;
import Project.paymentgatewaysystem.dto.TransactionResponseDto;

import java.util.List;

public interface TransactionService {
    TransactionResponseDto processTransaction(TransactionRequestDto request);
    TransactionResponseDto getById(Long transactionId);
    List<TransactionResponseDto> getByPaymentId(Long paymentId);
}
