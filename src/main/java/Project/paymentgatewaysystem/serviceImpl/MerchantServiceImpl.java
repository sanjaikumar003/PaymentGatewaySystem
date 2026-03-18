package Project.paymentgatewaysystem.serviceImpl;

import Project.paymentgatewaysystem.constants.MerchantStatus;
import Project.paymentgatewaysystem.dto.LoginRequestDto;
import Project.paymentgatewaysystem.dto.LoginResponseDto;
import Project.paymentgatewaysystem.dto.MerchantRequestDto;
import Project.paymentgatewaysystem.dto.MerchantResponseDto;
import Project.paymentgatewaysystem.entity.Merchant;
import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.exception.ResourceNotFoundException;
import Project.paymentgatewaysystem.repository.MerchantRepository;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import Project.paymentgatewaysystem.security.JwtUtil;
import Project.paymentgatewaysystem.service.MerchantService;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

        if(merchantRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }
        String rewApiKey = UUID.randomUUID().toString();
        String rawSecretKey = UUID.randomUUID().toString();

        Merchant merchant = new Merchant();
        merchant.setName(request.getName());
        merchant.setEmail(request.getEmail());
        merchant.setApiKey((rewApiKey));
        merchant.setSecretKey(passwordEncoder.encode(rawSecretKey));
        merchant.setStatus(MerchantStatus.ACTIVE);

        Merchant saved = merchantRepository.save(merchant);
        MerchantUser user= new MerchantUser();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setMerchant(saved);
        MerchantUser createUser = merchantUserRepository.save(user);
        return new MerchantResponseDto(
                saved.getMerchantId(),
                saved.getName(),
                saved.getEmail(),
                saved.getApiKey(),
                saved.getStatus(),
                rawSecretKey,
                saved.getCreatedAt()
        );
    }

    @Override
    public LoginResponseDto login(LoginRequestDto request){
        MerchantUser user = merchantUserRepository.findByEmail(request.getEmail()).orElseThrow(()->new RuntimeException("Invalid credentials"));
        if(!passwordEncoder.matches(request.getPassword(),user.getPasswordHash())){
            throw new BadCredentialsException("Inavalid credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponseDto(token, user.getEmail(),
                user.getMerchant().getMerchantId());
    }
    @Override
    public MerchantResponseDto getById(Long merchantId) {
        Merchant Id = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Merchant not found: " + merchantId));
        return new MerchantResponseDto(
                Id.getMerchantId(),
                Id.getName(),
                Id.getEmail(),
                Id.getApiKey(),
                Id.getStatus(),
                null,
                Id.getCreatedAt()
        );
    }


}