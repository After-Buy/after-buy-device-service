package com.After_Buy.DeviceService.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 탈퇴로 인한 기기 관련 데이터 전체 삭제 응답 DTO
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class InternalDeleteUserDataResponse {

    private boolean deleted;
    
    private Long user_id;
}
