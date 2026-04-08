package com.After_Buy.DeviceService.dto.response;

import com.After_Buy.DeviceService.entity.Device;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 기기 상품명 수정 응답 DTO
 * PATCH /api/devices/{device_id}/name 성공 데이터
 *
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceNameUpdateResponse {

	private final Long deviceId;
	private final String productName;
	private final LocalDateTime updatedAt;

	public static DeviceNameUpdateResponse from(Device device) {
		return new DeviceNameUpdateResponse(device);
	}

	private DeviceNameUpdateResponse(Device device) {
		this.deviceId = device.getDeviceId();
		this.productName = device.getProductName();
		this.updatedAt = device.getUpdatedAt();
	}
}
