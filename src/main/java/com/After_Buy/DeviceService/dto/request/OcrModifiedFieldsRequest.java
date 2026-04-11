package com.After_Buy.DeviceService.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OCR 오인식 필드 기록 요청 DTO
 * 사용자가 OCR 결과를 수정하여 기기를 저장한 후,
 * 어떤 필드를 수정했는지 기록하기 위한 요청입니다.
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OcrModifiedFieldsRequest {

    // 이전 OCR 호출에서 발급된 로그 ID
    @NotNull(message = "ocr_log_id는 필수 항목입니다.")
    @JsonProperty("ocr_log_id")
    private Long ocrLogId;

    // 결과를 수정하여 등록/수정된 기기 ID
    @NotNull(message = "device_id는 필수 항목입니다.")
    @JsonProperty("device_id")
    private Long deviceId;

    // 사용자가 직접 수정한 필드명 목록 (예: ["purchase_date", "purchase_price"])
    @NotEmpty(message = "modified_fields는 하나 이상의 항목이 필요합니다.")
    @JsonProperty("modified_fields")
    private List<String> modifiedFields;
}
