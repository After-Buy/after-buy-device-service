package com.After_Buy.DeviceService.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 네이버 쇼핑 제품 정보 전송 DTO
 * 내부적으로 정제시킨 네이버 쇼핑 검색 결과를 클라이언트(프론트엔드)로 반환할 때 사용하는 DTO 입니다.
 *
 * @since : 2026.04.07
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
public class NaverProductDto {
    private String productName;
    private String modelName;
    private String brand;
    private String imageUrl;
    private String productLinkUrl;
}
