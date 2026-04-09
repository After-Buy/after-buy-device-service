package com.After_Buy.DeviceService.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 폴더 생성 요청 DTO
 * POST /api/devices/folders API 전송 시 사용됩니다.
 *
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FolderCreateRequest {

    // 새 폴더의 이름 (필수)
    @NotBlank(message = "폴더명은 필수 입력값입니다.")
    private String folderName;

    // 상위 폴더 경로 ID (루트 폴더일 경우 null)
    private Long parentFolderId;
}
