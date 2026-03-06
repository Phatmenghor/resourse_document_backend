package com.emenu.features.appkey.models;

import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "app_keys", indexes = {
        @Index(name = "idx_app_key_deleted", columnList = "is_deleted"),
        @Index(name = "idx_app_key_api_key", columnList = "api_key"),
        @Index(name = "idx_app_key_application_name", columnList = "application_name"),
        @Index(name = "idx_app_key_active_deleted", columnList = "is_active, is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppKey extends BaseUUIDEntity {

    @Column(name = "application_name", nullable = false, unique = true)
    private String applicationName;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
