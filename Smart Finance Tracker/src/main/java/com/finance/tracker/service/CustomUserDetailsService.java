package com.finance.tracker.service;

import com.finance.tracker.entity.User;
import com.finance.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // CRITICAL FIX: Pass account status flags to Spring Security.
        // This means authenticationManager.authenticate() will throw:
        //   - DisabledException       → for SUSPENDED or BLOCKED users
        //   - LockedException         → for accountLocked = true
        // These are caught in AuthService.login() BEFORE a token is ever generated.
        boolean isActive = user.getStatus() == User.UserStatus.ACTIVE;
        boolean isNotLocked = !Boolean.TRUE.equals(user.getAccountLocked());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                isActive,      // enabled  — false for SUSPENDED / BLOCKED
                true,          // accountNonExpired
                true,          // credentialsNonExpired
                isNotLocked,   // accountNonLocked — false when account is locked
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()))
        );
    }
}
