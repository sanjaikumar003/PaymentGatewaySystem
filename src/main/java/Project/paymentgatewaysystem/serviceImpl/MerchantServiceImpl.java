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

import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MerchantUserRepository merchantUserRepository;
    private final JwtUtil jwtUtil;
    @Override
    @Transactional
    public MerchantResponseDto register(MerchantRequestDto request){
        String email= request.getEmail().toLowerCase();
        log.info("Registering merchant: {}",email);

        if(merchantRepository.existsByEmail(email)){
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
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
        String email= request.getEmail().toLowerCase();
        log.info("Login attempt: {}",email);
        MerchantUser user = merchantUserRepository.findByEmail(email).orElseThrow(()->new BadCredentialsException("Invalid credentials"));
        if(!passwordEncoder.matches(request.getPassword(),user.getPasswordHash())){
            throw new BadCredentialsException("Invalid credentials");
        }
        if(user.getMerchant().getStatus()!=MerchantStatus.ACTIVE){
            throw new BadCredentialsException("Merchant inactive");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponseDto(token, user.getEmail(),
                user.getMerchant().getMerchantId());

    }
    @Override
    public MerchantResponseDto getByEmail(String email) {
        MerchantUser user = merchantUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Merchant not found: " + email));
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