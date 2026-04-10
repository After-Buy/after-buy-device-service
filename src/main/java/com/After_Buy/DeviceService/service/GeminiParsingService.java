package com.After_Buy.DeviceService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gemini API 연동을 통한 네이버 쇼핑 결과 정제 서비스
 * 네이버 쇼핑 API 원본 응답(제품명, 브랜드)을 Gemini LLM에게 전달하여
 * 불필요한 모델 코드 제거, 브랜드 정규화 등을 수행합니다.
 * Gemini 호출 실패 시 원본 값을 그대로 반환하여 서비스 장애를 방지합니다.
 *
 * @since : 2026.04.11
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Service
public class GeminiParsingService {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    /* Gemini 2.5 Flash Lite 모델 REST API 엔드포인트
     * - 공식 문서 기준 현재 사용 가능한 가장 빠르고 저렴한 안정(Stable) 모델
     * - 단순 분류/정제 태스크에 최적, 처리량이 높아 503 발생 가능성이 낮음 */
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GEMINI_PATH = "/v1beta/models/gemini-2.5-flash-lite:generateContent";

    public GeminiParsingService(
            WebClient.Builder webClientBuilder,
            @Value("${gemini.api-key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(GEMINI_BASE_URL).build();
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 네이버 쇼핑 검색 결과 제품명·브랜드 정제 메서드
     * Gemini API를 통해 제품명과 브랜드를 정제하고 반환합니다.
     * Gemini 장애/파싱 실패 시 원본 값을 그대로 반환합니다.
     * 모델명(modelName)은 절대 수정하지 않으며, 이 메서드에서는 받지도 않습니다.
     *
     * @param rawProductName : 네이버 쇼핑 원본 제품명 (HTML 태그 제거 후)
     * @param rawBrand       : 네이버 쇼핑 원본 브랜드명
     * @return : 정제된 제품명과 브랜드를 담은 배열 [refinedProductName, refinedBrand]
     * @since : 2026.04.11
     * @author : 최준혁
     */
    public String[] refine(String rawProductName, String rawBrand) {
        try {
            String userPrompt = buildUserPrompt(rawProductName, rawBrand);
            String requestBody = buildRequestBody(userPrompt);

            String responseBody = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(GEMINI_PATH)
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    /* 4xx/5xx 에러 발생 시 응답 본문을 로그로 남겨 진짜 원인 파악 */
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .doOnNext(errorBody -> log.error(
                                            "[GeminiParsingService] Gemini API 오류 응답 (HTTP {}): {}",
                                            clientResponse.statusCode().value(), errorBody))
                                    .flatMap(errorBody -> reactor.core.publisher.Mono.error(
                                            new RuntimeException("Gemini HTTP 오류 " + clientResponse.statusCode().value()
                                                    + ": " + errorBody))))
                    .bodyToMono(String.class)
                    /*
                     * 503(Service Unavailable) 발생 시 최대 2회 재시도 (Exponential Backoff)
                     * - 1차 재시도: 1초 후
                     * - 2차 재시도: 2초 후
                     * - 429(Rate Limit)나 4xx는 재시도하지 않음 (의미 없음)
                     */
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable
                                    || (throwable instanceof RuntimeException
                                            && throwable.getMessage() != null
                                            && throwable.getMessage().contains("503")))
                            .doBeforeRetry(signal -> log.warn(
                                    "[GeminiParsingService] Gemini 503 재시도 중... ({}/2회)",
                                    signal.totalRetries() + 1)))
                    .block();

            return parseGeminiResponse(responseBody, rawProductName, rawBrand);

        } catch (Exception e) {
            /* Gemini 호출 실패 시 서비스 중단 없이 원본 값 그대로 반환 */
            log.warn("[GeminiParsingService] Gemini 정제 호출 실패, 원본 값 사용. 사유: {}", e.getMessage());
            return new String[] { rawProductName, rawBrand };
        }
    }

    /**
     * 시스템 인스트럭션 생성 메서드 (Gemini 역할/규칙 모듈)
     * 프롬프트에서 분리하여 토큰 절감. 각 호출마다 반복 전송하지 않고 요청 구조에서 도구 역할을 담습니다.
     * 전자기기 판별 기준과 정제 규칙을 포함합니다.
     *
     * @return : Gemini system_instruction 텍스트
     * @since : 2026.04.11
     * @author : 최준혁
     */
    private String buildSystemInstruction() {
        return "You are a Korean e-commerce product classifier. " +
               "Given a product name and brand from Naver Shopping:\n" +
               "1. Determine if it is a consumer ELECTRONICS DEVICE " +
               "(smartphone, laptop, tablet, earphones, headphones, smartwatch, TV, monitor, camera, game console). " +
               "NOT a case, film, charger, cable, stand, bag, or accessory.\n" +
               "2. If NOT a device: output {\"product_name\":null,\"brand\":null}\n" +
               "3. If a device: remove model codes (e.g. SM-S928N, MKGP3KH/A), " +
               "remove storage capacity (256GB, 512GB, 1TB etc), remove sales words (정품/자급제/공식), " +
               "remove brand prefix duplication, normalize brand (삼성전자→삼성, LG전자→LG).";
    }

    /**
     * 유저 프롬프트 생성 메서드 (모듈식 - 데이터만 전달)
     * 시스템 인스트럭션에서 규칙을 정의하고, 이 프롬프트에는 비교 데이터만 담습니다.
     * 토큰 사용량을 최소화합니다.
     *
     * @param rawProductName : 원본 제품명
     * @param rawBrand       : 원본 브랜드명
     * @return : 데이터만 담은 최소 프롬프트
     * @since : 2026.04.11
     * @author : 최준혁
     */
    private String buildUserPrompt(String rawProductName, String rawBrand) {
        return "Product: " + rawProductName + "\nBrand: " + rawBrand;
    }

    /**
     * Gemini REST API 요청 본문(JSON) 생성 메서드 (모듈식 구조 적용)
     * system_instruction에 역할/규칙을, contents에 데이터만 담아 토큰을 절감합니다.
     * generationConfig.response_mime_type으로 JSON 응답을 강제하여
     * 프롬프트에 'JSON으로 답해줘' 문구를 넣을 필요가 없습니다.
     *
     * @param userPrompt : 데이터만 담은 유저 프롬프트
     * @return : Gemini API 요청 JSON 문자열
     * @since : 2026.04.11
     * @author : 최준혁
     */
    private String buildRequestBody(String userPrompt) {
        try {
            Map<String, Object> requestMap = Map.of(
                    /* 역할/규칙 모듈: 시스템 인스트럭션으로 분리 */
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", buildSystemInstruction()))),
                    /* 데이터 모듈: 유저 프롬프트는 데이터만 */
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", userPrompt)))),
                    /* JSON 응답 강제: 프롬프트에 'JSON으로 답해줘' 구문 보담 안해도 됨 */
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json")
            );
            return objectMapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            throw new RuntimeException("Gemini 요청 본문 생성 실패", e);
        }
    }

    /**
     * Gemini 응답 파싱 메서드
     * candidates[0].content.parts[0].text 에서 JSON을 추출하여 정제된 값을 반환합니다.
     * Gemini가 전자기기가 아님으로 판별(케이스 등)시 null을 반환합니다.
     * 파싱 실패 시 원본 값을 반환합니다.
     *
     * @param responseBody   : Gemini API 원본 응답 JSON 문자열
     * @param rawProductName : 파싱 실패 시 fallback으로 반환할 원본 제품명
     * @param rawBrand       : 파싱 실패 시 fallback으로 반환할 원본 브랜드명
     * @return : [정제된 제품명, 정제된 브랜드명] 또는 null (전자기기 아님으로 판단시)
     * @since : 2026.04.11
     * @author : 최준혁
     */
    private String[] parseGeminiResponse(String responseBody, String rawProductName, String rawBrand) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            /* Gemini 응답 구조: candidates[0].content.parts[0].text */
            String text = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            /* Gemini가 마크다운 코드블록을 포함할 수 있으므로 순수 JSON 부분만 추출 */
            String cleanedText = text.trim();
            if (cleanedText.startsWith("```")) {
                cleanedText = cleanedText.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
            }

            JsonNode parsedResult = objectMapper.readTree(cleanedText);

            /* Gemini가 전자기기가 아님으로 판단한 경우: product_name이 null 또는 비어있음 */
            JsonNode productNameNode = parsedResult.path("product_name");
            if (productNameNode.isNull() || productNameNode.isMissingNode()) {
                log.warn("[GeminiParsingService] 전자기기가 아님으로 판단 - 검색 실패 처리. 원본 제품명: '{}'", rawProductName);
                return null;
            }

            String refinedProductName = productNameNode.asText(rawProductName);
            String refinedBrand = parsedResult.path("brand").asText(rawBrand);

            log.info("[GeminiParsingService] 정제 완료 - 제품명: '{}' → '{}', 브랜드: '{}' → '{}'",
                    rawProductName, refinedProductName, rawBrand, refinedBrand);

            return new String[] { refinedProductName, refinedBrand };

        } catch (Exception e) {
            log.warn("[GeminiParsingService] Gemini 응답 파싱 실패, 원본 값 사용. 사유: {}", e.getMessage());
            return new String[] { rawProductName, rawBrand };
        }
    }
}
