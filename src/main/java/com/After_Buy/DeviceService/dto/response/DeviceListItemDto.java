package com.After_Buy.DeviceService.dto.response;

import com.After_Buy.DeviceService.entity.Device;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 기기 목록 아이템 응답 DTO
 * GET /api/devices 응답에 사용되는 기기 목록 아이템 DTO입니다.
 * days_remaining은 서버에서 warranty_expiry_date - 현재 날짜로 실시간 계산됩니다.
 *
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceListItemDto {

	// 기기 고유 ID
	private final Long deviceId;

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

	// 보증 만료일
	private final LocalDate warrantyExpiryDate;

	// 보증 잔여 일수 (서버 실시간 계산: warranty_expiry_date - 오늘)
	private final Long daysRemaining;

	// 등록 일시
	private final LocalDateTime createdAt;

	/**
	 * Device 엔티티로부터 목록 아이템 DTO를 생성하는 정적 팩토리 메소드
	 *
	 * @param device : 변환할 Device 엔티티
	 * @return : DeviceListItemDto 객체
	 * @since : 2026.04.08
	 * @version : 1.0.0
	 * @author : 최준혁
	 */
	public static DeviceListItemDto from(Device device) {
		return new DeviceListItemDto(device);
	}

	private DeviceListItemDto(Device device) {
		this.deviceId = device.getDeviceId();
		this.folderId = device.getFolderId();
		this.productName = device.getProductName();
		this.modelName = device.getModelName();
		this.brand = device.getBrand();
		this.imageUrl = device.getImageUrl();
		this.warrantyExpiryDate = device.getWarrantyExpiryDate();
		/* days_remaining: warranty_expiry_date - 현재 날짜 (음수 = 이미 만료됨) */
		this.daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), device.getWarrantyExpiryDate());
		this.createdAt = device.getCreatedAt();
	}
}
