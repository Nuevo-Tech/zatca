package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.CustomersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomersRepository extends JpaRepository<CustomersEntity, String> {
}
