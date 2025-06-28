package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.InvoiceDiscountsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceDiscountsRepository extends JpaRepository<InvoiceDiscountsEntity, String> {
}
