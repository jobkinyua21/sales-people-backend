package com.salespeople.security;

import com.salespeople.auth.user.UserTb;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final Integer staffNumber;
    private final String userType;
    private final Set<GrantedAuthority> authorities;
    private final boolean deleted;

    public UserPrincipal(UserTb user) {
        this.id = user.getUserId();
        this.email = user.getUserEmail();
        this.password = user.getPassword();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.staffNumber = user.getStaffNumber();
        this.userType = user.getUserType() != null ? user.getUserType() : "SALES_PERSON";
        this.deleted = Boolean.TRUE.equals(user.getDeleted()) || Boolean.TRUE.equals(user.getSoftDelete());
        this.authorities = new HashSet<>();
        this.authorities.add(new SimpleGrantedAuthority("ROLE_" + this.userType));
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
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !deleted;
    }

    public String getFullName() {
        return (firstName != null ? firstName + " " : "") + (lastName != null ? lastName : "");
    }
}
