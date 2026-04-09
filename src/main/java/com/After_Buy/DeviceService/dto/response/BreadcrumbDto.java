package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Breadcrumb 경로 DTO
 * 계층형 폴더 구조에서 파일 또는 폴더의 위치를 알려주기 위해 사용됩니다.
 *
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BreadcrumbDto {
    private Long folderId;
    private String folderName;
}
