package com.salespeople.communications;

import com.salespeople.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message_template", schema = "sales_people")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplate extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mstId;

    private String mstName;

    private String mstDescription;

    @Column(columnDefinition = "TEXT")
    private String mstEmail;

    @Column(columnDefinition = "TEXT")
    private String mstSms;

    private String mstSubject;

    @Column(unique = true)
    private String mstType;

    private String mstStatus;

    @Column(columnDefinition = "TEXT")
    private String mstContent;
}
