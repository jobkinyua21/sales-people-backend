package com.salespeople.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("compositeUserDetailsService")
@RequiredArgsConstructor
public class CompositeUserDetailsService implements UserDetailsService {

    private final SystemUserDetailsService systemUserDetailsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return systemUserDetailsService.loadUserByUsername(username);
    }
}
