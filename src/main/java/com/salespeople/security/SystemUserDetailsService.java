package com.salespeople.security;

import com.salespeople.auth.user.UserTb;
import com.salespeople.auth.user.UserTbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("systemUserDetailsService")
@RequiredArgsConstructor
public class SystemUserDetailsService implements UserDetailsService {

    private final UserTbRepository userTbRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserTb user = userTbRepository.findByUserEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new UserPrincipal(user);
    }
}
