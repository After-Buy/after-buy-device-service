package com.After_Buy.DeviceService.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 폴더명 수정 요청 DTO
 * PATCH /api/devices/folders/{folder_id} API 전송 시 사용됩니다.
 *
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FolderUpdateNameRequest {

    // 변경할 새 폴더의 이름 (필수)
    @NotBlank(message = "폴더명은 필수 입력값입니다.")
    private String folderName;
}
