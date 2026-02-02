package com.possystem.security;
import com.possystem.common.UserType;

import com.possystem.auth.pos.PosUser;
import com.possystem.common.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class PosUserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID tenantId;
    private final UUID shopId;
    private final String email;
    private final String phoneNumber;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final UserStatus status;
    private final UserType userType;
    private final Set<GrantedAuthority> authorities;

    public PosUserPrincipal(PosUser user) {
        this.id = user.getUserId();
        this.tenantId = user.getTenantId();
        this.shopId = user.getShopId();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.password = user.getPassword();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.status = user.getStatus();
        this.userType = UserType.POS;
        this.authorities = buildAuthorities(user);
    }

    private Set<GrantedAuthority> buildAuthorities(PosUser user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_POS"));
        
        if (user.getPosProfile() != null && user.getPosProfile().getProfileCode() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getPosProfile().getProfileCode()));
        }
        
        return authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email != null && !email.isEmpty() ? email : phoneNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    public String getFullName() {
        return (firstName != null ? firstName + " " : "") + (lastName != null ? lastName : "");
    }
}
