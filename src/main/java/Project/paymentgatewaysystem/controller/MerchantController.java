package Project.paymentgatewaysystem.controller;

import Project.paymentgatewaysystem.dto.*;
import Project.paymentgatewaysystem.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /** Register a new merchant — public endpoint */
    @PostMapping("/register")
    public ResponseEntity<MerchantResponseDto> register(
            @Valid @RequestBody MerchantRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(merchantService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(merchantService.login(request));
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<MerchantResponseDto> getById(@PathVariable Long merchantId) {
        return ResponseEntity.ok(merchantService.getById(merchantId));
    }
}
