package com.nuevo.zatca.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nuevo.zatca.entity.ZatcaOnboardingCredentialsEntity;
import com.nuevo.zatca.repository.ZatcaOnboardingCredentialsRepository;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 Step1: onboardEgsUnit [one time setup only]
 Step2: generateZatcaProductionCSID

 */

@Service
public class ZatcaService {

    @Value("${application.endpoint}")
    private String applicationEndpoint;

    @Value("${zatca.endpoint}")
    private String zatcaEndpoint;

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    ZatcaOnboardingCredentialsRepository zatcaOnboardingCredentialsRepository;

    @Autowired
    FatooraCliService fatooraCliService;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getCsr(String fileNameWithPath) throws JsonProcessingException {
        String generateCsrUrl = applicationEndpoint + "/api/csr/generateCsr";

        Map<String, String> payload = new HashMap<>();
        payload.put("filePathWithFileName", fileNameWithPath);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload);
        ResponseEntity<String> csrResponse = restTemplate.exchange(
                generateCsrUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(csrResponse.getBody());
        String csrValue = jsonNode.get("csr").asText();
        return csrValue;
    }

    public ResponseEntity<JsonNode> onboardEgsUnit(@RequestBody Map<String, Object> requestBody) throws JsonProcessingException {
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("OTP", requestBody.get("otp").toString());
        headers.set("Accept-version", requestBody.get("acceptVersion").toString());

        String csr = getCsr(requestBody.get("onboardingPropertiesFileNameWithPath").toString());
        if (csr == null || csr.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode error = mapper.createObjectNode();
            error.put("error", "CSR is missing");
            return ResponseEntity.badRequest().body(error);
        }

        // Prepare payload
        Map<String, String> payload = new HashMap<>();
        payload.put("csr", csr);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

        String complianceUrl = zatcaEndpoint + "/compliance";

        // Make the request
        ResponseEntity<String> response = restTemplate.exchange(
                complianceUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();

        if(response.getStatusCode().equals(HttpStatus.OK)){
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String binarySecurityToken = jsonNode.get("binarySecurityToken").asText();
            String secret = jsonNode.get("secret").asText();
            String requestId = jsonNode.get("requestID").asText();

            ZatcaOnboardingCredentialsEntity entity = new ZatcaOnboardingCredentialsEntity();
            entity.setBinarySecurityToken(binarySecurityToken);
            entity.setSecret(secret);
            entity.setCsr(csr);
            entity.setSolutionProviderName(requestBody.get("solutionProviderName").toString());
            entity.setComplianceRequestId(requestId);
            zatcaOnboardingCredentialsRepository.save(entity);
        }

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return ResponseEntity.ok(responseJson);
    }

    public ResponseEntity<JsonNode> verifyInvoiceIsZatcaCompliant(Map<String, Object> requestBody) throws JsonProcessingException {
        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity  = zatcaOnboardingCredentialsRepository.findBySolutionProviderName(requestBody.get("solutionProviderName").toString());
        String username = zatcaOnboardingCredentialsEntity.getBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        ObjectMapper objectMapper = new ObjectMapper();
        Object generatedInvoiceObj = requestBody.get("generatedInvoiceRequest");
        JsonNode jsonNode = objectMapper.valueToTree(generatedInvoiceObj);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Accept-Language", requestBody.get("acceptLanguage").toString());
        headers.set("Accept-version", requestBody.get("acceptVersion").toString());
        headers.set("Authorization", "Basic " + base64Creds);

        HttpEntity<JsonNode> requestEntity = new HttpEntity<>(jsonNode, headers);
        String invoiceComplianceUrl = zatcaEndpoint + "/compliance/invoices";

        ResponseEntity<String> response = restTemplate.exchange(
                invoiceComplianceUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        JsonNode responseJson = objectMapper.readTree(response.getBody());

        return ResponseEntity.ok(responseJson);
    }

    public ResponseEntity<JsonNode> generateZatcaProductionCSID(Map<String, Object> requestBody) throws JsonProcessingException {
        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity = zatcaOnboardingCredentialsRepository.findBySolutionProviderName(requestBody.get("solutionProviderName").toString());
        String username = zatcaOnboardingCredentialsEntity.getBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("Accept-version", requestBody.get("acceptVersion").toString());
        headers.set("Authorization", "Basic " + base64Creds);

        HashMap<String, String> payload = new HashMap<>();
        payload.put("compliance_request_id", zatcaOnboardingCredentialsEntity.getComplianceRequestId());

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);
        String invoiceComplianceUrl = zatcaEndpoint + "/production/csids";

        ResponseEntity<String> response = restTemplate.exchange(
                invoiceComplianceUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.readTree(response.getBody());

        if(response.getStatusCode().equals(HttpStatus.OK)){
            zatcaOnboardingCredentialsEntity.setProdComplianceRequestId(responseJson.get("requestID").asText());
            zatcaOnboardingCredentialsEntity.setProdDispositionMessage(responseJson.get("dispositionMessage").asText());
            zatcaOnboardingCredentialsEntity.setProdBinarySecurityToken(responseJson.get("binarySecurityToken").asText());
            zatcaOnboardingCredentialsEntity.setProdSecret(responseJson.get("secret").asText());
            zatcaOnboardingCredentialsRepository.save(zatcaOnboardingCredentialsEntity);
        }

        return ResponseEntity.ok(responseJson);
    }

    public ResponseEntity<JsonNode> reportSimplifiedInvoiceOrCreditNoteOrDebitNoteToZatca(Map<String, Object> requestBody) throws JsonProcessingException {

        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity  = zatcaOnboardingCredentialsRepository.findBySolutionProviderName(requestBody.get("solutionProviderName").toString());
        String username = zatcaOnboardingCredentialsEntity.getProdBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getProdSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("Accept-version", requestBody.get("acceptVersion").toString());
        headers.set("Accept-Language", requestBody.get("acceptLanguage").toString());
        headers.set("Authorization", "Basic " + base64Creds);

        ObjectMapper objectMapper = new ObjectMapper();
        Object generatedInvoiceObj = requestBody.get("generatedInvoiceRequest");
        JsonNode jsonNode = objectMapper.valueToTree(generatedInvoiceObj);

        HttpEntity<JsonNode> requestEntity = new HttpEntity<>(jsonNode, headers);
        String invoiceComplianceUrl = zatcaEndpoint + "/invoices/reporting/single";

        ResponseEntity<String> response = restTemplate.exchange(
                invoiceComplianceUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return ResponseEntity.ok(responseJson);
    }


    public ResponseEntity<JsonNode> reportStandardInvoiceOrCreditNoteOrDebitNoteToZatca(Map<String, Object> requestBody) throws JsonProcessingException {

        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity  = zatcaOnboardingCredentialsRepository.findBySolutionProviderName(requestBody.get("solutionProviderName").toString());
        String username = zatcaOnboardingCredentialsEntity.getProdBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getProdSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("Accept-version", requestBody.get("acceptVersion").toString());
        headers.set("Accept-Language", requestBody.get("acceptLanguage").toString());
        headers.set("Authorization", "Basic " + base64Creds);

        ObjectMapper objectMapper = new ObjectMapper();
        Object generatedInvoiceObj = requestBody.get("generatedInvoiceRequest");
        JsonNode jsonNode = objectMapper.valueToTree(generatedInvoiceObj);

        HttpEntity<JsonNode> requestEntity = new HttpEntity<>(jsonNode, headers);
        String invoiceComplianceUrl = zatcaEndpoint + "/invoices/clearance/single";

        ResponseEntity<String> response = restTemplate.exchange(
                invoiceComplianceUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return ResponseEntity.ok(responseJson);
    }
}
