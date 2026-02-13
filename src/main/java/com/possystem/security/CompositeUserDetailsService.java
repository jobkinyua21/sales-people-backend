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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return systemUserDetailsService.loadUserByUsername(username);
    }

    public UserDetails loadUserByUsernameAndType(String username, UserType userType) throws UsernameNotFoundException {
        return systemUserDetailsService.loadUserByUsername(username);
    }
}
