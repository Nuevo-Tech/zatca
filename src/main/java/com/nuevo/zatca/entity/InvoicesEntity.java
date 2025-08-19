package com.nuevo.zatca.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "invoices")
public class InvoicesEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;


    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;

    @Column(name = "invoice_id", unique = true, nullable = false)
    private String invoiceId;

    @Column(name = "invoice_type")
    private String invoiceType;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "invoice_time")
    private LocalTime invoiceTime;

    @Column(name = "issue_datetime_gregorian")
    private OffsetDateTime issueDatetimeGregorian;

    @Column(name = "issue_datetime_hijri")
    private String issueDatetimeHijri;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "payment_means")
    private String paymentMeans;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(name = "exchange_rate")
    private BigDecimal exchangeRate;

    @Column(name = "net_total")
    private BigDecimal netTotal;

    @Column(name = "vat_total")
    private BigDecimal vatTotal;

    @Column(name = "gross_total")
    private BigDecimal grossTotal;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "withholding_tax_amount")
    private BigDecimal withholdingTaxAmount;

    @Column(name = "pre_tax_total")
    private BigDecimal preTaxTotal;

    @Column(name = "round_off_amount")
    private BigDecimal roundOffAmount;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "delivery_location")
    private String deliveryLocation;

    @Column(name = "delivery_terms")
    private String deliveryTerms;

    @Column(name = "document_currency_code")
    private String documentCurrencyCode;

    @Column(name = "tax_currency_code")
    private String taxCurrencyCode;

    @Column(name = "zatca_uuid", columnDefinition = "uuid")
    private UUID zatcaUuid;

    @Column(name = "zatca_hash")
    private String zatcaHash;

    @Column(name = "zatca_qr_code")
    private String zatcaQrCode;

    @Column(name = "zatca_previous_invoice_hash")
    private String zatcaPreviousInvoiceHash;

    @Column(name = "zatca_error_code")
    private String zatcaErrorCode;


    @Column(name = "zatca_compliance_request_payload", columnDefinition = "TEXT")
    private String zatcaComplianceRequestPayload;

    @Column(name = "zatca_compliance_response", columnDefinition = "TEXT")
    private String zatcaComplianceResponse;

    @Column(name = "invoice_xml_path")
    private String invoiceXmlPath;

    @Column(name = "zatca_compliance_error_message")
    private String zatcaComplianceErrorMessages;

    @Column(name = "zatca_compliance_warning_message")
    private String zatcaComplianceWarningMessages;

    @Column(name = "zatca_compliance_status")
    private String zatcaComplianceStatus;

    @Column(name = "zatca_compliance_clearance_status")
    private String zatcaComplianceClearanceStatus;


    @Column(name = "zatca_reporting_error_message")
    private String zatcaReportingErrorMessages;

    @Column(name = "zatca_reporting_warning_message")
    private String zatcaReportingWarningMessages;

    @Column(name = "zatca_reporting_response", columnDefinition = "TEXT")
    private String zatcaReportingResponse;

    @Column(name = "zatca_reporting_status")
    private String zatcaReportingStatus;

    @Column(name = "zatca_reporting_clearance_status")
    private String zatcaReportingClearanceStatus;

    @Column(name = "cleared_invoice_xml", columnDefinition = "TEXT")
    private String clearedInvoiceXml;

    @Column(name = "is_cancellable")
    private Boolean isCancellable;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // Relationships
    @ManyToOne
    @JoinColumn(name = "reference_invoice_id")
    private InvoicesEntity referenceInvoice;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomersEntity customer;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private TenantUsersEntity createdByUser;

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
