package com.possystem.security;
import com.possystem.common.UserType;

import com.possystem.auth.user.User;
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
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String phoneNumber;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final UserStatus status;
    private final UserType userType;
    private final Set<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getUsrId();
        this.email = user.getUsrEmail();
        this.phoneNumber = user.getUsrPhoneNumber();
        this.password = user.getUsrPassword();
        this.firstName = user.getUsrFirstName();
        this.lastName = user.getUsrLastName();
        this.status = user.getUsrStatus();
        this.userType = user.getUserType() != null ? user.getUserType() : UserType.TENANT;
        this.authorities = buildAuthorities(user);
    }

    private Set<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        UserType type = user.getUserType() != null ? user.getUserType() : UserType.TENANT;
        authorities.add(new SimpleGrantedAuthority("ROLE_" + type.name()));

        if (user.getProfile() != null && user.getProfile().getProfileCode() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getProfile().getProfileCode()));
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
