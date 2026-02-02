package com.possystem.security;
import com.possystem.common.UserType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service("compositeUserDetailsService")
@RequiredArgsConstructor
public class CompositeUserDetailsService implements UserDetailsService {

    private final SystemUserDetailsService systemUserDetailsService;
    private final PosUserDetailsService posUserDetailsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try system user first
        try {
            return systemUserDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.debug("User not found in system users, trying POS users");
        }

        // Try POS user
        try {
            return posUserDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.debug("User not found in POS users");
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }

    public UserDetails loadUserByUsernameAndType(String username, UserType userType) throws UsernameNotFoundException {
        return switch (userType) {
            case ADMIN, TENANT -> systemUserDetailsService.loadUserByUsername(username);
            case POS -> posUserDetailsService.loadUserByUsername(username);
        };
    }
}
