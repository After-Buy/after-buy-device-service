package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 통계용 항목별 OCR 실패 현황 DTO
 *
 * @since : 2026.05.23
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class FieldFailureStatDto {

	@JsonProperty("field_name")
	private String fieldName;

	@JsonProperty("failure_count")
	private Long failureCount;
}
