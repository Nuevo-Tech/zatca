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
import com.nuevo.zatca.service.sdkservices.ZatcaSdkHelper;
import com.nuevo.zatca.utils.RestApiHelpers;
import io.micrometer.observation.ObservationRegistry;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//import com.zatca.sdk.service.CsrGenerationService;
//import com.zatca.configuration.

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

    @Autowired
    RestApiHelpers restApiHelpers;

    @Autowired
    ZatcaSdkHelper zatcaSdkHelper;


    private final RestTemplate restTemplate = new RestTemplate();

    private static final String VALIDATION_RESULTS = "validationResults";
    private static final String STATUS = "status";
    private static final String ERROR_MESSAGES = "errorMessages";
    private static final String CLEARANCE_STATUS = "clearanceStatus";
    private static final String WARNING_MESSAGES = "warningMessages";
    private static final String INVOICE_ID = "invoiceId";
    private static final String MESSAGE = "message";
    private static final String REPORTING_STATUS = "reportingStatus";

    @Transactional
    public ResponseEntity<JsonNode> onboardClient(@RequestBody Map<String, Object> requestBody) throws Exception {
        // Prepare headers

        RestApiHelpers restApiHelpers = new RestApiHelpers();
        HttpHeaders headers = restApiHelpers.setCommonHeadersWithZatcaAcceptVersion(zatcaAcceptVersion);
        headers.set("OTP", requestBody.get("otp").toString());

        String egsClientName = requestBody.get("egs_client_name").toString();

        ObjectMapper mapper = new ObjectMapper();
        InputStream csrPropertiesInputStream = createCsrPropertiesFileAndGetItsLocation(requestBody);
        String csrAndPrivateKeyString = zatcaSdkHelper.generateCsrAndPrivateKey(csrPropertiesInputStream);
        JsonNode csrAndPrivateKeyJson = mapper.readTree(csrAndPrivateKeyString);
        String csr = csrAndPrivateKeyJson.get("csr").asText();
        String csrPrivateKey = csrAndPrivateKeyJson.get("privateKey").asText();

        if (csr == null || csr.isEmpty()) {

            ObjectNode error = mapper.createObjectNode();
            error.put("error", "CSR is missing");
            return ResponseEntity.badRequest().body(error);
        } else if (csrPrivateKey == null || csrPrivateKey.isEmpty()) {
            ObjectNode error = mapper.createObjectNode();
            error.put("error", "privateKey is missing");
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
            ZatcaOnboardingCredentialsEntity savedZatcaCredentialsEntity = zatcaOnboardingCredentialsRepository.save(entity);

            egsClientEntity.setZatcaOnboardingCredentialsEntity(savedZatcaCredentialsEntity);

            ResponseEntity<JsonNode> responseProdCSID = generateZatcaProductionCSID(savedZatcaCredentialsEntity);
            JsonNode responseJson = responseProdCSID.getBody();

            if (responseProdCSID.getStatusCode().equals(HttpStatus.OK)) {
                Map<String, String> map = new HashMap<>();
                map.put(MESSAGE, "Successfully Onboarded the Client");
                JsonNode successMessage = objectMapper.valueToTree(map);
                return ResponseEntity.ok(successMessage);
            }
            return ResponseEntity.ok(responseJson);
        }
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return ResponseEntity.ok(responseJson);
    }

    @Transactional
    public InputStream createCsrPropertiesFileAndGetItsLocation(Map<String, Object> requestBody) throws IOException {
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

        InputStream csrPropertiesInputStream = new ByteArrayInputStream(csrProperties.getBytes(StandardCharsets.UTF_8));

        EgsClientEntity egsClientEntity = new EgsClientEntity();
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
        return csrPropertiesInputStream;
    }

    @Transactional
    public ResponseEntity<JsonNode> generateZatcaProductionCSID(ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity) throws JsonProcessingException {
        // Prepare headers
        HttpHeaders headers = restApiHelpers.setCommonHeadersWithZatcaAcceptVersion(zatcaAcceptVersion);
        headers = restApiHelpers.setZatcaAuthorizationHeader(zatcaOnboardingCredentialsEntity, headers);

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


        HttpHeaders headers = restApiHelpers.setCommonHeadersWithZatcaAcceptVersion(zatcaAcceptVersion);
        headers = restApiHelpers.setZatcaAuthorizationHeader(zatcaOnboardingCredentialsEntity, headers);
        headers.set("Accept-Language", requestBody.get("acceptLanguage").toString());

        List<Map<String, Object>> invoices = (List<Map<String, Object>>) requestBody.get("invoices");

        String invoiceComplianceUrl = zatcaEndpoint + "/compliance/invoices";

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> responseInvoices = new ArrayList<>();


        for (Map<String, Object> invoice : invoices) {
            Map<String, Object> map = new HashMap<>();
            String invoiceId = invoice.get("invoiceId").toString();
            if (invoiceId == null || invoiceId.isEmpty()) {
                map.put("invoiceId", invoiceId + ": is invalid");
                responseInvoices.add(map);
                continue;
            }
            String invoiceType = invoice.get("invoiceType").toString();
            if (!(invoiceType.equals(ZatcaAttributes.SIMPLIFIED_INVOICE) || invoiceType.equals(ZatcaAttributes.STANDARD_INVOICE))) {
                map.put("invoiceId", invoiceId);
                map.put("invoiceType", invoiceType + ": is invalid");
                responseInvoices.add(map);
                continue;
            }
            String dirName = "xml_invoice_id_" + invoiceId;
            Path directoryPath = Paths.get("src", "main", "resources", "xmlinvoices", dirName);

            //TODO : write code to create xml invoices for simplified/standard/creditnote/debitnote here

            String fileName = "/" + "Simplified_Invoice.xml";
            String fileNameWithPath = directoryPath + fileName;

            Files.createDirectories(directoryPath);
            JsonNode generatedInvoiceRequest;
            if (invoiceType.equals(ZatcaAttributes.SIMPLIFIED_INVOICE)) {
                long start = System.nanoTime();
                String signedInvoiceScbml = generateSignedInvoice(fileNameWithPath, zatcaOnboardingCredentialsEntity, false);
                InputStream signedInvoiceScbmlInputStream = new ByteArrayInputStream(signedInvoiceScbml.getBytes(StandardCharsets.UTF_8));
                generatedInvoiceRequest = generateInvoiceRequestAndUpdateTheInvoiceHash(signedInvoiceScbmlInputStream);
                long end = System.nanoTime();
                System.out.println("Time taken: " + (end - start) / 1_000_000 + " ms");
            } else {
                long start = System.nanoTime();
                InputStream standardInvoiceXmlInputStream = new FileInputStream(fileNameWithPath);
                generatedInvoiceRequest = generateInvoiceRequestAndUpdateTheInvoiceHash(standardInvoiceXmlInputStream);
                long end = System.nanoTime();
                System.out.println("Time taken: " + (end - start) / 1_000_000 + " ms");
            }
            // Creates directory if not exists
            String invoiceHash = generatedInvoiceRequest.get("invoiceHash").asText();

            HttpEntity<JsonNode> requestEntity = new HttpEntity<>(generatedInvoiceRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    invoiceComplianceUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode validationResults = responseJson.get(VALIDATION_RESULTS);
            String status = validationResults.get(STATUS).asText();
            String errorMessages = validationResults.get(ERROR_MESSAGES).asText();
            String warningMessages = validationResults.get(WARNING_MESSAGES).asText();
            String clearanceStatus = responseJson.get(CLEARANCE_STATUS).asText();
            String reportingStatus = responseJson.get(REPORTING_STATUS).asText();

            InvoicesEntity invoicesEntity = invoicesRepository.findByInvoiceId(invoiceId);
            if (invoicesEntity == null) {
                invoicesEntity = new InvoicesEntity();
                invoicesEntity.setInvoiceId(invoiceId);
            }

            invoicesEntity.setZatcaComplianceRequestPayload(generatedInvoiceRequest.toString());
            invoicesEntity.setZatcaComplianceResponse(response.getBody());
            invoicesEntity.setZatcaComplianceStatus(status);
            invoicesEntity.setZatcaComplianceErrorMessages(errorMessages);
            invoicesEntity.setZatcaComplianceWarningMessages(warningMessages);
            invoicesEntity.setZatcaHash(invoiceHash);
            invoicesEntity.setZatcaComplianceClearanceStatus(clearanceStatus);
            invoicesEntity.setZatcaComplianceReportingStatus(reportingStatus);
            invoicesEntity.setInvoiceXmlPath(fileNameWithPath);
            invoicesEntity.setInvoiceNumber(invoiceId);
            invoicesRepository.save(invoicesEntity);

            map.put(INVOICE_ID, invoiceId);
            map.put(STATUS, status);
            map.put(CLEARANCE_STATUS, clearanceStatus);
            map.put(WARNING_MESSAGES, validationResults.get(WARNING_MESSAGES));
            map.put(ERROR_MESSAGES, validationResults.get(ERROR_MESSAGES));
            responseInvoices.add(map);
        }

        JsonNode responseJson = objectMapper.valueToTree(responseInvoices);
        return ResponseEntity.ok(responseJson);
    }


    public JsonNode generateInvoiceRequestAndUpdateTheInvoiceHash(InputStream standardInvoiceXmlInputStream) throws Exception {
        byte[] xmlBytes = standardInvoiceXmlInputStream.readAllBytes();
        InputStream standardInvoiceXmlInputStream1 = new ByteArrayInputStream(xmlBytes);
        String invoiceRequestString = zatcaSdkHelper.generateInvoiceRequest(standardInvoiceXmlInputStream1);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode invoiceRequest = mapper.readTree(invoiceRequestString);
        InputStream standardInvoiceXmlInputStream2 = new ByteArrayInputStream(xmlBytes);
        String invoiceHash = zatcaSdkHelper.generateInvoiceHash(standardInvoiceXmlInputStream2);
        invoiceRequest = ((ObjectNode) invoiceRequest).put("invoiceHash", invoiceHash);
        return invoiceRequest;
    }

    public String generateSignedInvoice(String invoiceXmlFileNameWithPath, ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity, boolean prodCreds) throws Exception {
        String privateKey = zatcaOnboardingCredentialsEntity.getCsrPrivateKey();
        String binarySecurityToken = "";

        if (prodCreds) {
            binarySecurityToken = zatcaOnboardingCredentialsEntity.getProdBinarySecurityToken();
        } else {
            binarySecurityToken = zatcaOnboardingCredentialsEntity.getBinarySecurityToken();
        }
        String decodedBinarySecurityToken = new String(Base64.getDecoder().decode(binarySecurityToken));

        InputStream standardInvoiceXmlInputStream = new FileInputStream(invoiceXmlFileNameWithPath);

        String generatedSignedInvoiceXml = zatcaSdkHelper.generateSignedInvoiceXml(standardInvoiceXmlInputStream, privateKey, decodedBinarySecurityToken);
        return generatedSignedInvoiceXml;
    }


    public ResponseEntity<JsonNode> reportSimplifiedInvoiceOrCreditNoteOrDebitNoteToZatca(Map<String, Object> requestBody) throws JsonProcessingException {

        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity = zatcaOnboardingCredentialsRepository.findByEgsClientName(requestBody.get("clientName").toString());
        // Prepare headers
        HttpHeaders headers = restApiHelpers.setCommonHeadersWithZatcaAcceptVersion(zatcaAcceptVersion);
        headers = restApiHelpers.setZatcaProdAuthorizationHeader(zatcaOnboardingCredentialsEntity, headers);
        headers.set("Accept-Language", requestBody.get("acceptLanguage").toString());

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

    @Transactional
    public ResponseEntity<JsonNode> reportStandardInvoiceOrCreditNoteOrDebitNoteToZatca(Map<String, Object> requestBody) throws JsonProcessingException {

        ZatcaOnboardingCredentialsEntity zatcaOnboardingCredentialsEntity = zatcaOnboardingCredentialsRepository.findByEgsClientName(requestBody.get("egsClientName").toString());
        HttpHeaders headers = restApiHelpers.setCommonHeadersWithZatcaAcceptVersion(zatcaAcceptVersion);
        headers = restApiHelpers.setZatcaProdAuthorizationHeader(zatcaOnboardingCredentialsEntity, headers);
        headers.set("Accept-Language", requestBody.get("acceptLanguage").toString());

        List<String> invoiceIds = (List<String>) requestBody.get("invoiceIds");
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> invoices = new ArrayList<>();

        for (String invoiceId : invoiceIds) {
            InvoicesEntity invoicesEntity = invoicesRepository.findByInvoiceId(invoiceId);
            Map<String, Object> map = new HashMap<>();
            if (invoicesEntity == null) {
                map.put(INVOICE_ID, invoiceId);
                map.put(MESSAGE, "InvoiceId not found please validate before submitting to zatca");
                invoices.add(map);
                continue;
            }

            String generatedInvoiceObj = invoicesEntity.getZatcaComplianceRequestPayload();
            JsonNode jsonNode = objectMapper.readTree(generatedInvoiceObj);

            HttpEntity<JsonNode> requestEntity = new HttpEntity<>(jsonNode, headers);
            String invoiceComplianceUrl = zatcaEndpoint + "/invoices/clearance/single";

            ResponseEntity<String> response = restTemplate.exchange(
                    invoiceComplianceUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode validationResults = responseJson.get(VALIDATION_RESULTS);
            String status = validationResults.get(STATUS).asText();
            String errorMessages = validationResults.get(ERROR_MESSAGES).asText();
            String warningMessages = validationResults.get(WARNING_MESSAGES).asText();
            String clearanceStatus = responseJson.get(CLEARANCE_STATUS).asText();
            String encodedClearedInvoiceXml = responseJson.get("clearedInvoice").asText();

            invoicesEntity.setZatcaReportingStatus(status);
            invoicesEntity.setZatcaReportingResponse(response.getBody());
            invoicesEntity.setZatcaReportingClearanceStatus(clearanceStatus);
            invoicesEntity.setZatcaReportingWarningMessages(warningMessages);
            invoicesEntity.setZatcaReportingErrorMessages(errorMessages);
            invoicesEntity.setClearedInvoiceXml(encodedClearedInvoiceXml);
            invoicesRepository.save(invoicesEntity);

            map.put(INVOICE_ID, invoiceId);
            map.put(STATUS, status);
            map.put(CLEARANCE_STATUS, clearanceStatus);
            map.put(WARNING_MESSAGES, validationResults.get(WARNING_MESSAGES));
            map.put(ERROR_MESSAGES, validationResults.get(ERROR_MESSAGES));
            invoices.add(map);
        }
        JsonNode responseJson = objectMapper.valueToTree(invoices);
        return ResponseEntity.ok(responseJson);
    }
}
