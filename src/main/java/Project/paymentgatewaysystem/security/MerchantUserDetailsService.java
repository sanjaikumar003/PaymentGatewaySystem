package Project.paymentgatewaysystem.security;

import Project.paymentgatewaysystem.entity.MerchantUser;
import Project.paymentgatewaysystem.repository.MerchantUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantUserDetailsService implements UserDetailsService {

    private final MerchantUserRepository merchantUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        MerchantUser merchantUser = merchantUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new User(
                merchantUser.getEmail(),
                merchantUser.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
        );
    }
}
