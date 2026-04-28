package com.salespeople.salesperson;

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
@Table(name = "sales_people", schema = "public")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SalesPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_person_id")
    @EqualsAndHashCode.Include
    private Long salesPersonId;

    @Column(name = "sales_person_number", unique = true, nullable = false, insertable = false, updatable = false)
    private Integer salesPersonNumber;

    @Column(name = "first_name", nullable = false, length = 200)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 200)
    private String lastName;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    @Column(name = "phone", length = 200)
    private String phone;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "order_limit", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal orderLimit = new BigDecimal("30000.00");

    @Column(name = "make_order")
    @Builder.Default
    private Boolean makeOrder = false;

    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
