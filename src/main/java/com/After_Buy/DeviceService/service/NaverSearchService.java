package com.After_Buy.DeviceService.service;

import com.After_Buy.DeviceService.dto.response.NaverProductDto;
import com.After_Buy.DeviceService.dto.response.NaverSearchResponse;
import com.After_Buy.DeviceService.exception.CustomException;
import com.After_Buy.DeviceService.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
public class NaverSearchService {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;

    public NaverSearchService(
            WebClient.Builder webClientBuilder,
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.client-secret}") String clientSecret) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.naver.com/v1/search").build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * 모델명을 기반으로 네이버 쇼핑 API를 호출하여 가장 정확도(sim)가 높은 첫 번째 제품 데이터를 반환합니다.
     * OCR에서 추출된 고유 모델명이 입력되는 것을 상정합니다.
     *
     * @param modelName 기기가 식별되는 고유 모델명
     * @return NaverProductDto 변환된 검색 결과
     * @throws CustomException 검색 결과가 없을 경우 에러 발생
     */
    public NaverProductDto searchProduct(String modelName) {
        log.info("[NaverSearchService] Searching for model: {}", modelName);

        NaverSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/shop.json")
                        .queryParam("query", modelName)
                        .queryParam("display", 1)  // 가장 유사도 높은 최상단 데이터 1개만 매핑
                        .queryParam("sort", "sim") // 유사도(정확도) 순 정렬 명시
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(NaverSearchResponse.class)
                .block(); // 백엔드 프록시이므로 블로킹으로 결과를 대기

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new CustomException(ErrorCode.SEARCH_NO_RESULT);
        }

        NaverSearchResponse.Item item = response.getItems().get(0);

        // 네이버 API 응답의 title 등에 포함된 <b>, </b> 태그 등을 이스케이프 해제 및 정제
        String cleanTitle = cleanHtmlTags(item.getTitle());

        // 검색된 제품의 이름(cleanTitle)에 사용자가 입력한 모델명이 포함되어 있는지 엄격하게 검증 (띄어쓰기, 하이픈 무시)
        if (!normalizeString(cleanTitle).contains(normalizeString(modelName))) {
            log.warn("[NaverSearchService] 검색 결과가 요청한 모델명과 일치하지 않습니다. 매핑 거부. (조회된 상품: {}, 요청 모델명: {})", cleanTitle, modelName);
            throw new CustomException(ErrorCode.SEARCH_NO_RESULT);
        }

        return NaverProductDto.builder()
                .productName(cleanTitle)
                .modelName(modelName)
                .brand(item.getBrand() != null && !item.getBrand().isEmpty() ? item.getBrand() : item.getMaker())
                .imageUrl(item.getImage())
                .productLinkUrl(item.getLink())
                .build();
    }

    /**
     * 공백, 하이픈, 언더바 등을 제거하고 모두 대문자로 변환하여 엄격한 문자열 비교를 위한 정규화를 수행합니다.
     */
    private String normalizeString(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\s\\-_]", "").toUpperCase();
    }

    /**
     * HTML 태그(<b> 등)를 제거하고 이스케이프된 문자를 복원합니다.
     */
    private String cleanHtmlTags(String input) {
        if (input == null) return null;
        String unescaped = HtmlUtils.htmlUnescape(input);
        return unescaped.replaceAll("<[^>]*>", "");
    }
}
