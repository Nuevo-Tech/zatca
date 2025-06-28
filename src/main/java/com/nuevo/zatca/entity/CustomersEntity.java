package com.nuevo.zatca.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "customers")
public class CustomersEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "vat_registration_number", nullable = false, unique = true)
    private String vatRegistrationNumber;

    @Column(nullable = false)
    private String type;

    @Column(name = "address_street")
    private String addressStreet;

    @Column(name = "address_building_no")
    private String addressBuildingNo;

    @Column(name = "address_district")
    private String addressDistrict;

    @Column(name = "address_city")
    private String addressCity;

    @Column(name = "address_zip_code")
    private String addressZipCode;

    @Column(name = "address_additional_no")
    private String addressAdditionalNo;

    @Column(name = "address_unit_no")
    private String addressUnitNo;

    @Column(name = "address_province")
    private String addressProvince;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;


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
