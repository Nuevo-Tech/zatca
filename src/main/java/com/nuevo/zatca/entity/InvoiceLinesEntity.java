package com.nuevo.zatca.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "invoice_lines")
public class InvoiceLinesEntity {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoicesEntity invoice;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductsEntity product;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_extension_amount")
    private BigDecimal lineExtensionAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @Column(name = "vat_rate_percentage")
    private BigDecimal vatRatePercentage;

    @Column(name = "vat_category_code")
    private String vatCategoryCode;

    @Column(name = "vat_reason_code")
    private String vatReasonCode;

    @Column(name = "vat_amount")
    private BigDecimal vatAmount;

    @Column(name = "gross_amount")
    private BigDecimal grossAmount;

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
