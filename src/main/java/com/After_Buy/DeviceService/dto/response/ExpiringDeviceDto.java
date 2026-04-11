package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 보증 만료 임박 기기 DTO
 * 내부 통신 시 전달되는 기기 정보 항목
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class ExpiringDeviceDto {

    @JsonProperty("device_id")
    private Long deviceId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("warranty_expiry_date")
    private String warrantyExpiryDate;
}
