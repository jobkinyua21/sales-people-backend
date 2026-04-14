package com.salespeople.security;

import com.salespeople.common.UserType;
import com.salespeople.auth.user.User;
import com.salespeople.common.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID roleId;
    private final String email;
    private final String phoneNumber;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final UserStatus status;
    private final UserType userType;
    private final LocalDateTime lockedUntil;
    private final LocalDateTime passwordExpiresAt;
    private final Set<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this(user, List.of());
    }

    public UserPrincipal(User user, List<String> permissionCodes) {
        this.id = user.getUsrId();
        this.roleId = user.getRoleId();
        this.email = user.getUsrEmail();
        this.phoneNumber = user.getUsrPhoneNumber();
        this.password = user.getUsrPassword();
        this.firstName = user.getUsrFirstName();
        this.lastName = user.getUsrLastName();
        this.status = user.getUsrStatus();
        this.userType = user.getUserType() != null ? user.getUserType() : UserType.SALES_PERSON;
        this.lockedUntil = user.getLockedUntil();
        this.passwordExpiresAt = user.getPasswordExpiresAt();
        this.authorities = buildAuthorities(user, permissionCodes);
    }

    private Set<GrantedAuthority> buildAuthorities(User user, List<String> permissionCodes) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        UserType type = user.getUserType() != null ? user.getUserType() : UserType.SALES_PERSON;
        authorities.add(new SimpleGrantedAuthority("ROLE_" + type.name()));

        for (String code : permissionCodes) {
            authorities.add(new SimpleGrantedAuthority(code));
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
        if (status == UserStatus.SUSPENDED) return false;
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return passwordExpiresAt == null || passwordExpiresAt.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    public String getFullName() {
        return (firstName != null ? firstName + " " : "") + (lastName != null ? lastName : "");
    }
}
