package com.After_Buy.DeviceService.dto.response;

import com.After_Buy.DeviceService.entity.Device;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 기기 응답 DTO
 * POST /api/devices 성공(201 Created) 응답 및 GET /api/devices/{device_id} 응답에 사용됩니다.
 * API 명세서의 201 Created 응답 구조와 동일합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceResponse {

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

	// 기기 이미지 URL (null 시 placeholder 이미지 적용됨)
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

	// 보증 만료일 (purchase_date + warranty_months 자동 계산)
	private final LocalDate warrantyExpiryDate;

	// 시리얼 넘버
	private final String serialNumber;

	// 사용자 메모
	private final String memo;

	// 등록 일시
	private final LocalDateTime createdAt;

	// 마지막 수정 일시
	private final LocalDateTime updatedAt;

	/**
	 * Device 엔티티로부터 응답 DTO를 생성하는 정적 팩토리 메소드
	 * Entity → DTO 변환을 일관되게 처리합니다.
	 *
	 * @param device : 변환할 Device 엔티티
	 * @return : 클라이언트에 반환할 DeviceResponse DTO
	 */
	public static DeviceResponse from(Device device) {
		return new DeviceResponse(device);
	}

	private DeviceResponse(Device device) {
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
		this.createdAt = device.getCreatedAt();
		this.updatedAt = device.getUpdatedAt();
	}
}
