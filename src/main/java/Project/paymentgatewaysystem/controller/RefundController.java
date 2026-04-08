package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.RefundRequestDto;
import Project.paymentgatewaysystem.dto.RefundResponseDto;
import Project.paymentgatewaysystem.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/refund")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;
    @PostMapping
    public ResponseEntity<RefundResponseDto> createRefund(
            @Valid @RequestBody RefundRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(refundService.createRefund(userDetails.getUsername(),request));

    }
}
