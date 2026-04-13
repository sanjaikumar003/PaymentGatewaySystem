package Project.MerchantService;

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
import Project.paymentgatewaysystem.serviceImpl.MerchantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MerchantServiceTest {
    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantUserRepository merchantUserRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private MerchantServiceImpl merchantServiceImpl;
    private MerchantRequestDto validRequest;
    private LoginRequestDto loginRequest;
    private MerchantUser merchantUser;
    private Merchant merchant;
    @BeforeEach
    void setUp() {

        merchant = new Merchant();
        merchant.setMerchantId(1L);
        merchant.setName("Test Merchant");
        merchant.setEmail("test@example.com");
        merchant.setStatus(MerchantStatus.ACTIVE);
        merchant.setCreatedAt(LocalDateTime.now());

        merchantUser = new MerchantUser();
        merchantUser.setEmail("test@example.com");
        merchantUser.setPasswordHash("hashed_password");
        merchantUser.setMerchant(merchant);

        validRequest = new MerchantRequestDto();
        validRequest.setName("Test Merchant");
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("securePass123");

        loginRequest = new LoginRequestDto();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("securePass123");
    }
@Test
    void register_success(){


    when(merchantUserRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashed_value");
    when(merchantRepository.save(any(Merchant.class))).thenReturn(merchant);
    when(merchantUserRepository.save(any(MerchantUser.class))).thenReturn(new MerchantUser());

    MerchantResponseDto response = merchantServiceImpl.register(validRequest);
    System.out.println("Actual name: " + response.getName());
    assertNotNull(response);
    assertEquals(1L, response.getMerchantId());
    assertEquals("Test Merchant",response.getName());
    assertEquals("test@example.com", response.getEmail());
    assertEquals(MerchantStatus.ACTIVE, response.getStatus());
    assertNotNull(response.getApiKey());
    assertNotNull(response.getSecretKey());
    assertDoesNotThrow(() -> UUID.fromString(response.getApiKey()));
    assertDoesNotThrow(() -> UUID.fromString(response.getSecretKey()));

    verify(merchantRepository).save(any(Merchant.class));
    verify(merchantUserRepository).save(any(MerchantUser.class));
    }
    @Test
    void register_duplicateEmail(){
        when(merchantUserRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->{merchantServiceImpl.register(validRequest);
        });

        verify(merchantRepository, never()).save(any());
    }
    @Test
    void register_multipleUser(){
        when(merchantUserRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_value");
        when(merchantRepository.save(any(Merchant.class))).thenReturn(merchant);
        when(merchantUserRepository.save(any(MerchantUser.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DuplicateResourceException.class, () ->{
            merchantServiceImpl.register(validRequest);
        });
    }
    @Test
    void register_nullEmial(){
        validRequest.setEmail(null);
        assertThrows(NullPointerException.class, () ->{
            merchantServiceImpl.register(validRequest);
        });
        verifyNoInteractions(merchantRepository);
    }
    @Test
    void register_nullPassword(){
        validRequest.setPassword(null);
        assertThrows(NullPointerException.class, ()-> {
            merchantServiceImpl.register(validRequest);
        });
        verifyNoInteractions(merchantRepository);
    }
    @Test
    void login_success(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(passwordEncoder.matches("securePass123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com")).thenReturn("mock_jwt_token");

        LoginResponseDto response = merchantServiceImpl.login(loginRequest);
        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
        verify(jwtUtil).generateToken("test@example.com");

    }
    @Test
    void login_userNotfound(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> {
            merchantServiceImpl.login(loginRequest);
        });
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }
    @Test
    void login_wrongPassword(){
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(passwordEncoder.matches("securePass123", "hashed_password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> {
            merchantServiceImpl.login(loginRequest);
        });
    }
    @Test
    void login_merchantInactive(){
        merchant.setStatus(MerchantStatus.INACTIVE);
        when(merchantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(merchantUser));
        when(passwordEncoder.matches("securePass123", "hashed_password")).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> {
            merchantServiceImpl.login(loginRequest);
        });
        verify(jwtUtil, never()).generateToken(anyString());
    }
    @Test
    void login_merchantNull_throwsException() {
        merchantUser.setMerchant(null);
        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(merchantUser));
        when(passwordEncoder.matches("securePass123", "hashed_password"))
                .thenReturn(true);


        assertThrows(BadCredentialsException.class, () -> {
            merchantServiceImpl.login(loginRequest);
        });
    }

    @Test
    void login_nullEmail_throwsException() {

        loginRequest.setEmail(null);


        assertThrows(NullPointerException.class, () -> {
            merchantServiceImpl.login(loginRequest);
        });
        verifyNoInteractions(merchantUserRepository);
    }
    @Test
    void getByEmail_success() {

        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(merchantUser));


        MerchantResponseDto response = merchantServiceImpl.getByEmail("test@example.com");


        assertNotNull(response);
        assertEquals(1L, response.getMerchantId());
        assertEquals("Test Merchant", response.getName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(MerchantStatus.ACTIVE, response.getStatus());
    }
    @Test
    void getByEmail_notFound() {

        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());


        assertThrows(ResourceNotFoundException.class, () -> {
            merchantServiceImpl.getByEmail("test@example.com");
        });
    }
    @Test
    void getByEmail_emailIsNormalized() {
        when(merchantUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(merchantUser));


        merchantServiceImpl.getByEmail("  TEST@EXAMPLE.COM  ");

        verify(merchantUserRepository).findByEmail("test@example.com");
    }
}




