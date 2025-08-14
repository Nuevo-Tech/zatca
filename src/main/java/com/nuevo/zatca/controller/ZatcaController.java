package com.nuevo.zatca.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nuevo.zatca.service.ZatcaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/zatca")
@Tag(name = "Zatca Controller", description = "Controller for managing Zatca APIs")
public class ZatcaController {
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    ZatcaService zatcaService;

    @Value("${zatca.endpoint}")
    private String zatcaEndpoint;

    @Value("${application.endpoint}")
    private String applicationEndpoint;


    @Operation(summary = "Onboarding EGS Unit and generate CSID")
    @PostMapping("/onboardClient")
    public ResponseEntity<JsonNode> onboardEGSAndGenerateZatcaCSID(@RequestBody Map<String, Object> requestBody) throws IOException {
        return zatcaService.onboardClient(requestBody);
    }

    @Operation(summary = "Checking with zatca the invoice is in compliance")
    @PostMapping("/compliance/invoice")
    public ResponseEntity<JsonNode> performComplianceCheckOnInvoiceZatca(@RequestBody Map<String, Object> requestBody) throws JsonProcessingException {
        return zatcaService.verifyInvoiceIsZatcaCompliant(requestBody);
    }

    @Operation(summary = "Generate Invoice request and update it with newly generated invoice hash")
    @PostMapping("/generateInvoiceRequest")
    public ResponseEntity<JsonNode> genrerateInvoiceRequestAndUpdateInvoiceHash(@RequestBody Map<String, Object> requestBody) throws JsonProcessingException {
        return zatcaService.verifyInvoiceIsZatcaCompliant(requestBody);
    }

    @Operation(summary = "Generate zatca Production CSID")
    @PostMapping("/production/csids")
    public ResponseEntity<JsonNode> generateZatcaProductionCSID(@RequestBody Map<String, Object> requestBody) throws JsonProcessingException {
        return zatcaService.generateZatcaProductionCSID(requestBody);
    }

    @Operation(summary = "Report the simplified invoice/debit note/credit note to zatca")
    @PostMapping("/invoices/reporting/single")
    public ResponseEntity<JsonNode> reportSimplifiedInvoiceOrCreditNoteOrDebitNoteToZatca(@RequestBody Map<String, Object> requestBody) throws JsonProcessingException {
        return zatcaService.reportSimplifiedInvoiceOrCreditNoteOrDebitNoteToZatca(requestBody);
    }

    @Operation(summary = "Report the standard invoice/debit note/credit note to zatca")
    @PostMapping("/invoices/clearance/single")
    public ResponseEntity<JsonNode> reportStandardInvoiceOrCreditNoteOrDebitNoteToZatca(@RequestBody Map<String, Object> requestBody) throws JsonProcessingException {
        return zatcaService.reportStandardInvoiceOrCreditNoteOrDebitNoteToZatca(requestBody);
    }
}
