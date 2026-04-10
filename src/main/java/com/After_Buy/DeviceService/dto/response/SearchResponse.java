package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 통합 검색 결과 응답 DTO
 * GET /api/devices/search 응답 data 필드를 담는 DTO입니다.
 * 검색 결과가 없을 경우 각 리스트는 빈 배열([])로 반환합니다.
 *
 * @since : 2026.04.10
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchResponse {

    /** 폴더 검색 결과 목록 (없을 경우 빈 배열) */
    private List<SearchFolderResultDto> folders;

    /** 기기 검색 결과 목록 (없을 경우 빈 배열) */
    private List<SearchDeviceResultDto> devices;
}
