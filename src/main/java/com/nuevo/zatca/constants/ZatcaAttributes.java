package com.nuevo.zatca.constants;

/**
1000 = Standard Tax Invoice (B2B/B2G)
0100 = Simplified Tax Invoice (B2C)
0010 = Credit Note
0001 = Debit Note
1100 = Supports Standard and Simplified Invoices
1111 = Supports all types
* */

public final class ZatcaAttributes {

    private ZatcaAttributes() {
        //prevents initialization
    }

    public static final String INVOICE_TYPE_B2B_B2G = "1000";
    public static final String INVOICE_TYPE_B2C = "0100";
    public static final String INVOICE_TYPE_CREDIT_NOTE = "0010";
    public static final String INVOICE_TYPE_DEBIT_NOTE = "0001";
    public static final String INVOICE_TYPE_B2B_B2G_B2C = "1100";
    public static final String INVOICE_TYPE_ALL = "1111";
}
