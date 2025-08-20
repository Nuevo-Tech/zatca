package com.nuevo.zatca.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nuevo.zatca.service.ZatcaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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
    public ResponseEntity<JsonNode> onboardEGSAndGenerateZatcaCSID(@RequestBody Map<String, Object> requestBody) throws Exception {
        return zatcaService.onboardClient(requestBody);
    }

    @Operation(summary = "Checking with zatca the invoice is in compliance")
    @PostMapping("/checkInvoicesCompliance")
    public ResponseEntity<JsonNode> performComplianceCheckOnInvoiceZatca(@RequestBody Map<String, Object> requestBody) throws Exception {
        return zatcaService.verifyInvoiceIsZatcaCompliant(requestBody);
    }

    @Deprecated
    @Operation(summary = "Generate Invoice request and update it with newly generated invoice hash")
    @PostMapping("/generateInvoiceRequest")
    public ResponseEntity<JsonNode> genrerateInvoiceRequestAndUpdateInvoiceHash(@RequestBody Map<String, Object> requestBody) throws Exception {
        return zatcaService.verifyInvoiceIsZatcaCompliant(requestBody);
    }

    @Operation(summary = "Report the simplified invoice/debit note/credit note to zatca")
    @PostMapping("/reportSimplifiedInvoice")
    public ResponseEntity<JsonNode> reportSimplifiedInvoiceOrCreditNoteOrDebitNoteToZatca(@RequestBody Map<String, Object> requestBody) throws JsonProcessingException {
        return zatcaService.reportSimplifiedInvoiceOrCreditNoteOrDebitNoteToZatca(requestBody);
    }

    @Operation(summary = "Report the standard invoice/debit note/credit note to zatca")
    @PostMapping("/reportStandardInvoice")
    public ResponseEntity<JsonNode> reportStandardInvoiceOrCreditNoteOrDebitNoteToZatca(@RequestBody Map<String, Object> requestBody) throws JsonProcessingException {
        return zatcaService.reportStandardInvoiceOrCreditNoteOrDebitNoteToZatca(requestBody);
    }
}
