package com.After_Buy.DeviceService.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 다중 폴더 및 기기 일괄 이동 요청 DTO
 * 
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BulkMoveRequest {

    /** 이동할 폴더 ID 목록 */
    private List<Long> folderIds;

    /** 이동할 기기 ID 목록 */
    private List<Long> deviceIds;

    /** 이동 대상이 되는 새로운 부모 폴더 ID (NULL 이면 루트로 이동) */
    private Long targetFolderId;
}
