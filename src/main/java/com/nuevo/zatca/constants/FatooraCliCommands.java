package com.nuevo.zatca.constants;

public final class FatooraCliCommands {

    private FatooraCliCommands() {
        // Prevent instantiation
    }

    private static final String FATOORA = "fatoora ";
    private static final String INVOICE = "-invoice ";
    public static final String NON_PROD_ENVIROMENT = " -nonprod";
    public static final String SIMULATION_ENVIROMENT = "-sim";


    public static final String GENERATE_CSR = FATOORA + "-csr -csrConfig ";
    public static final String VALIDATE_INVOICE = FATOORA + "-validate " + INVOICE;
    public static final String GENERATE_QR_FOR_INVOICE = FATOORA + "-qr " + INVOICE;
    public static final String GENERATE_INVOICE_HASH = FATOORA + "-generateHash " + INVOICE;
    public static final String GENERATE_INVOICE_REQUEST = FATOORA + "-invoiceRequest " + INVOICE;
    public static final String SIGN_INVOICE = FATOORA + "-sign " + INVOICE;
}