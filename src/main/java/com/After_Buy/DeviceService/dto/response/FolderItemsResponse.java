package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 특정 폴더 내부 아이템 목록 반환용 응답 DTO
 * GET /api/devices/folders/{folder_id}/items 의 응답 포맷
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
public class FolderItemsResponse {

    // 현재 진입한 폴더 자체의 정보
    private FolderDto currentFolder;

    // 루트부터 현재 위치까지의 경로(Breadcrumb) 배열
    private List<BreadcrumbDto> breadcrumb;

    // 직속 하위 폴더 목록
    private List<FolderDto> subFolders;

    // 해당 폴더 내 직속 기기 목록
    private List<DeviceListItemDto> devices;

    public static FolderItemsResponse of(FolderDto currentFolder, List<BreadcrumbDto> breadcrumb, List<FolderDto> subFolders, List<DeviceListItemDto> devices) {
        return FolderItemsResponse.builder()
                .currentFolder(currentFolder)
                .breadcrumb(breadcrumb)
                .subFolders(subFolders)
                .devices(devices)
                .build();
    }
}
