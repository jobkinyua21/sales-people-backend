package com.possystem.security;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("systemUserDetailsService")
@RequiredArgsConstructor
public class SystemUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsrEmailOrUsrPhoneNumber(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return new UserPrincipal(user);
    }
}
