package com.After_Buy.DeviceService.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 기기 등록 요청 DTO
 * POST /api/devices 요청 바디를 매핑합니다.
 * API 명세서 기준 필수 항목(@NotBlank, @NotNull)과 선택 항목(nullable)을 구분합니다.
 *
 * 필수 항목 (빨간별 *): product_name, model_name, brand, purchase_date, purchase_price, warranty_months
 * 선택 항목             : folder_id, image_url, product_link_url, purchase_store, serial_number, memo
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
public class DeviceRegisterRequest {

	/* ===== 선택 항목 ===== */

	// 소속 폴더 ID (null = 미분류 상태로 저장, 미입력 시 최상위 레벨에 노출)
	@Schema(description = "소속 폴더 ID (미입력 시 미분류 상태로 저장)", nullable = true, example = "null")
	private Long folderId;

	/* ===== 필수 항목 ===== */

	// 상품명 (필수, 최대 200자)
	@NotBlank(message = "상품명은 필수 입력값입니다.")
	@Size(max = 200, message = "상품명은 200자 이내여야 합니다.")
	private String productName;

	// 모델명 (필수, 최대 100자)
	@NotBlank(message = "모델명은 필수 입력값입니다.")
	@Size(max = 100, message = "모델명은 100자 이내여야 합니다.")
	private String modelName;

	// 브랜드명 (필수, 최대 100자)
	@NotBlank(message = "브랜드명은 필수 입력값입니다.")
	@Size(max = 100, message = "브랜드명은 100자 이내여야 합니다.")
	private String brand;

	/* ===== 선택 항목 ===== */

	// 기기 이미지 URL (선택, S3 Pre-signed URL 업로드 후 전달, 없으면 placeholder 자동 삽입)
	@Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다.")
	private String imageUrl;

	// 공식 제품 페이지 URL (선택, 네이버 쇼핑 API 연동)
	@Size(max = 500, message = "제품 링크 URL은 500자 이내여야 합니다.")
	private String productLinkUrl;

	/* ===== 필수 항목 ===== */

	// 구매일 (필수, YYYY-MM-DD 형식, 영수증 OCR 또는 직접 입력)
	@NotNull(message = "구매일은 필수 입력값입니다.")
	private LocalDate purchaseDate;

	// 구매 가격 (필수, DECIMAL(12,2), 영수증 OCR 또는 직접 입력)
	@NotNull(message = "구매 가격은 필수 입력값입니다.")
	@DecimalMin(value = "0.0", inclusive = false, message = "구매 가격은 0보다 커야 합니다.")
	private BigDecimal purchasePrice;

	/* ===== 선택 항목 ===== */

	// 구매처 (선택, 영수증 OCR 또는 직접 입력, 최대 100자)
	@Size(max = 100, message = "구매처는 100자 이내여야 합니다.")
	private String purchaseStore;

	/* ===== 필수 항목 ===== */

	// 무상 보증 기간 개월 수 (필수, INT UNSIGNED)
	@NotNull(message = "무상 보증 기간은 필수 입력값입니다.")
	@Min(value = 1, message = "무상 보증 기간은 1개월 이상이어야 합니다.")
	private Integer warrantyMonths;

	/* ===== 선택 항목 ===== */

	// 시리얼 넘버 (선택, OCR 또는 직접 입력, 최대 100자)
	@Size(max = 100, message = "시리얼 넘버는 100자 이내여야 합니다.")
	private String serialNumber;

	// 사용자 메모 (선택)
	private String memo;
}
