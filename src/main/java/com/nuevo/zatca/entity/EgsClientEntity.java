package com.nuevo.zatca.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "egs_client")
public class EgsClientEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "egs_client_name", nullable = false, unique = true)
    private String egsClientName;

    @OneToOne(mappedBy = "egsClientEntity", cascade = CascadeType.ALL)
    private ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity;

    @Column(name = "egs_client_seriel_number", nullable = false, unique = true)
    private String egsClientSerielNumber;


    @Column(name = "vat_registration_number", nullable = false, unique = true)
    private String vatRegistrationNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "location_address")
    private String locationAddress;

    @Column(name = "city")
    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "country")
    private String country;

    @Column(name = "email")
    private String email;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "industry_type")
    private String industryType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "organization_unit")
    private String organizationUnit;


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
