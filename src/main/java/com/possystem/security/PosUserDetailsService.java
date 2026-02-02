package com.possystem.security;

import com.possystem.auth.pos.PosUser;
import com.possystem.auth.pos.PosUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("posUserDetailsService")
@RequiredArgsConstructor
public class PosUserDetailsService implements UserDetailsService {

    private final PosUserRepository posUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        PosUser user = posUserRepository.findByEmailOrPhoneNumber(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("POS User not found: " + username));
        
        return new PosUserPrincipal(user);
    }
}
