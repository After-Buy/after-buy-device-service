package com.After_Buy.DeviceService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.util.List;

/**
 * 다중 폴더 및 기기 일괄 삭제 요청 DTO
 * 
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BulkDeleteRequest {

    /** 삭제할 폴더 ID 목록 */
    private List<Long> folderIds;

    /** 삭제할 기기 ID 목록 */
    private List<Long> deviceIds;
}
