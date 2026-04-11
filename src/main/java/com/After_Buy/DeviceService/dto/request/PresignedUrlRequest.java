package com.After_Buy.DeviceService.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기기 이미지 업로드용 Presigned URL 발급 요청 DTO
 * 
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PresignedUrlRequest {

    // 예: "jpg", "png", "jpeg", "webp"
    @NotBlank(message = "파일 확장자는 필수 항목입니다.")
    @JsonProperty("file_extension")
    private String fileExtension;
}
