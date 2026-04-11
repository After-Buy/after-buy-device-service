package com.After_Buy.DeviceService.controller;

import com.After_Buy.DeviceService.dto.request.OcrModifiedFieldsRequest;
import com.After_Buy.DeviceService.dto.request.OcrRequest;
import com.After_Buy.DeviceService.dto.response.ApiResponse;
import com.After_Buy.DeviceService.dto.response.OcrResponse;
import com.After_Buy.DeviceService.security.UserPrincipal;
import com.After_Buy.DeviceService.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR 처리 컨트롤러
 * AWS Lambda(Textract)를 통한 OCR 텍스트 추출 및 오인식 필드 기록 API 엔드포인트를 제공합니다.
 * Base URL: /api/devices/ocr
 * 포트: 8082
 * 모든 엔드포인트는 JWT Bearer 토큰 인증 필수입니다.
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@RestController
@RequestMapping("/api/devices/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR", description = "OCR 텍스트 추출 및 오인식 기록 API")
public class OcrController {

    private final OcrService ocrService;

    /**
     * OCR 텍스트 추출 API
     * Base64 이미지를 AWS Lambda(Textract)로 전달하여 모델명/시리얼/영수증 정보를 추출합니다.
     * 처리 이력은 ocr_logs 테이블에 자동 기록됩니다.
     *
     * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
     * @param request       : OCR 유형과 Base64 인코딩 이미지
     * @return 200 OK + OCR 인식 결과 (성공/실패 여부 포함)
     * @since : 2026.04.12
     * @author : 최준혁
     */
    @Operation(
            summary = "OCR 텍스트 추출",
            description = "Base64 이미지를 AWS Lambda(Textract)로 전달하여 모델명/시리얼/영수증 정보를 추출합니다."
    )
    @PostMapping("")
    public ResponseEntity<ApiResponse<OcrResponse>> processOcr(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OcrRequest request) {

        log.info("OCR 처리 요청 - userId={}, ocrType={}", userPrincipal.getUserId(), request.getOcrType());
        OcrResponse response = ocrService.processOcr(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * OCR 오인식 필드 기록 API
     * OCR 결과를 수정하여 기기를 등록/수정한 후, 어떤 필드를 수정했는지 ocr_logs에 기록합니다.
     *
     * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
     * @param request       : ocr_log_id, device_id, 수정된 필드 목록
     * @return 200 OK + 성공 메시지
     * @since : 2026.04.12
     * @author : 최준혁
     */
    @Operation(
            summary = "OCR 오인식 필드 기록",
            description = "OCR 결과를 수정하여 기기를 저장한 후, 수정된 필드 목록을 ocr_logs에 기록합니다."
    )
    @PostMapping("/modified-fields")
    public ResponseEntity<ApiResponse<Void>> recordModifiedFields(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OcrModifiedFieldsRequest request) {

        log.info("OCR 오인식 필드 기록 요청 - userId={}, ocrLogId={}", userPrincipal.getUserId(), request.getOcrLogId());
        ocrService.recordModifiedFields(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.successMessage("오인식 정보가 기록되었습니다."));
    }
}
