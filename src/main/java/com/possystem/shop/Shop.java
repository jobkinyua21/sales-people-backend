package com.possystem.shop;

import com.possystem.audit.Auditable;
import com.possystem.shop.enums.ShopStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "shop", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Shop extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "shop_code", nullable = false, unique = true, length = 50)
    private String shopCode;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(name = "address")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ShopStatus status = ShopStatus.ACTIVE;
}
