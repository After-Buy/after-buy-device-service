package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 통합 검색 결과 - 기기 항목 DTO
 * GET /api/devices/search 응답 내 devices 배열의 각 항목을 담는 DTO입니다.
 * 미분류 기기(folder_id = null)인 경우 folderName도 null로 반환합니다.
 *
 * @since : 2026.04.10
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchDeviceResultDto {

    /** 기기 고유 ID */
    private Long deviceId;

    /** 상품명 */
    private String productName;

    /** 브랜드명 */
    private String brand;

    /** 보증 만료일 */
    private LocalDate warrantyExpiryDate;

    /** 소속 폴더 ID (null = 미분류) */
    private Long folderId;

    /** 소속 폴더명 (null = 미분류) */
    private String folderName;
}
