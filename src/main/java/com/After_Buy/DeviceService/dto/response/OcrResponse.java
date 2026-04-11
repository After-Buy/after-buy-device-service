package com.After_Buy.DeviceService.dto.response;

import com.After_Buy.DeviceService.entity.enums.OcrType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * OCR 처리 응답 DTO
 * OCR 인식 성공/실패 여부와 유형에 따른 결과를 반환합니다.
 * - is_success: false인 경우 result는 null이고 message 필드 포함.
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcrResponse {

    // OCR 처리 유형 (MODEL, SERIAL, RECEIPT)
    @JsonProperty("ocr_type")
    private OcrType ocrType;

    // 발급된 OCR 로그 ID (클라이언트가 modified-fields 호출 시 사용)
    @JsonProperty("ocr_log_id")
    private Long ocrLogId;

    // OCR 인식 성공 여부
    @JsonProperty("is_success")
    private Boolean isSuccess;

    // OCR 인식 결과 (실패 시 null)
    @JsonProperty("result")
    private OcrResultDto result;

    // 실패 메시지 (성공 시 null)
    @JsonProperty("message")
    private String message;
}
