package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 통계용 일간 OCR 결과 트렌드 DTO
 *
 * @since : 2026.05.23
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class DailyOcrResultTrendDto {

	@JsonProperty("date")
	private String date;

	@JsonProperty("total_attempts")
	private Long totalAttempts;

	@JsonProperty("success_count")
	private Long successCount;

	@JsonProperty("modified_count")
	private Long modifiedCount;

	@JsonProperty("failure_count")
	private Long failureCount;
}
