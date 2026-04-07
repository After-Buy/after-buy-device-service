package com.After_Buy.DeviceService.controller;

import com.After_Buy.DeviceService.dto.request.DeviceRegisterRequest;
import com.After_Buy.DeviceService.dto.response.ApiResponse;
import com.After_Buy.DeviceService.dto.response.DeviceResponse;
import com.After_Buy.DeviceService.dto.response.NaverProductDto;
import com.After_Buy.DeviceService.dto.response.HomeSummaryResponse;
import com.After_Buy.DeviceService.security.UserPrincipal;
import com.After_Buy.DeviceService.service.DeviceService;
import com.After_Buy.DeviceService.service.NaverSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 기기 CRUD 컨트롤러
 * 기기 등록, 조회, 수정, 삭제 API 엔드포인트를 제공합니다.
 * Base URL: /api/devices
 * 포트: 8082
 * 모든 엔드포인트는 JWT Bearer 토큰 인증 필수입니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "기기 CRUD API")
public class DeviceController {

	private final DeviceService deviceService;
	private final NaverSearchService naverSearchService;

	/**
	 * 홈 화면 요약 데이터 조회 API
	 * 사용자의 기기 통계(총 기기 수, 총 가치, 30일 내 만료), 기기 리스트(최근 3대), 보증 임박 기기(1대)를 반환합니다.
	 *
	 * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
	 * @return : 200 OK + 홈 화면 요약 응답 데이터
	 * @since : 2026.04.08
	 * @author : 최준혁
	 */
	@Operation(summary = "홈 화면 요약 데이터 조회", description = "보유 자산 통계, 최근 등록 3개, 보증 시간 가장 임박 1개 기기를 반환합니다.")
	@GetMapping("/home-summary")
	public ResponseEntity<ApiResponse<HomeSummaryResponse>> getHomeSummary(
			@AuthenticationPrincipal UserPrincipal userPrincipal) {
		log.info("홈 화면 요약 조회 요청: userId={}", userPrincipal.getUserId());
		HomeSummaryResponse response = deviceService.getHomeSummary(userPrincipal.getUserId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 기기 등록 API
	 * 새 기기를 등록합니다. warranty_expiry_date는 서버에서 purchase_date + warranty_months로 자동
	 * 계산 후 저장합니다.
	 * image_url이 없을 경우 placeholder 이미지가 자동으로 삽입됩니다.
	 *
	 * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
	 * @param request       : 기기 등록 요청 DTO (@Valid 유효성 검사 적용)
	 * @return : 201 Created + 등록된 기기 전체 정보
	 * @throws com.After_Buy.DeviceService.exception.CustomException : folder_id 검증
	 *                                                               실패 시 DEVICE-001
	 *                                                               (404)
	 */
	@Operation(summary = "기기 등록", description = "새 기기를 등록합니다. warranty_expiry_date는 서버에서 자동 계산됩니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<DeviceResponse>> registerDevice(
			@AuthenticationPrincipal UserPrincipal userPrincipal,
			@Valid @RequestBody DeviceRegisterRequest request) {
		log.info("기기 등록 요청: userId={}", userPrincipal.getUserId());
		DeviceResponse response = deviceService.registerDevice(userPrincipal.getUserId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	/**
	 * 모델명 기반 제품 검색 메소드
	 * OCR 등으로 추출된 제품의 고유 모델명을 기반으로 네이버 쇼핑 API 검색을 수행하여, 가장 연관성이 높은 최상단의 제품 데이터를
	 * 가져옵니다.
	 *
	 * @param modelName : 검색할 고유 모델명 (예: SM-G991N)
	 * @return : 200 OK + NaverProductDto (클라이언트에 반영될 정제된 제품 메타 정보)
	 * @since : 2026.04.07
	 * @version : 1.0.0
	 * @throws : Exception
	 * @author : 최준혁
	 */
	@Operation(summary = "제조사 및 제품 정보 자동 매핑", description = "제품의 고유 모델명을 입력하면 네이버 쇼핑 엔진의 정확도 순(유사도) 매치 결과를 통해 브랜드명, 제품 이미지 URL, 구매처 링크 등을 매핑해옵니다.")
	@GetMapping("/naver-search")
	public ResponseEntity<ApiResponse<NaverProductDto>> getProductFromNaver(
			@RequestParam("modelName") String modelName) {
		log.info("네이버 쇼핑 API 모델명 검색 요청: modelName={}", modelName);
		NaverProductDto response = naverSearchService.searchProduct(modelName);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
