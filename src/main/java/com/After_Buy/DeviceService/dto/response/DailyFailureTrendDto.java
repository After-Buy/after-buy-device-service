package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 통계용 일간 실패 트렌드 DTO
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class DailyFailureTrendDto {

    @JsonProperty("date")
    private String date;

    @JsonProperty("failure_count")
    private Long failureCount;
}
