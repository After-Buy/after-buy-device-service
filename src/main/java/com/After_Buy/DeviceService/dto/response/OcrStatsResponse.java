package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * OCR 통계 응답 DTO
 * Admin Service 통계 화면 표출용
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class OcrStatsResponse {

    @JsonProperty("total_attempts")
    private Long totalAttempts;

    @JsonProperty("failure_count")
    private Long failureCount;

    @JsonProperty("modified_count")
    private Long modifiedCount;

    @JsonProperty("field_modified_stats")
    private List<FieldModifiedStatDto> fieldModifiedStats;

    @JsonProperty("daily_failure_trend")
    private List<DailyFailureTrendDto> dailyFailureTrend;
}
