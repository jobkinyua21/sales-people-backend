package com.possystem.auth.pos;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pos_profile", schema = "pos_core")
public class PosProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "profile_name", nullable = false, length = 100)
    private String profileName;

    @Column(name = "profile_code", unique = true, nullable = false, length = 100)
    private String profileCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
