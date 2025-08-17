package com.nuevo.zatca.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nuevo.zatca.constants.ZatcaAttributes;
import com.nuevo.zatca.entity.EgsClientEntity;
import com.nuevo.zatca.entity.InvoicesEntity;
import com.nuevo.zatca.entity.ZatcaOnboardingCredentialsEntity;
import com.nuevo.zatca.repository.EgsClientRepository;
import com.nuevo.zatca.repository.InvoicesRepository;
import com.nuevo.zatca.repository.ZatcaOnboardingCredentialsRepository;
import com.nuevo.zatca.utils.FileUtils;
import io.micrometer.observation.ObservationRegistry;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Step1: onboardEgsUnit [one time setup only]
 * Step2: generateZatcaProductionCSID
 */

@Service
public class ZatcaService {

    @Value("${application.endpoint}")
    private String applicationEndpoint;

    @Value("${zatca.endpoint}")
    private String zatcaEndpoint;

    @Value("${zatca.accept.version}")
    private String zatcaAcceptVersion;

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    ZatcaOnboardingCredentialsRepository zatcaOnboardingCredentialsRepository;

    @Autowired
    InvoicesRepository invoicesRepository;

    @Autowired
    FatooraCliService fatooraCliService;

    @Autowired
    EgsClientService egsClientService;

    @Autowired
    EgsClientRepository egsClientRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public ResponseEntity<JsonNode> onboardClient(@RequestBody Map<String, Object> requestBody) throws IOException {
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("OTP", requestBody.get("otp").toString());
        headers.set("Accept-version", zatcaAcceptVersion);

        String egsClientName = requestBody.get("egs_client_name").toString();

        String csrPropertiesFileNameWithPath = createCsrPropertiesFileAndGetItsLocation(requestBody);
        String csr = fatooraCliService.fatooraGenerateCsrForFile(csrPropertiesFileNameWithPath);

        String csrPrivateKey = "";
        if (csr != null || !csr.isEmpty()) {
            String parentPath = new File(csrPropertiesFileNameWithPath).getParent();
            csrPrivateKey = FileUtils.getLatestFileContentForFileType("key", parentPath);
        }


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

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String binarySecurityToken = jsonNode.get("binarySecurityToken").asText();
            String secret = jsonNode.get("secret").asText();
            String requestId = jsonNode.get("requestID").asText();

            ZatcaOnboardingCredentialsEntity entity = new ZatcaOnboardingCredentialsEntity();
            entity.setBinarySecurityToken(binarySecurityToken);
            entity.setSecret(secret);
            entity.setCsr(csr);
            entity.setEgsClientName(egsClientName);
            entity.setComplianceRequestId(requestId);
            entity.setCsrPrivateKey(csrPrivateKey);

            EgsClientEntity egsClientEntity = egsClientRepository.findByEgsClientName(egsClientName);
            entity.setEgsClientEntity(egsClientEntity);
            ZatcaOnboardingCredentialsEntity savedEntity = zatcaOnboardingCredentialsRepository.save(entity);

            egsClientEntity.setZatcaOnboardingCredentialsEntity(savedEntity);

            //payload to generate zatca production CSID
            Map<String, Object> reqBodyToGeneratedProdCSID = new HashMap<>();
            reqBodyToGeneratedProdCSID.put("acceptVersion", zatcaAcceptVersion);
            reqBodyToGeneratedProdCSID.put("clientName", egsClientName);

            ResponseEntity<JsonNode> responseProdCSID = generateZatcaProductionCSID(reqBodyToGeneratedProdCSID);
            JsonNode responseJson = responseProdCSID.getBody();

            if (responseProdCSID.getStatusCode().equals(HttpStatus.OK)) {
                Map<String, String> map = new HashMap<>();
                map.put("message", "Successfully Onboarded the Client");
                JsonNode successMessage = objectMapper.valueToTree(map);
                return ResponseEntity.ok(successMessage);
            }
            return ResponseEntity.ok(responseJson);
        }
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return ResponseEntity.ok(responseJson);
    }

    @Transactional
    public String createCsrPropertiesFileAndGetItsLocation(Map<String, Object> requestBody) throws IOException {
        String egsClientName = requestBody.get("egs_client_name").toString();
        String vatRegistrationNumber = requestBody.get("vat_registration_number").toString();
        String city = requestBody.get("city").toString();
        String address = requestBody.get("address").toString();
        String countryCode = requestBody.get("country_code").toString();
        String country = "";
        String businessType = requestBody.get("business_type").toString();
        String locationAddress = requestBody.get("location_address").toString();
        String industryType = requestBody.get("industry_type").toString();
        String contactNumber = requestBody.get("contact_number").toString();
        String email = requestBody.get("email").toString();
        String zipCode = requestBody.get("zip_code").toString();
        String organizationUnit = requestBody.get("organization_unit").toString();
        if (organizationUnit == null || organizationUnit.isEmpty()) {
            organizationUnit = city;
        }

        String egsClientSerielNumber = egsClientService.getEgsClientUniqueSerielNumber(egsClientName);

        switch (businessType) {
            case "B2B/B2G":
                businessType = ZatcaAttributes.INVOICE_TYPE_B2B_B2G;
                break;

            case "B2B/B2G/B2C":
                businessType = ZatcaAttributes.INVOICE_TYPE_B2B_B2G_B2C;
                break;

            default:
                throw new IllegalArgumentException("Invalid business type: " + businessType);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("csr.common.name=").append(egsClientName).append(" E Invoicing").append("\n");
        sb.append("csr.serial.number=").append("1-EInvoicing|2-Version1|3-").append(egsClientSerielNumber).append("\n");
        sb.append("csr.organization.identifier=").append(vatRegistrationNumber).append("\n");
        sb.append("csr.organization.unit.name=").append(organizationUnit).append("\n");
        sb.append("csr.organization.name=").append(egsClientName).append("\n");
        sb.append("csr.country.name=").append(countryCode).append("\n");
        sb.append("csr.invoice.type=").append(businessType).append("\n");
        sb.append("csr.location.address=").append(locationAddress).append("\n");
        sb.append("csr.industry.business.category=").append(industryType);
        String csrProperties = sb.toString();


        String dirName = egsClientSerielNumber;
        Path directoryPath = Paths.get("src", "main", "resources", "egsclients", dirName);
        Files.createDirectories(directoryPath); // Creates directory if not exists

        String fileName = "csr-" + egsClientSerielNumber + ".properties";
        // Target file inside the new directory
        Path filePath = directoryPath.resolve(fileName);

        try {
            // Write the file
            Files.write(filePath, csrProperties.getBytes());
            System.out.println("File written successfully at: " + directoryPath);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        String fileNameWithPath = directoryPath + "/" + fileName;
        EgsClientEntity egsClientEntity = new EgsClientEntity();
        egsClientEntity.setFilepathCsrProperties(fileNameWithPath);
        egsClientEntity.setEgsClientName(egsClientName);
        egsClientEntity.setVatRegistrationNumber(vatRegistrationNumber);
        egsClientEntity.setEgsClientSerielNumber(egsClientSerielNumber);
        egsClientEntity.setCity(city);
        egsClientEntity.setCountry(country);
        egsClientEntity.setCountryCode(countryCode);
        egsClientEntity.setLocationAddress(locationAddress);
        egsClientEntity.setAddress(address);
        egsClientEntity.setZipCode(zipCode);
        egsClientEntity.setIndustryType(industryType);
        egsClientEntity.setContactNumber(contactNumber);
        egsClientEntity.setOrganizationUnit(organizationUnit);
        egsClientEntity.setEmail(email);
        egsClientRepository.save(egsClientEntity);
        return fileNameWithPath;
    }

    @Transactional
    public ResponseEntity<JsonNode> generateZatcaProductionCSID(Map<String, Object> requestBody) throws JsonProcessingException {
        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity = zatcaOnboardingCredentialsRepository.findByEgsClientName(requestBody.get("clientName").toString());
        String username = zatcaOnboardingCredentialsEntity.getBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("Accept-version", zatcaAcceptVersion);
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

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            zatcaOnboardingCredentialsEntity.setProdComplianceRequestId(responseJson.get("requestID").asText());
            zatcaOnboardingCredentialsEntity.setProdDispositionMessage(responseJson.get("dispositionMessage").asText());
            zatcaOnboardingCredentialsEntity.setProdBinarySecurityToken(responseJson.get("binarySecurityToken").asText());
            zatcaOnboardingCredentialsEntity.setProdSecret(responseJson.get("secret").asText());
            zatcaOnboardingCredentialsRepository.save(zatcaOnboardingCredentialsEntity);
        }

        return ResponseEntity.ok(responseJson);
    }


    @Transactional
    public ResponseEntity<JsonNode> verifyInvoiceIsZatcaCompliant(Map<String, Object> requestBody) throws Exception {
        String egsClientName = requestBody.get("egsClientName").toString();
        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity = zatcaOnboardingCredentialsRepository.findByEgsClientName(egsClientName);
        String username = zatcaOnboardingCredentialsEntity.getBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());
        String invoiceId = requestBody.get("invoiceId").toString();

        String dirName = "xml_invoice_id_" + invoiceId;
        Path directoryPath = Paths.get("src", "main", "resources", "xmlinvoices", dirName);

        //TODO : write code to create xml invoices for simplified/standard/creditnote/debitnote here

        String fileName = "/" + "Standard_Invoice.xml";

        Files.createDirectories(directoryPath); // Creates directory if not exists
        JsonNode generatedInvoiceRequest = generateInvoiceRequestAndUpdateTheInvoiceHash(directoryPath + fileName);
        String invoiceHash = generatedInvoiceRequest.get("invoiceHash").asText();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Accept-Language", requestBody.get("acceptLanguage").toString());
        headers.set("Accept-version", zatcaAcceptVersion);
        headers.set("Authorization", "Basic " + base64Creds);

        HttpEntity<JsonNode> requestEntity = new HttpEntity<>(generatedInvoiceRequest, headers);
        String invoiceComplianceUrl = zatcaEndpoint + "/compliance/invoices";

        ResponseEntity<String> response = restTemplate.exchange(
                invoiceComplianceUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode validationResults = responseJson.get("validationResults");
        String status = validationResults.get("status").asText();
        String errorMessages = validationResults.get("errorMessages").asText();
        String warningMessages = validationResults.get("warningMessages").asText();
        String clearanceStatus = responseJson.get("clearanceStatus").asText();

        InvoicesEntity invoicesEntity = new InvoicesEntity();
        invoicesEntity.setZatcaComplianceRequestPayload(generatedInvoiceRequest.toString());
        invoicesEntity.setZatcaComplianceResponse(response.getBody());
        invoicesEntity.setZatcaComplianceStatus(status);
        invoicesEntity.setZatcaComplianceErrorMessages(errorMessages);
        invoicesEntity.setZatcaComplianceWarningMessages(warningMessages);
        invoicesEntity.setZatcaHash(invoiceHash);
        invoicesEntity.setZatcaComplianceClearanceStatus(clearanceStatus);
        invoicesRepository.save(invoicesEntity);
        return ResponseEntity.ok(responseJson);
    }


    public JsonNode generateInvoiceRequestAndUpdateTheInvoiceHash(String invoiceXmlFileNameWithPath) throws IOException {
        FatooraCliService fatooraCliService = new FatooraCliService();
        JsonNode invoiceRequest = fatooraCliService.fatooraGenerateInvoiceRequest(invoiceXmlFileNameWithPath);
        String invoiceHash = fatooraCliService.fatooraGenerateInvoiceHash(invoiceXmlFileNameWithPath);
        invoiceRequest = ((ObjectNode) invoiceRequest).put("invoiceHash", invoiceHash);

        return invoiceRequest;
    }


    public ResponseEntity<JsonNode> reportSimplifiedInvoiceOrCreditNoteOrDebitNoteToZatca(Map<String, Object> requestBody) throws JsonProcessingException {

        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity = zatcaOnboardingCredentialsRepository.findByEgsClientName(requestBody.get("clientName").toString());
        String username = zatcaOnboardingCredentialsEntity.getProdBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getProdSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("Accept-version", zatcaAcceptVersion);
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

        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity = zatcaOnboardingCredentialsRepository.findByEgsClientName(requestBody.get("clientName").toString());
        String username = zatcaOnboardingCredentialsEntity.getProdBinarySecurityToken();
        String password = zatcaOnboardingCredentialsEntity.getProdSecret();
        String plainCreds = username + ":" + password;
        String base64Creds = Base64.getEncoder().encodeToString(plainCreds.getBytes());

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("Accept-version", zatcaAcceptVersion);
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
