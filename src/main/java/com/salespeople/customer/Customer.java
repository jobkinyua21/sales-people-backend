package com.salespeople.customer;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers", schema = "public")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    @EqualsAndHashCode.Include
    private Long customerId;

    @Column(name = "customer_email", length = 200)
    private String customerEmail;

    @Column(name = "customer_outlet_name", nullable = false, length = 200)
    private String customerOutletName;

    @Column(name = "customer_contact_person", nullable = false, length = 200)
    private String customerContactPerson;

    @Column(name = "customer_contact", nullable = false, length = 200)
    private String customerContact;

    @Column(name = "customer_location", length = 200)
    private String customerLocation;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "buy_on_credit")
    @Builder.Default
    private Boolean buyOnCredit = false;

    // full_name is a generated column — read-only
    @Column(name = "full_name", insertable = false, updatable = false)
    private String fullName;

    @Column(name = "balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
}
