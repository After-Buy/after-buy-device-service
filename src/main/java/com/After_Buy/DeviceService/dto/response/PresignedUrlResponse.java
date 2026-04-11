package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 기기 이미지 업로드용 Presigned URL 발급 응답 DTO
 * 
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
@AllArgsConstructor
public class PresignedUrlResponse {

    @JsonProperty("presigned_url")
    private String presignedUrl;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("expires_in")
    private long expiresIn;
}
