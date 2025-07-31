package com.nuevo.zatca.repository;

import com.nuevo.zatca.entity.TenantUsersEntity;
import com.nuevo.zatca.entity.ZatcaOnboardingCredentialsEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface ZatcaOnboardingCredentialsRepository  extends JpaRepository<ZatcaOnboardingCredentialsEntity, String> {

}
