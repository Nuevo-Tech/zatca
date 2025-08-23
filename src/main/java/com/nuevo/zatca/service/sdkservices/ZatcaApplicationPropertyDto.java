package com.nuevo.zatca.service.sdkservices;

import com.zatca.sdk.dto.ApplicationPropertyDto;

import java.io.InputStream;

public class ZatcaApplicationPropertyDto extends ApplicationPropertyDto {
    private boolean generateQr;
    private boolean generateSignature;
    private boolean generateInvoiceRequest;
    private boolean generateCsr;
    private boolean outputPemFormat;
    private boolean validateInvoice;
    private boolean nonPrdServer;
    private boolean isSimulation;
    private boolean generateHash;
    private String invoiceFileName;
    private String outputInvoiceFileName;
    private String invoiceRequestFileName;
    private String csrConfigFileName;
    private String privateKeyFileName;
    private String csrFileName;
    private InputStream fileInputStream;

    public ZatcaApplicationPropertyDto() {
    }

    public ZatcaApplicationPropertyDto(boolean generateQr, String invoiceFileName) {
        this.generateQr = generateQr;
        this.invoiceFileName = invoiceFileName;
    }

    public ZatcaApplicationPropertyDto(boolean generateSignature, String invoiceFileName, String outputInvoiceFileName) {
        this.generateSignature = generateSignature;
        this.invoiceFileName = invoiceFileName;
        this.outputInvoiceFileName = outputInvoiceFileName;
    }

    public ZatcaApplicationPropertyDto(boolean generateInvoiceRequest, InputStream fileInputStream, boolean generateSignature, String outputInvoiceFileName) {
        this.generateInvoiceRequest = generateInvoiceRequest;
       this.fileInputStream = fileInputStream;
        this.generateSignature = generateSignature;
    }

    public ZatcaApplicationPropertyDto(boolean generateHash, InputStream fileInputStream, boolean generateQr) {
        this.generateHash = generateHash;
        this.fileInputStream = fileInputStream;
        this.generateQr = generateQr;
    }

    public ZatcaApplicationPropertyDto(boolean validateInvoice, boolean generateHash, String invoiceFileName, String invoiceRequestFileName, String outputInvoiceFileName, boolean generateQr) {
        this.validateInvoice = validateInvoice;
        this.generateHash = generateHash;
        this.invoiceFileName = invoiceFileName;
        this.invoiceRequestFileName = invoiceRequestFileName;
        this.outputInvoiceFileName = outputInvoiceFileName;
        this.generateQr = generateQr;
    }

    public ZatcaApplicationPropertyDto(boolean generateCsr, InputStream fileInputStream, boolean isPemFormat, boolean isNonPrd, boolean simulation) {
        this.generateCsr = generateCsr;
        this.fileInputStream = fileInputStream;
        this.outputPemFormat = isPemFormat;
        this.nonPrdServer = isNonPrd;
        this.isSimulation = simulation;
    }

    public boolean isGenerateQr() {
        return this.generateQr;
    }

    public boolean isGenerateSignature() {
        return this.generateSignature;
    }

    public boolean isGenerateInvoiceRequest() {
        return this.generateInvoiceRequest;
    }

    public boolean isGenerateCsr() {
        return this.generateCsr;
    }

    public boolean isOutputPemFormat() {
        return this.outputPemFormat;
    }

    public boolean isValidateInvoice() {
        return this.validateInvoice;
    }

    public boolean isNonPrdServer() {
        return this.nonPrdServer;
    }

    public boolean isSimulation() {
        return this.isSimulation;
    }

    public boolean isGenerateHash() {
        return this.generateHash;
    }

    public boolean isInput() {
        return this.generateCsr;
    }

    public String getInvoiceFileName() {
        return this.invoiceFileName;
    }

    public String getOutputInvoiceFileName() {
        return this.outputInvoiceFileName;
    }

    public String getInvoiceRequestFileName() {
        return this.invoiceRequestFileName;
    }

    public String getCsrConfigFileName() {
        return this.csrConfigFileName;
    }

    public String getPrivateKeyFileName() {
        return this.privateKeyFileName;
    }

    public String getCsrFileName() {
        return this.csrFileName;
    }

    public InputStream getFileInputStream() {
        return this.fileInputStream;
    }

    public void setGenerateQr(boolean generateQr) {
        this.generateQr = generateQr;
    }

    public void setGenerateSignature(boolean generateSignature) {
        this.generateSignature = generateSignature;
    }

    public void setGenerateInvoiceRequest(boolean generateInvoiceRequest) {
        this.generateInvoiceRequest = generateInvoiceRequest;
    }

    public void setGenerateCsr(boolean generateCsr) {
        this.generateCsr = generateCsr;
    }

    public void setOutputPemFormat(boolean outputPemFormat) {
        this.outputPemFormat = outputPemFormat;
    }

    public void setValidateInvoice(boolean validateInvoice) {
        this.validateInvoice = validateInvoice;
    }

    public void setNonPrdServer(boolean nonPrdServer) {
        this.nonPrdServer = nonPrdServer;
    }

    public void setSimulation(boolean isSimulation) {
        this.isSimulation = isSimulation;
    }

    public void setGenerateHash(boolean generateHash) {
        this.generateHash = generateHash;
    }

    public void setInvoiceFileName(String invoiceFileName) {
        this.invoiceFileName = invoiceFileName;
    }

    public void setOutputInvoiceFileName(String outputInvoiceFileName) {
        this.outputInvoiceFileName = outputInvoiceFileName;
    }

    public void setInvoiceRequestFileName(String invoiceRequestFileName) {
        this.invoiceRequestFileName = invoiceRequestFileName;
    }

    public void setCsrConfigFileName(String csrConfigFileName) {
        this.csrConfigFileName = csrConfigFileName;
    }

    public void setPrivateKeyFileName(String privateKeyFileName) {
        this.privateKeyFileName = privateKeyFileName;
    }

    public void setCsrFileName(String csrFileName) {
        this.csrFileName = csrFileName;
    }

    public void setFileInputStream(InputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ZatcaApplicationPropertyDto)) {
            return false;
        } else {
            ZatcaApplicationPropertyDto other = (ZatcaApplicationPropertyDto) o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.isGenerateQr() != other.isGenerateQr()) {
                return false;
            } else if (this.isGenerateSignature() != other.isGenerateSignature()) {
                return false;
            } else if (this.isGenerateInvoiceRequest() != other.isGenerateInvoiceRequest()) {
                return false;
            } else if (this.isGenerateCsr() != other.isGenerateCsr()) {
                return false;
            } else if (this.isOutputPemFormat() != other.isOutputPemFormat()) {
                return false;
            } else if (this.isValidateInvoice() != other.isValidateInvoice()) {
                return false;
            } else if (this.isNonPrdServer() != other.isNonPrdServer()) {
                return false;
            } else if (this.isSimulation() != other.isSimulation()) {
                return false;
            } else if (this.isGenerateHash() != other.isGenerateHash()) {
                return false;
            } else {
                Object this$invoiceFileName = this.getInvoiceFileName();
                Object other$invoiceFileName = other.getInvoiceFileName();
                if (this$invoiceFileName == null) {
                    if (other$invoiceFileName != null) {
                        return false;
                    }
                } else if (!this$invoiceFileName.equals(other$invoiceFileName)) {
                    return false;
                }

                Object this$outputInvoiceFileName = this.getOutputInvoiceFileName();
                Object other$outputInvoiceFileName = other.getOutputInvoiceFileName();
                if (this$outputInvoiceFileName == null) {
                    if (other$outputInvoiceFileName != null) {
                        return false;
                    }
                } else if (!this$outputInvoiceFileName.equals(other$outputInvoiceFileName)) {
                    return false;
                }

                Object this$invoiceRequestFileName = this.getInvoiceRequestFileName();
                Object other$invoiceRequestFileName = other.getInvoiceRequestFileName();
                if (this$invoiceRequestFileName == null) {
                    if (other$invoiceRequestFileName != null) {
                        return false;
                    }
                } else if (!this$invoiceRequestFileName.equals(other$invoiceRequestFileName)) {
                    return false;
                }

                Object this$csrConfigFileName = this.getCsrConfigFileName();
                Object other$csrConfigFileName = other.getCsrConfigFileName();
                if (this$csrConfigFileName == null) {
                    if (other$csrConfigFileName != null) {
                        return false;
                    }
                } else if (!this$csrConfigFileName.equals(other$csrConfigFileName)) {
                    return false;
                }

                Object this$privateKeyFileName = this.getPrivateKeyFileName();
                Object other$privateKeyFileName = other.getPrivateKeyFileName();
                if (this$privateKeyFileName == null) {
                    if (other$privateKeyFileName != null) {
                        return false;
                    }
                } else if (!this$privateKeyFileName.equals(other$privateKeyFileName)) {
                    return false;
                }

                Object this$csrFileName = this.getCsrFileName();
                Object other$csrFileName = other.getCsrFileName();
                if (this$csrFileName == null) {
                    if (other$csrFileName != null) {
                        return false;
                    }
                } else if (!this$csrFileName.equals(other$csrFileName)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof ZatcaApplicationPropertyDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isGenerateQr() ? 79 : 97);
        result = result * 59 + (this.isGenerateSignature() ? 79 : 97);
        result = result * 59 + (this.isGenerateInvoiceRequest() ? 79 : 97);
        result = result * 59 + (this.isGenerateCsr() ? 79 : 97);
        result = result * 59 + (this.isOutputPemFormat() ? 79 : 97);
        result = result * 59 + (this.isValidateInvoice() ? 79 : 97);
        result = result * 59 + (this.isNonPrdServer() ? 79 : 97);
        result = result * 59 + (this.isSimulation() ? 79 : 97);
        result = result * 59 + (this.isGenerateHash() ? 79 : 97);
        Object $invoiceFileName = this.getInvoiceFileName();
        result = result * 59 + ($invoiceFileName == null ? 43 : $invoiceFileName.hashCode());
        Object $outputInvoiceFileName = this.getOutputInvoiceFileName();
        result = result * 59 + ($outputInvoiceFileName == null ? 43 : $outputInvoiceFileName.hashCode());
        Object $invoiceRequestFileName = this.getInvoiceRequestFileName();
        result = result * 59 + ($invoiceRequestFileName == null ? 43 : $invoiceRequestFileName.hashCode());
        Object $csrConfigFileName = this.getCsrConfigFileName();
        result = result * 59 + ($csrConfigFileName == null ? 43 : $csrConfigFileName.hashCode());
        Object $privateKeyFileName = this.getPrivateKeyFileName();
        result = result * 59 + ($privateKeyFileName == null ? 43 : $privateKeyFileName.hashCode());
        Object $csrFileName = this.getCsrFileName();
        result = result * 59 + ($csrFileName == null ? 43 : $csrFileName.hashCode());
        return result;
    }

    public String toString() {
        boolean var10000 = this.isGenerateQr();
        return "ZatcaApplicationPropertyDto(generateQr=" + var10000 + ", generateSignature=" + this.isGenerateSignature() + ", generateInvoiceRequest=" + this.isGenerateInvoiceRequest() + ", generateCsr=" + this.isGenerateCsr() + ", outputPemFormat=" + this.isOutputPemFormat() + ", validateInvoice=" + this.isValidateInvoice() + ", nonPrdServer=" + this.isNonPrdServer() + ", isSimulation=" + this.isSimulation() + ", generateHash=" + this.isGenerateHash() + ", invoiceFileName=" + this.getInvoiceFileName() + ", outputInvoiceFileName=" + this.getOutputInvoiceFileName() + ", invoiceRequestFileName=" + this.getInvoiceRequestFileName() + ", csrConfigFileName=" + this.getCsrConfigFileName() + ", privateKeyFileName=" + this.getPrivateKeyFileName() + ", csrFileName=" + this.getCsrFileName() + ")";
    }
}

