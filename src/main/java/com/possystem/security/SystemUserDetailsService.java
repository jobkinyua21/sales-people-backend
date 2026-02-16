package com.possystem.security;

import com.possystem.auth.user.User;
import com.possystem.auth.user.UserRepository;
import com.possystem.role.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("systemUserDetailsService")
@RequiredArgsConstructor
public class SystemUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsrEmailOrUsrPhoneNumber(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<String> permissionCodes = List.of();
        if (user.getRoleId() != null) {
            permissionCodes = rolePermissionRepository.findPermissionCodesByRoleId(user.getRoleId());
        }

        return new UserPrincipal(user, permissionCodes);
    }
}
