package com.After_Buy.DeviceService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 홈 화면 요약 통계 DTO
 * 
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryDto {
    // 사용자가 소유한 총 기기 수
    private Integer total_devices;
    // 사용자가 소유한 총 기기의 가치 (구매 가격 총합)
    private BigDecimal total_value;
    // 본래 오늘 기점으로 30일 이내에 보증이 만료되는 기기 수
    private Integer expiring_soon_count;
}
