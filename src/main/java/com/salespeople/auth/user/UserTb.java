package com.salespeople.auth.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usertb", schema = "public")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserTb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @EqualsAndHashCode.Include
    private Long userId;

    @Column(name = "user_email", unique = true, nullable = false, length = 200)
    private String userEmail;

    @Column(name = "staff_number", unique = true)
    private Integer staffNumber;

    @Column(name = "first_name", nullable = false, length = 200)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 200)
    private String lastName;

    @Column(name = "phone", nullable = false)
    private Integer phone;

    @Column(name = "password", nullable = false, length = 200)
    private String password;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "updated_by", length = 200)
    private String updatedBy;

    @Column(name = "soft_delete")
    @Builder.Default
    private Boolean softDelete = false;

    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "user_type", length = 50, columnDefinition = "varchar(50) default 'SALES_PERSON'")
    @Builder.Default
    private String userType = "SALES_PERSON";

    // failed_login_attempts and locked_until added for account lockout support
    @Column(name = "failed_login_attempts", columnDefinition = "integer default 0")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private java.time.LocalDateTime lockedUntil;

    public String getFullName() {
        return (firstName != null ? firstName + " " : "") + (lastName != null ? lastName : "");
    }
}
