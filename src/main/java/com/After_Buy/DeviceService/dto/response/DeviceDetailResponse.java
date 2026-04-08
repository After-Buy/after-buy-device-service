package com.After_Buy.DeviceService.dto.response;

import com.After_Buy.DeviceService.entity.Device;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 기기 상세 조회 응답 DTO
 * GET /api/devices/{device_id} 성공(200 OK) 응답에 사용됩니다.
 * days_remaining은 API 명세서에 따라 서버에서 warranty_expiry_date - 현재 날짜로 실시간 계산됩니다.
 * POST(기기 등록) 응답인 DeviceResponse와 달리 days_remaining 필드를 포함합니다.
 *
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceDetailResponse {

	// 기기 고유 ID
	private final Long deviceId;

	// 소유 사용자 ID
	private final Long userId;

	// 소속 폴더 ID (null = 미분류)
	private final Long folderId;

	// 상품명
	private final String productName;

	// 모델명
	private final String modelName;

	// 브랜드명
	private final String brand;

	// 기기 이미지 URL
	private final String imageUrl;

	// 공식 제품 페이지 URL
	private final String productLinkUrl;

	// 구매일
	private final LocalDate purchaseDate;

	// 구매 가격
	private final BigDecimal purchasePrice;

	// 구매처
	private final String purchaseStore;

	// 무상 보증 기간 개월 수
	private final Integer warrantyMonths;

	// 보증 만료일
	private final LocalDate warrantyExpiryDate;

	// 시리얼 넘버
	private final String serialNumber;

	// 사용자 메모
	private final String memo;

	/* 보증 잔여 일수 (서버 실시간 계산: warranty_expiry_date - 오늘, 음수 = 이미 만료됨) */
	private final Long daysRemaining;

	// 등록 일시
	private final LocalDateTime createdAt;

	// 마지막 수정 일시
	private final LocalDateTime updatedAt;

	/**
	 * Device 엔티티로부터 상세 응답 DTO를 생성하는 정적 팩토리 메소드
	 *
	 * @param device : 변환할 Device 엔티티
	 * @return : 클라이언트에 반환할 DeviceDetailResponse DTO
	 * @since : 2026.04.08
	 * @version : 1.0.0
	 * @author : 최준혁
	 */
	public static DeviceDetailResponse from(Device device) {
		return new DeviceDetailResponse(device);
	}

	private DeviceDetailResponse(Device device) {
		this.deviceId = device.getDeviceId();
		this.userId = device.getUserId();
		this.folderId = device.getFolderId();
		this.productName = device.getProductName();
		this.modelName = device.getModelName();
		this.brand = device.getBrand();
		this.imageUrl = device.getImageUrl();
		this.productLinkUrl = device.getProductLinkUrl();
		this.purchaseDate = device.getPurchaseDate();
		this.purchasePrice = device.getPurchasePrice();
		this.purchaseStore = device.getPurchaseStore();
		this.warrantyMonths = device.getWarrantyMonths();
		this.warrantyExpiryDate = device.getWarrantyExpiryDate();
		this.serialNumber = device.getSerialNumber();
		this.memo = device.getMemo();
		/* days_remaining: warranty_expiry_date - 현재 날짜 (API 명세서: 서버에서 실시간 계산) */
		this.daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), device.getWarrantyExpiryDate());
		this.createdAt = device.getCreatedAt();
		this.updatedAt = device.getUpdatedAt();
	}
}
