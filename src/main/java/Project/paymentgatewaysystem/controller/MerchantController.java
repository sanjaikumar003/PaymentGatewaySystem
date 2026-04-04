package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.*;
import Project.paymentgatewaysystem.service.MerchantService;
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
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/register")
    public ResponseEntity<MerchantResponseDto> register(
            @Valid @RequestBody MerchantRequestDto request) {
        log.info("Register request for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(merchantService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request) {
        log.info("Login attempt for email: {}", request.getEmail());
        return ResponseEntity.ok(merchantService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MerchantResponseDto> getMyDetails(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(merchantService.getByEmail(userDetails.getUsername()));
    }
}
