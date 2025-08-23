package com.nuevo.zatca.service.sdkservices;

import com.zatca.sdk.util.XmlUtil;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

public class ZatcaInvoiceRequestGenerationService extends ZatcaGeneratorTemplate {
    private static final Logger LOG = Logger.getLogger(ZatcaInvoiceRequestGenerationService.class);
    private String requestStr;
    private static final String UUID_XPATH_EXPRESSION = "/Invoice/UUID";
    private static final String INVOICE_HASH_XPATH_EXPRESSION = "/Invoice/UBLExtensions/UBLExtension/ExtensionContent/UBLDocumentSignatures/SignatureInformation/Signature/SignedInfo/Reference/DigestValue";

    public Properties getInvoiceData(String invoice) {
        try {
            LOG.debug(" get invoice data");
            Document document = XmlUtil.transform(invoice);
            String uuidValue = "";
            NodeList uuidNodeList = XmlUtil.evaluateXpath(document, "/Invoice/UUID");
            if (uuidNodeList != null) {
                uuidValue = uuidNodeList.item(0).getFirstChild().getNodeValue();
            }

            NodeList invoiceHashNodeList = XmlUtil.evaluateXpath(document, "/Invoice/UBLExtensions/UBLExtension/ExtensionContent/UBLDocumentSignatures/SignatureInformation/Signature/SignedInfo/Reference/DigestValue");
            String invoiceHashValue = "";
            if (invoiceHashNodeList != null && invoiceHashNodeList.getLength() > 0) {
                invoiceHashValue = invoiceHashNodeList.item(0).getFirstChild().getNodeValue();
            }

            Properties prop = new Properties();
            prop.put("invoiceHash", invoiceHashValue);
            prop.put("uuid", uuidValue);
            prop.put("invoice", invoice);
            return prop;
        } catch (Exception e) {
            LOG.error("failed to generate invoice request - " + e.getMessage());
            return new Properties();
        }
    }

    public String prepareRequestBodyString(Properties prop) {
        LOG.debug("prepare request body");
        return String.format("{\"invoiceHash\":\"%s\",\"uuid\":\"%s\",\"invoice\":\"%s\"}", prop.getProperty("invoiceHash"), prop.getProperty("uuid"), Base64.getEncoder().encodeToString(prop.getProperty("invoice").getBytes()));
    }

    public boolean prepareRequestBody(Properties prop) {
        try {
            LOG.debug("prepare request JSON file");
            this.requestStr = this.prepareRequestBodyString(prop);
            return this.requestStr != null;
        } catch (Exception e) {
            LOG.error("failed to generate invoice request - " + e.getMessage());
            return false;
        }
    }

    private Boolean generateInvoiceRequestFile() {
        try {
            LOG.debug("generate invoice request file ");
            this.generateFile(this.property.getInvoiceRequestFileName(), this.requestStr);
            LOG.info("invoice request has been generated successfully");
            return true;
        } catch (Exception var2) {
            LOG.error("failed to generate invoice request [unable to write the invoice request into file] ");
            return false;
        }
    }

    protected boolean loadInvoiceFile() {
        try {
            LOG.debug("load invoice file");
            if (this.property.isGenerateSignature()) {
                InputStreamReader reader = new InputStreamReader(this.property.getFileInputStream(), StandardCharsets.UTF_8);
                this.invoiceStr = IOUtils.toString(reader);
//                this.invoiceStr = FileUtils.readFileToString(new File(this.property.getOutputInvoiceFileName()), StandardCharsets.UTF_8);
            } else {
//                this.invoiceStr = FileUtils.readFileToString(new File(this.property.getInvoiceFileName()), StandardCharsets.UTF_8);
                InputStreamReader reader = new InputStreamReader(this.property.getFileInputStream(), StandardCharsets.UTF_8);
                this.invoiceStr = IOUtils.toString(reader);
            }
            return true;
        } catch (IOException var2) {
            LOG.error("unable to read the invoice file content ");
            return false;
        }
    }

    public boolean loadInput() {
        return this.loadInvoiceFile();
    }

    public boolean validateInput() {
        return true;
    }

    public boolean process() {
        Properties prop = this.getInvoiceData(this.invoiceStr);
        return prop.isEmpty() ? false : this.prepareRequestBody(prop);
    }

    public String generateOutputContent() {
        return this.requestStr;
    }
}