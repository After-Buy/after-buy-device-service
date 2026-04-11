package com.After_Buy.DeviceService.dto.request;

import com.After_Buy.DeviceService.entity.enums.OcrType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OCR 텍스트 추출 요청 DTO
 * Base64 인코딩된 이미지와 OCR 유형을 전달합니다.
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OcrRequest {

    // OCR 유형 (MODEL, SERIAL, RECEIPT 중 하나)
    @NotNull(message = "ocr_type은 필수 항목입니다.")
    @JsonProperty("ocr_type")
    private OcrType ocrType;

    // Base64 인코딩된 이미지 데이터
    @NotBlank(message = "image_base64는 필수 항목입니다.")
    @JsonProperty("image_base64")
    private String imageBase64;
}
