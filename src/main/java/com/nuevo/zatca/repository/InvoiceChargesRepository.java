package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.InvoiceChargesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceChargesRepository extends JpaRepository<InvoiceChargesEntity, String> {
}
