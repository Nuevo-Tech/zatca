package com.nuevo.zatca.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "products")
public class ProductsEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "vat_rate_percentage", nullable = false)
    private BigDecimal vatRatePercentage;

    @Column(name = "vat_category_code")
    private String vatCategoryCode;

    @Column(name = "vat_reason_code")
    private String vatReasonCode;

    @Column(name = "is_active")
    private Boolean isActive;

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
