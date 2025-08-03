package com.nuevo.zatca.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuevo.zatca.constants.FatooraCliCommands;
import com.nuevo.zatca.utils.FileUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class FatooraCliService {

    // fatoora csr -csrConfig fileName -privateKey fileName -generatedCsr fileName -pem
    public String fatooraGenerateCsrForFile(String csrConfigFilePathWithFileName) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(FatooraCliCommands.GENERATE_CSR + csrConfigFilePathWithFileName + FatooraCliCommands.NON_PROD_ENVIROMENT);
        return FileUtils.getLatestFileContentForFileType("csr");
    }

    public String fatooraGenerateInvoiceHash(String invoiceFilePathWithFileName) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(FatooraCliCommands.GENERATE_INVOICE_HASH + invoiceFilePathWithFileName);
        String invoiceHash = getInvoiceHashFromStreamOfCliProcess(pr);
        return invoiceHash;
    }

    public JsonNode fatooraGenerateInvoiceRequest(String invoiceFilePathWithFileName) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(FatooraCliCommands.GENERATE_INVOICE_REQUEST + invoiceFilePathWithFileName);
        String requestJson = FileUtils.getLatestFileContentForFileType("json");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode invoiceRequestAsJsonNode = objectMapper.readTree(requestJson);
        return invoiceRequestAsJsonNode;
    }

    public String fatooraSignInvoice(String invoiceFilePathWithFileName) throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(FatooraCliCommands.SIGN_INVOICE + invoiceFilePathWithFileName);
        String invoiceHash = getInvoiceHashFromStreamOfCliProcess(pr);
        return invoiceHash;
    }

    public boolean fatooraValidateInvoice(String invoiceFilePathWithFileName) throws Exception {
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(FatooraCliCommands.VALIDATE_INVOICE + invoiceFilePathWithFileName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            List<String> requiredChecks = List.of("XSD", "EN", "KSA", "PIH", "GLOBAL");
            List<String> passedChecks = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                for (String check : requiredChecks) {
                    if (line.contains("[" + check + "]") && line.contains("PASSED")) {
                        passedChecks.add(check);
                    } else if (check.equals("GLOBAL") && line.contains("GLOBAL VALIDATION RESULT = PASSED")) {
                        passedChecks.add("GLOBAL");
                    }
                }
            }
            pr.waitFor(); // Wait for process to complete
            return passedChecks.containsAll(requiredChecks);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String fatooraGenerateQrForInvoice(String invoiceFilePathWithFileName) throws Exception {
        String qrCode = null;
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(FatooraCliCommands.GENERATE_QR_FOR_INVOICE + invoiceFilePathWithFileName);

            qrCode = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("*** QR code =")) {
                    qrCode = line.substring(line.indexOf("=") + 1).trim();
                }
            }

            pr.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return qrCode;
    }


    public String getInvoiceHashFromStreamOfCliProcess(Process pr) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line;
        String invoiceHash = null;

        while ((line = reader.readLine()) != null) {
            if (line.contains("*** INVOICE HASH =")) {
                // Extract hash from the line
                invoiceHash = line.split("= ")[1].trim();
                break;
            }
        }
        try {
            int exitCode = pr.waitFor();
            if (exitCode != 0) {
                throw new IOException("fatoora command failed with exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Process interrupted", e);
        }
        if (invoiceHash == null) {
            throw new IOException("Invoice hash not found in the command output");
        }
        System.out.println("InvoiceHasH :" + invoiceHash);
        return invoiceHash;
    }
}
