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

    /* Google Cloud Vertex AI Gemini 엔드포인트 */
    private static final String VERTEX_GEMINI_BASE_URL = "https://aiplatform.googleapis.com";
    private static final String VERTEX_GEMINI_PATH = "/v1/publishers/google/models/gemini-2.5-flash-lite:generateContent";

    public GeminiParsingService(
            WebClient.Builder webClientBuilder,
            @Value("${vertex.api-key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(VERTEX_GEMINI_BASE_URL).build();
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
                            .path(VERTEX_GEMINI_PATH)
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
     * 모델명 매핑 일치 여부 판단 메서드
     * 사용자가 입력한 모델명과 네이버 쇼핑 검색 결과 제목을 Gemini에게 전달하여 같은 제품인지 여부를 판단합니다.
     *
     * @param inputModel : 사용자가 입력한 모델명 (예: 16Z90TP-KD7WK)
     * @param naverTitle : 네이버 쇼핑 검색 결과 제품 제목 (HTML 태그 제거 후)
     * @return : 동일 제품으로 판단되면 true, 아니면 false
     * @since : 2026.05.11
     * @version : 0.0.1
     * @author : 최준혁
     */
    public boolean matchModelName(String inputModel, String naverTitle) {
        try {
            String userPrompt = buildModelMatchUserPrompt(inputModel, naverTitle);
            String requestBody = buildModelMatchRequestBody(userPrompt);

            String responseBody = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(VERTEX_GEMINI_PATH)
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .doOnNext(errorBody -> log.error(
                                            "[GeminiParsingService] 모델명 매핑 Gemini API 오류 (HTTP {}): {}",
                                            clientResponse.statusCode().value(), errorBody))
                                    .flatMap(errorBody -> reactor.core.publisher.Mono.error(
                                            new RuntimeException("Gemini HTTP 오류 " + clientResponse.statusCode().value()
                                                    + ": " + errorBody))))
                    .bodyToMono(String.class)
                    .block();

            return parseModelMatchResponse(responseBody, inputModel, naverTitle);

        } catch (Exception e) {
            /* Gemini 호출 실패 시 기존 normalizeString contains 로직으로 폴백 */
            log.warn("[GeminiParsingService] 모델명 매핑 Gemini 호출 실패, 폴백 로직 사용. 사유: {}", e.getMessage());
            String normalizedTitle = naverTitle.replaceAll("[\\s\\-_]", "").toUpperCase();
            String normalizedModel = inputModel.replaceAll("[\\s\\-_]", "").toUpperCase();
            return normalizedTitle.contains(normalizedModel);
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
     * 모델명 매핑 전용 시스템 인스트럭션 생성 메서드
     * 가전 모델명 도메인 지식(접미사 규칙, 체급/사양 구분 기준)을 포함한 고도화 프롬프트입니다.
     *
     * @return : 모델명 매핑 판단용 Gemini system_instruction 텍스트
     * @since : 2026.05.11
     * @version : 0.0.1
     * @author : 최준혁
     */
    private String buildModelMatchSystemInstruction() {
        return "You are an expert in consumer electronics model naming conventions, " +
                "specializing in Korean market products.\n\n" +
                "## Your Task\n" +
                "Determine whether a user-provided model name refers to the SAME product " +
                "as a product title from Naver Shopping search results.\n\n" +
                "## Domain Knowledge: Model Name Structure\n" +
                "Consumer electronics model names (laptops, smartphones, TVs, etc.) follow:\n" +
                "  [Base Model]-[Spec Code][Regional/Color Suffix]\n\n" +
                "### Suffixes to IGNORE (do NOT use as grounds for mismatch):\n" +
                "  - Color or distribution-channel codes: WK, AK, AR, BK, W, K, A, etc.\n" +
                "  - These appear at the END, AFTER the core spec code.\n" +
                "  - Example: KD7WK → core spec is KD7, WK is a suffix → SAME product as KD7.\n" +
                "  - Example: 16Z90TP-KD7WK vs 16Z90TP-KD7 → is_match: true\n\n" +
                "### Must DISTINGUISH (return false if these differ):\n" +
                "  - Product tier / screen size indicator: 16Z vs 17Z (different product line).\n" +
                "  - Spec grade code (CPU/hardware tier): KD7 vs KD5, KD3 vs KD7 (different spec).\n" +
                "  - Generation indicator that affects internal hardware.\n\n" +
                "## Matching Algorithm:\n" +
                "1. Extract the core model number from the user input by stripping trailing alphabetic suffixes.\n" +
                "2. Normalize both strings: remove hyphens, spaces, underscores. Compare case-insensitively.\n" +
                "3. Check if the Naver title CONTAINS the normalized core model number.\n" +
                "4. If yes AND no spec-grade/tier mismatch exists → is_match: true.\n" +
                "5. If the core numbers differ (different tier or spec grade) → is_match: false.\n\n" +
                "## Output Format (strict JSON, no other text):\n" +
                "{\"is_match\": true_or_false, \"reason\": \"Korean explanation\", " +
                "\"refined_model_name\": \"core model name without regional suffix\"}";
    }

    /**
     * 모델명 매핑 전용 유저 프롬프트 생성 메서드
     *
     * @param inputModel : 사용자 입력 모델명
     * @param naverTitle : 네이버 검색 결과 제품명 (HTML 태그 제거 후)
     * @return : 비교 데이터만 담은 최소 유저 프롬프트
     * @since : 2026.05.11
     * @version : 0.0.1
     * @author : 최준혁
     */
    private String buildModelMatchUserPrompt(String inputModel, String naverTitle) {
        return "User input model: " + inputModel + "\nNaver result title: " + naverTitle;
    }

    /**
     * 모델명 매핑 전용 Gemini REST API 요청 본문(JSON) 생성 메서드
     *
     * @param userPrompt : 비교 데이터만 담은 유저 프롬프트
     * @return : Gemini API 요청 JSON 문자열
     * @since : 2026.05.11
     * @version : 0.0.1
     * @author : 최준혁
     */
    private String buildModelMatchRequestBody(String userPrompt) {
        try {
            Map<String, Object> requestMap = Map.of(
                    "system_instruction", Map.of(
                            "role", "system",
                            "parts", List.of(Map.of("text", buildModelMatchSystemInstruction()))),
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", userPrompt)))),
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json"));
            return objectMapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            throw new RuntimeException("Gemini 모델명 매핑 요청 본문 생성 실패", e);
        }
    }

    /**
     * 모델명 매핑 Gemini 응답 파싱 메서드
     * candidates[0].content.parts[0].text 에서 JSON을 추출하여 is_match 값을 반환합니다.
     * 파싱 실패 시 false를 반환하여 안전하게 검색 거부를 유도합니다.
     *
     * @param responseBody : Gemini API 원본 응답 JSON 문자열
     * @param inputModel   : 로깅용 원본 입력 모델명
     * @param naverTitle   : 로깅용 네이버 검색 결과 제목
     * @return : Gemini가 동일 제품으로 판단하면 true, 아니면 false
     * @since : 2026.05.11
     * @version : 0.0.1
     * @author : 최준혁
     */
    private boolean parseModelMatchResponse(String responseBody, String inputModel, String naverTitle) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            String cleanedText = text.trim();
            if (cleanedText.startsWith("```")) {
                cleanedText = cleanedText.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
            }

            JsonNode parsedResult = objectMapper.readTree(cleanedText);
            boolean isMatch = parsedResult.path("is_match").asBoolean(false);
            String reason = parsedResult.path("reason").asText("");
            String refinedModelName = parsedResult.path("refined_model_name").asText(inputModel);

            log.info("[GeminiParsingService] 모델명 매핑 결과 - 입력: '{}', 네이버: '{}', 일치: {}, 정제명: '{}', 사유: {}",
                    inputModel, naverTitle, isMatch, refinedModelName, reason);

            return isMatch;

        } catch (Exception e) {
            log.warn("[GeminiParsingService] 모델명 매핑 응답 파싱 실패, false 반환. 사유: {}", e.getMessage());
            return false;
        }
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
                            "role", "system",
                            "parts", List.of(Map.of("text", buildSystemInstruction()))),
                    /* 데이터 모듈: 유저 프롬프트는 데이터만 */
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", userPrompt)))),
                    /* JSON 응답 강제: 프롬프트에 'JSON으로 답해줘' 구문 보담 안해도 됨 */
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json"));
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
