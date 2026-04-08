package com.After_Buy.DeviceService.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 기기 정보 전체 수정 요청 DTO
 * PUT /api/devices/{device_id} 통신에 사용됩니다.
 * API 명세서에 따라 model_name을 제외한 나머지 정보를 모두 입력받아 업데이트합니다.
 *
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceUpdateRequest {

	// 소속 폴더 ID (null = 미분류 상태)
	@Schema(description = "소속 폴더 ID (미분류 시 null)", nullable = true, example = "null")
	private Long folderId;

	// 상품명 (필수, 최대 200자)
	@NotBlank(message = "상품명은 필수 입력값입니다.")
	@Size(max = 200, message = "상품명은 200자 이내여야 합니다.")
	private String productName;

	// 브랜드명 (필수, 최대 100자)
	@NotBlank(message = "브랜드명은 필수 입력값입니다.")
	@Size(max = 100, message = "브랜드명은 100자 이내여야 합니다.")
	private String brand;

	// 기기 이미지 URL (선택)
	@Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다.")
	private String imageUrl;

	// 공식 제품 페이지 URL (선택)
	@Size(max = 500, message = "제품 링크 URL은 500자 이내여야 합니다.")
	private String productLinkUrl;

	// 구매일 (필수, YYYY-MM-DD 형식)
	@NotNull(message = "구매일은 필수 입력값입니다.")
	private LocalDate purchaseDate;

	// 구매 가격 (필수)
	@NotNull(message = "구매 가격은 필수 입력값입니다.")
	@DecimalMin(value = "0.0", inclusive = false, message = "구매 가격은 0보다 커야 합니다.")
	private BigDecimal purchasePrice;

	// 구매처 (선택)
	@Size(max = 100, message = "구매처는 100자 이내여야 합니다.")
	private String purchaseStore;

	// 무상 보증 기간 개월 수 (필수)
	@NotNull(message = "무상 보증 기간은 필수 입력값입니다.")
	@Min(value = 1, message = "무상 보증 기간은 1개월 이상이어야 합니다.")
	private Integer warrantyMonths;

	// 시리얼 넘버 (선택)
	@Size(max = 100, message = "시리얼 넘버는 100자 이내여야 합니다.")
	private String serialNumber;

	// 사용자 메모 (선택)
	private String memo;
}
