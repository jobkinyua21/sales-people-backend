package com.possystem.communications;

import com.possystem.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message_template", schema = "pos_core")
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
