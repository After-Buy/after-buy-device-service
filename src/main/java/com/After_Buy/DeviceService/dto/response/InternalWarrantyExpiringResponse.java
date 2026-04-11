package com.After_Buy.DeviceService.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 보증 만료 임박 목록 응답 DTO
 * 내부 통신을 통해 반환되는 루트 객체
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class InternalWarrantyExpiringResponse {

    private List<ExpiringDeviceDto> devices;
}
