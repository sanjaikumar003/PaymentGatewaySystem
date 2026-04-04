package Project.paymentgatewaysystem.service;

import Project.paymentgatewaysystem.dto.TransactionRequestDto;
import Project.paymentgatewaysystem.dto.TransactionResponseDto;

import java.util.List;

public interface TransactionService {
    TransactionResponseDto processTransaction(String email,TransactionRequestDto request);
    TransactionResponseDto getById(String email,Long transactionId);
    List<TransactionResponseDto> getByPaymentId(String email,Long paymentId);
}
