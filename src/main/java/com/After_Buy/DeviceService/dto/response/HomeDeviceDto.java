package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 홈 화면용 기기 요약 DTO
 * 최근 등록된 기기와 보증 만료가 가장 임박한 기기 정보를 담습니다.
 * null 값인 필드는 JSON 응답에서 제외됩니다.
 *
 * @since : 2026.04.08
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeDeviceDto {
    private Long device_id;
    private String product_name;
    private String model_name;
    private String brand;
    private String image_url;
    private String product_link_url;
    private LocalDate warranty_expiry_date;
    private Long days_remaining;
    private LocalDateTime created_at;
}
