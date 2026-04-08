package com.After_Buy.DeviceService.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기기 상품명 수정 요청 DTO
 * PATCH /api/devices/{device_id}/name 호출 시 사용됩니다.
 *
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceNameUpdateRequest {

	// 상품명 (필수, 최대 200자)
	@NotBlank(message = "상품명은 필수 입력값입니다.")
	@Size(max = 200, message = "상품명은 200자 이내여야 합니다.")
	private String productName;
}
