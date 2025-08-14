package com.nuevo.zatca.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "zatca_onboarding_credentials", uniqueConstraints = @UniqueConstraint(columnNames = {"egs_client_name", "egs_client_id"}))
public class ZatcaOnboardingCredentialsEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "egs_client_name", nullable = false)
    private String egsClientName;

    @OneToOne
    @JoinColumn(name = "egs_client_entity", referencedColumnName = "id", nullable = false)
    private EgsClientEntity egsClientEntity;

    @Column(name = "compliance_request_id")
    private String complianceRequestId;

    @Column(name = "prod_compliance_request_id")
    private String prodComplianceRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "csr", length = 2048)
    private String csr;

    @Column(name = "csr_private_key", length = 2048)
    private String csrPrivateKey;

    @Column(name = "secret")
    private String secret;

    @Column(name = "binary_security_token", length = 2048)
    private String binarySecurityToken;

    @Column(name = "prod_disposition_message")
    private String prodDispositionMessage;

    @Column(name = "prod_secret")
    private String prodSecret;

    @Column(name = "prod_binary_security_token", length = 2048)
    private String prodBinarySecurityToken;


    @PrePersist
    protected void onCreate() {
        setCreatedAt(OffsetDateTime.now());
        setUpdatedAt(OffsetDateTime.now());
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(OffsetDateTime.now());
    }

}
