package com.After_Buy.DeviceService.entity.enums;

/**
 * OCR 처리 유형 Enum
 * 어떤 정보를 추출할 목적으로 OCR을 호출했는지 구분합니다.
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
public enum OcrType {

    /** 기기 모델명 인식 */
    MODEL,

    /** 기기 시리얼 번호 인식 */
    SERIAL,

    /** 구매 영수증 정보 인식 */
    RECEIPT
}
