package com.emenu.features.appkey.dto.request;

import lombok.Data;

@Data
public class AppKeyUpdateRequest {

    private String description;

    private Boolean isActive;
}
