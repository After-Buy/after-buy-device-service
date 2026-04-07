package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 네이버 쇼핑 검색 결과 바인딩용 DTO
 * 네이버 쇼핑 Open API의 JSON 응답 데이터를 매핑하기 위해 사용되는 내부 DTO 입니다.
 *
 * @since : 2026.04.07
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverSearchResponse {
    private List<Item> items;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String title;
        private String link;
        private String image;
        private String mallName;
        private String maker;
        private String brand;
    }
}
