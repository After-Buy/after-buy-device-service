package com.After_Buy.DeviceService.dto.response;

import com.After_Buy.DeviceService.entity.Folder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 폴더 생성 완료 응답 DTO
 * 생성 직후 생성된 폴더의 데이터 세부 사항(user_id 포함된 스펙 만족)을 반환합니다.
 *
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FolderCreateResponse {
    private Long folderId;
    private Long userId;
    private String folderName;
    private Long parentFolderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FolderCreateResponse from(Folder folder) {
        return FolderCreateResponse.builder()
                .folderId(folder.getFolderId())
                .userId(folder.getUserId())
                .folderName(folder.getFolderName())
                .parentFolderId(folder.getParentFolderId())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}
