package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 루트 폴더 목록 조회 응답 DTO
 * GET /api/devices/folders API의 데이터 응답 객체입니다.
 *
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RootFolderListResponse {

    // 최상위(루트) 폴더 목록
    private List<FolderDto> folders;

    // 미분류(폴더에 속하지 않은) 기기 목록
    private List<DeviceListItemDto> unclassifiedDevices;

    /**
     * 정적 팩토리 메소드
     *
     * @param folders             : 루트 폴더 목록
     * @param unclassifiedDevices : 미분류 기기 목록
     * @return : 생성된 응답 DTO 객체
     */
    public static RootFolderListResponse of(List<FolderDto> folders, List<DeviceListItemDto> unclassifiedDevices) {
        return RootFolderListResponse.builder()
                .folders(folders)
                .unclassifiedDevices(unclassifiedDevices)
                .build();
    }
}
