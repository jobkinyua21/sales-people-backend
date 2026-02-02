package com.possystem.auth.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.possystem.audit.Auditable;
import com.possystem.common.UserStatus;
import com.possystem.common.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user", schema = "pos_core")
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    @EqualsAndHashCode.Include
    private UUID usrId;

    @Column(name = "email", unique = true)
    private String usrEmail;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String usrPhoneNumber;

    @Column(name = "first_name", nullable = false)
    private String usrFirstName;

    @Column(name = "last_name", nullable = false)
    private String usrLastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private UserStatus usrStatus = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    @Builder.Default
    private UserType userType = UserType.TENANT;

    @JsonIgnore
    @Column(name = "password")
    private String usrPassword;

    @JsonIgnore
    @Column(name = "pin")
    private String usrPin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profile_id", referencedColumnName = "profile_id")
    private Profile profile;

    @Column(name = "last_login")
    private LocalDateTime usrLastLogin;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Transient
    public String getFullName() {
        return (usrFirstName != null ? usrFirstName + " " : "") +
               (usrLastName != null ? usrLastName : "");
    }
}
