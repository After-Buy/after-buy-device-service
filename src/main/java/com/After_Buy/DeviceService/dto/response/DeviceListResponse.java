package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

import java.util.List;

/**
 * 기기 목록 조회 응답 래퍼 DTO
 * GET /api/devices의 응답 데이터 최상위 구조입니다.
 * API 명세: { "success": true, "data": { "devices": [...] } }
 *
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceListResponse {

	// 기기 목록 (folder_id = NULL인 미분류 기기만 포함)
	private final List<DeviceListItemDto> devices;

	/**
	 * 기기 목록 응답 생성 정적 팩토리 메소드
	 *
	 * @param devices : 기기 목록 아이템 DTO 리스트
	 * @return : DeviceListResponse 객체
	 * @since : 2026.04.08
	 * @version : 1.0.0
	 * @author : 최준혁
	 */
	public static DeviceListResponse of(List<DeviceListItemDto> devices) {
		return new DeviceListResponse(devices);
	}

	private DeviceListResponse(List<DeviceListItemDto> devices) {
		this.devices = devices;
	}
}
