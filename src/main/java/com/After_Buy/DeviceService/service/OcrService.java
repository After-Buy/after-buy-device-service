package com.After_Buy.DeviceService.service;

import com.After_Buy.DeviceService.dto.request.OcrModifiedFieldsRequest;
import com.After_Buy.DeviceService.dto.request.OcrRequest;
import com.After_Buy.DeviceService.dto.response.OcrResponse;
import com.After_Buy.DeviceService.dto.response.OcrResultDto;
import com.After_Buy.DeviceService.entity.OcrLog;
import com.After_Buy.DeviceService.entity.enums.OcrType;
import com.After_Buy.DeviceService.exception.CustomException;
import com.After_Buy.DeviceService.exception.ErrorCode;
import com.After_Buy.DeviceService.repository.OcrLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR 처리 서비스
 * Lambda Function URL을 통해 AWS Lambda(Textract)를 HTTP 호출하여 OCR 결과를 반환하고
 * ocr_logs 테이블에 처리 이력을 자동 기록합니다.
 *
 * 처리 흐름: 앱 → Device Service → Lambda Function URL → Amazon Textract → Lambda 파싱 → Device Service → 앱
 *
 * @since : 2026.04.12
 * @version : 2.0.0
 * @author : 최준혁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final OcrLogRepository ocrLogRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${aws.lambda.function-url}")
    private String lambdaFunctionUrl;

    @Value("${internal.secret-key}")
    private String internalSecretKey;

    private WebClient lambdaWebClient;

    @PostConstruct
    void init() {
        this.lambdaWebClient = webClientBuilder.build();
    }

    /**
     * OCR 텍스트 추출 메인 메서드
     * Lambda Function URL에 HTTP POST로 요청하여 OCR 결과를 반환하며 ocr_logs에 이력을 기록합니다.
     *
     * @param userId  : 요청 사용자 ID (JWT에서 추출)
     * @param request : OCR 요청 (유형 + Base64 이미지)
     * @return OcrResponse : OCR 결과 응답
     */
    @Transactional
    public OcrResponse processOcr(Long userId, OcrRequest request) {
        log.info("[OcrService] OCR 처리 요청 - userId={}, ocrType={}", userId, request.getOcrType());

        // Lambda 요청 페이로드 구성
        Map<String, String> payload = new HashMap<>();
        payload.put("ocr_type", request.getOcrType().name());
        payload.put("image_base64", request.getImageBase64());

        // Lambda Function URL HTTP 호출
        String responseBody = invokeLambda(payload);

        // 결과 파싱 및 응답 DTO 구성
        OcrResponse ocrResponse = parseOcrResponse(request.getOcrType(), responseBody);

        // OCR 이력을 DB에 기록 (성공/실패 모두 기록)
        OcrLog ocrLog = OcrLog.builder()
                .userId(userId)
                .ocrType(request.getOcrType())
                .isSuccess(ocrResponse.getIsSuccess())
                .build();
        OcrLog savedLog = ocrLogRepository.save(ocrLog);
        log.info("[OcrService] OCR 로그 저장 완료 - ocrLogId={}", savedLog.getOcrLogId());

        // 클라이언트가 modified-fields 호출 시 사용할 로그 ID를 응답에 포함
        return OcrResponse.builder()
                .ocrType(ocrResponse.getOcrType())
                .ocrLogId(savedLog.getOcrLogId())
                .isSuccess(ocrResponse.getIsSuccess())
                .result(ocrResponse.getResult())
                .message(ocrResponse.getMessage())
                .build();
    }

    /**
     * OCR 오인식 필드 기록 메서드
     * 사용자가 OCR 결과를 수정하여 기기를 저장한 후 호출합니다.
     * 수정된 필드 목록과 연결된 기기 ID를 해당 OCR 로그에 업데이트합니다.
     *
     * @param userId  : 요청 사용자 ID (소유권 검증용)
     * @param request : 오인식 기록 요청 (ocr_log_id, device_id, modified_fields)
     */
    @Transactional
    public void recordModifiedFields(Long userId, OcrModifiedFieldsRequest request) {
        log.info("[OcrService] OCR 오인식 필드 기록 - userId={}, ocrLogId={}", userId, request.getOcrLogId());

        // OCR 로그 존재 여부 확인
        OcrLog ocrLog = ocrLogRepository.findById(request.getOcrLogId())
                .orElseThrow(() -> new CustomException(ErrorCode.OCR_LOG_NOT_FOUND));

        // 수정된 필드 목록을 JSON 배열 문자열로 변환 (예: ["purchase_date","purchase_price"])
        String modifiedFieldsJson = convertFieldsToJson(request.getModifiedFields());

        // 로그에 기기 ID와 수정 필드 기록
        ocrLog.updateModifiedFields(request.getDeviceId(), modifiedFieldsJson);

        log.info("[OcrService] OCR 오인식 필드 기록 완료 - ocrLogId={}, modifiedFields={}",
                request.getOcrLogId(), modifiedFieldsJson);
    }

    /* ===== Private 내부 메서드 ===== */

    /**
     * Lambda Function URL에 HTTP POST 요청을 전송합니다.
     * x-internal-secret-key 헤더로 서비스 간 인증을 수행합니다.
     *
     * @param payload : Lambda에 전달할 요청 body (ocr_type, image_base64)
     * @return Lambda 응답 body JSON 문자열 (Function URL 래핑 해제 후)
     */
    private String invokeLambda(Map<String, String> payload) {
        try {
            return lambdaWebClient.post()
                    .uri(lambdaFunctionUrl)
                    .header("x-internal-secret-key", internalSecretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .doOnNext(errorBody -> log.error(
                                            "[OcrService] Lambda 호출 오류 (HTTP {}): {}",
                                            clientResponse.statusCode().value(), errorBody))
                                    .flatMap(errorBody -> reactor.core.publisher.Mono.error(
                                            new RuntimeException("Lambda HTTP 오류 " + clientResponse.statusCode().value()
                                                    + ": " + errorBody)))
                    )
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[OcrService] Lambda 호출 HTTP 오류 - status={}", e.getStatusCode(), e);
            throw new CustomException(ErrorCode.OCR_RECOGNITION_FAILED);
        } catch (Exception e) {
            log.error("[OcrService] Lambda 호출 실패", e);
            throw new CustomException(ErrorCode.OCR_RECOGNITION_FAILED);
        }
    }

    /**
     * Lambda 응답 JSON을 OCR 유형에 맞게 파싱하여 OcrResponse를 반환합니다.
     * Lambda Function URL 응답: {"statusCode": 200, "body": "{...}", "headers": {...}}
     * → body 필드를 추출하여 내부 JSON을 파싱합니다.
     *
     * @param ocrType      : OCR 유형
     * @param responseJson : Lambda Function URL이 반환한 JSON 문자열
     * @return OcrResponse : 파싱된 응답 DTO
     */
    private OcrResponse parseOcrResponse(OcrType ocrType, String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            // Function URL 응답 래핑 해제: body 필드에서 실제 JSON 추출
            JsonNode bodyNode = root.path("body");
            if (!bodyNode.isMissingNode() && bodyNode.isTextual()) {
                root = objectMapper.readTree(bodyNode.asText());
            }

            // Lambda에서 is_success 필드로 성공 여부를 내려준다고 가정
            boolean isSuccess = root.path("is_success").asBoolean(false);

            if (!isSuccess) {
                // OCR 인식 실패 — 클라이언트에 안내 메시지 반환
                return OcrResponse.builder()
                        .ocrType(ocrType)
                        .isSuccess(false)
                        .result(null)
                        .message("텍스트를 인식하지 못했습니다. 다시 시도해주세요.")
                        .build();
            }

            // 성공: OCR 유형에 따라 결과 필드를 추출
            OcrResultDto result = parseResultByType(ocrType, root.path("result"));

            return OcrResponse.builder()
                    .ocrType(ocrType)
                    .isSuccess(true)
                    .result(result)
                    .build();

        } catch (Exception e) {
            log.error("[OcrService] Lambda 응답 파싱 실패 - responseJson={}", responseJson, e);
            throw new CustomException(ErrorCode.OCR_RECOGNITION_FAILED);
        }
    }

    /**
     * OCR 유형별로 result 노드를 파싱하여 OcrResultDto를 반환합니다.
     *
     * @param ocrType    : OCR 유형 (MODEL, SERIAL, RECEIPT)
     * @param resultNode : Lambda 응답 JSON의 result 노드
     * @return OcrResultDto : 유형별 결과 데이터
     */
    private OcrResultDto parseResultByType(OcrType ocrType, JsonNode resultNode) {
        return switch (ocrType) {
            case MODEL -> OcrResultDto.builder()
                    .modelName(resultNode.path("model_name").asText(null))
                    .build();

            case SERIAL -> OcrResultDto.builder()
                    .serialNumber(resultNode.path("serial_number").asText(null))
                    .build();

            case RECEIPT -> OcrResultDto.builder()
                    .purchaseDate(resultNode.path("purchase_date").asText(null))
                    .purchasePrice(resultNode.has("purchase_price")
                            ? new BigDecimal(resultNode.path("purchase_price").asText())
                            : null)
                    .purchaseStore(resultNode.path("purchase_store").asText(null))
                    .build();
        };
    }

    /**
     * 수정된 필드 목록(List)을 JSON 배열 형식의 문자열로 변환합니다.
     * 예: ["purchase_date", "purchase_price"]
     *
     * @param fields : 수정된 필드명 리스트
     * @return JSON 배열 문자열
     */
    private String convertFieldsToJson(List<String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            log.error("[OcrService] modified_fields JSON 변환 실패", e);
            return "[]";
        }
    }
}
