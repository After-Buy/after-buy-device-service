package com.After_Buy.DeviceService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 홈 화면 요약 응답 DTO
 * 
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeSummaryResponse {
    private SummaryDto summary;
    private List<HomeDeviceDto> recent_devices;
    private HomeDeviceDto urgent_device;
}
