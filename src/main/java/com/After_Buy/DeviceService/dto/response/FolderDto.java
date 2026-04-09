package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 폴더 응답 DTO
 * GET /api/devices/folders 등에서 폴더 정보와 하위 항목(자식) 개수를 반환할 때 사용됩니다.
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
public class FolderDto {

    // 폴더 고유 ID
    private Long folderId;

    // 폴더명
    private String folderName;

    // 상위 폴더 ID (null = 루트)
    private Long parentFolderId;

    // 하위 항목 수 (하위 폴더 개수 + 직속 기기 개수)
    private Long childCount;

    // 생성 일시
    private LocalDateTime createdAt;

    // 마지막 수정 일시
    private LocalDateTime updatedAt;
}
