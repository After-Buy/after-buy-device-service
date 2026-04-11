package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 통계용 필드 수정 현황 DTO
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class FieldModifiedStatDto {

    @JsonProperty("field_name")
    private String fieldName;

    @JsonProperty("modified_count")
    private Long modifiedCount;
}
