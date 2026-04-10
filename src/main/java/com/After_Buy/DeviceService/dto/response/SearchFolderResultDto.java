package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 통합 검색 결과 - 폴더 항목 DTO
 * GET /api/devices/search 응답 내 folders 배열의 각 항목을 담는 DTO입니다.
 *
 * @since : 2026.04.10
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchFolderResultDto {

    /** 폴더 고유 ID */
    private Long folderId;

    /** 폴더명 */
    private String folderName;

    /** 검색 매칭된 필드명 (folder_name 고정) */
    private String matchField;
}
