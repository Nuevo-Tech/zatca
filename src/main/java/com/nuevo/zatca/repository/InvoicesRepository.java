package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.InvoicesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoicesRepository extends JpaRepository<InvoicesEntity, String> {

//    InvoicesEntity findByInvoiceNumber(String invoiceNumber);
    InvoicesEntity findByInvoiceId(String invoiceId);
}
