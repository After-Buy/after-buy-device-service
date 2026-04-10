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

/**
 * 네이버 쇼핑 검색 서비스
 * 제품의 모델명을 통해 네이버 쇼핑 API와 통신하여 정보를 추출하고 정제하는 서비스 클래스
 *
 * @since : 2026.04.07
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Service
public class NaverSearchService {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final GeminiParsingService geminiParsingService;

    public NaverSearchService(
            WebClient.Builder webClientBuilder,
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.client-secret}") String clientSecret,
            GeminiParsingService geminiParsingService) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.naver.com/v1/search").build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.geminiParsingService = geminiParsingService;
    }

    /**
     * 제품 모델명 검색 메소드
     * 모델명을 기반으로 네이버 쇼핑 API를 호출하여 가장 정확도(sim)가 높은 첫 번째 제품 데이터를 반환합니다.
     * OCR에서 추출된 고유 모델명이 입력되는 것을 상정합니다.
     *
     * @param modelName : 기기가 식별되는 고유 모델명
     * @return : NaverProductDto (변환 및 정제된 검색 결과)
     * @since : 2026.04.07
     * @version : 1.0.0
     * @throws : CustomException (검색 결과가 없거나 모델명이 일치하지 않을 경우 SEARCH_NO_RESULT 에러 발생)
     * @author : 최준혁
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

        // 브랜드 원본 추출 (brand 없으면 maker 사용)
        String rawBrand = item.getBrand() != null && !item.getBrand().isEmpty()
                ? item.getBrand()
                : item.getMaker();

        /*
         * Gemini LLM으로 제품명·브랜드 정제 수행
         * - 모델 코드 제거, 판매 수식어 제거, 브랜드 정규화 등
         * - 저장 용량(256GB 등)은 유지
         * - modelName은 이 단계에서 절대 수정하지 않음
         */
        String[] refined = geminiParsingService.refine(cleanTitle, rawBrand);
        String refinedProductName = refined[0];
        String refinedBrand = refined[1];

        return NaverProductDto.builder()
                .productName(refinedProductName)
                .modelName(modelName)
                .brand(refinedBrand)
                .imageUrl(item.getImage())
                .productLinkUrl(item.getLink())
                .build();
    }

    /**
     * 모델명 데이터 정규화 처리 메소드
     * 공백, 하이픈, 언더바 등을 제거하고 모두 대문자로 변환하여 엄격한 문자열 비교를 위한 정규화를 수행합니다.
     *
     * @param input : 정규화할 원본 문자열
     * @return : 공백과 특수기호가 제거된 대문자 문자열
     * @since : 2026.04.07
     * @version : 1.0.0
     * @author : 최준혁
     */
    private String normalizeString(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\s\\-_]", "").toUpperCase();
    }

    /**
     * HTML 태그 제거 및 이스케이프 복원 메소드
     * HTML <b> 등의 태그를 제거하고 이스케이프된 문자를 일반 문자열로 복원합니다.
     *
     * @param input : HTML 태그가 포함된 원본 문자열
     * @return : HTML 태그가 제거된 순수 문자열
     * @since : 2026.04.07
     * @version : 1.0.0
     * @author : 최준혁
     */
    private String cleanHtmlTags(String input) {
        if (input == null) return null;
        String unescaped = HtmlUtils.htmlUnescape(input);
        return unescaped.replaceAll("<[^>]*>", "");
    }
}
