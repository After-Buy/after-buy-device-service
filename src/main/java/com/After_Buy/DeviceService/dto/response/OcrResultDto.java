package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * OCR 처리 결과 DTO
 * OCR 유형에 따라 반환되는 필드가 다르며, null 필드는 JSON 직렬화에서 제외됩니다.
 * - MODEL  : model_name
 * - SERIAL : serial_number
 * - RECEIPT: purchase_date, purchase_price, purchase_store
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcrResultDto {

    // ===== MODEL 타입 =====

    // 인식된 기기 모델명
    @JsonProperty("model_name")
    private String modelName;

    // ===== SERIAL 타입 =====

    // 인식된 시리얼 번호
    @JsonProperty("serial_number")
    private String serialNumber;

    // ===== RECEIPT 타입 =====

    // 구매 날짜 (형식: yyyy-MM-dd)
    @JsonProperty("purchase_date")
    private String purchaseDate;

    // 구매 가격
    @JsonProperty("purchase_price")
    private BigDecimal purchasePrice;

    // 구매처 (판매점 이름)
    @JsonProperty("purchase_store")
    private String purchaseStore;
}
