package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.MerchantStatus;
import Project.paymentgatewaysystem.dto.LoginRequestDto;
import Project.paymentgatewaysystem.dto.LoginResponseDto;
import Project.paymentgatewaysystem.dto.MerchantRequestDto;
import Project.paymentgatewaysystem.dto.MerchantResponseDto;
import Project.paymentgatewaysystem.entity.Merchant;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.exception.DuplicateResourceException;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.repository.MerchantRepository;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.security.JwtUtil;
import Project.paymentgatewaysystem.service.MerchantService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MerchantUserRepository merchantUserRepository;
    private final JwtUtil jwtUtil;
    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
    @Override
    @Transactional
    public MerchantResponseDto register(MerchantRequestDto request){

        String email = normalize(
                Objects.requireNonNull(request.getEmail(), "Email required")
        );

        Objects.requireNonNull(request.getPassword(), "Password required");
        log.info("Registering merchant: {}",email);

        if(merchantUserRepository.existsByEmail(email)){
            throw new DuplicateResourceException("Email already registered: " + email);
        }
        String rawApiKey = UUID.randomUUID().toString();
        String rawSecretKey = UUID.randomUUID().toString();

        Merchant merchant = new Merchant();
        merchant.setName(request.getName());
        merchant.setEmail(email);
        merchant.setApiKey(passwordEncoder.encode(rawApiKey));
        merchant.setSecretKey(passwordEncoder.encode(rawSecretKey));
        merchant.setStatus(MerchantStatus.ACTIVE);

        Merchant saved = merchantRepository.save(merchant);
        MerchantUser user= new MerchantUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setMerchant(saved);
        merchantUserRepository.save(user);
        log.info("Merchant created with ID: {}", saved.getMerchantId());
        return new MerchantResponseDto(
                saved.getMerchantId(),
                saved.getName(),
                saved.getEmail(),
                rawApiKey,
                saved.getStatus(),
                rawSecretKey,
                saved.getCreatedAt()
        );
    }

    @Override
    public LoginResponseDto login(LoginRequestDto request){
        String email = normalize(
                Objects.requireNonNull(request.getEmail(), "Email required")
        );
        log.info("Login attempt: {}",email);
        Objects.requireNonNull(request.getPassword(), "Password required");
        log.info("Login attempt: {}, email");
        MerchantUser user = merchantUserRepository.findByEmail(email).orElseThrow(()->{log.warn("Login failed for {}",email);
            return new BadCredentialsException("Invalid credentials");
        });
        if(!passwordEncoder.matches(request.getPassword(),user.getPasswordHash())){
            log.warn("Invalid password for {}", email);
            throw new BadCredentialsException("Invalid credentials");
        }
        if(user.getMerchant().getStatus()!=MerchantStatus.ACTIVE){
            throw new BadCredentialsException("Merchant inactive");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponseDto(token,
                user.getEmail(),
                user.getMerchant().getMerchantId());

    }
    @Override
    public MerchantResponseDto getByEmail(String email) {
        String normalizedEmail = normalize(email);
        MerchantUser user = merchantUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Merchant not found: " + normalizedEmail));
        Merchant merchant =user.getMerchant();
        return new MerchantResponseDto(
                merchant.getMerchantId(),
                merchant.getName(),
                merchant.getEmail(),
                merchant.getApiKey(),
                merchant.getStatus(),
                null,
                merchant.getCreatedAt()
        );
    }


}