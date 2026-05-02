package com.After_Buy.DeviceService.controller;

import com.After_Buy.DeviceService.dto.request.DeviceRegisterRequest;
import com.After_Buy.DeviceService.dto.request.DeviceUpdateRequest;
import com.After_Buy.DeviceService.dto.request.DeviceNameUpdateRequest;
import com.After_Buy.DeviceService.dto.response.ApiResponse;
import com.After_Buy.DeviceService.dto.response.DeviceResponse;
import com.After_Buy.DeviceService.dto.response.NaverProductDto;
import com.After_Buy.DeviceService.dto.response.HomeSummaryResponse;
import com.After_Buy.DeviceService.dto.response.DeviceListResponse;
import com.After_Buy.DeviceService.dto.response.DeviceDetailResponse;
import com.After_Buy.DeviceService.dto.response.DeviceNameUpdateResponse;
import com.After_Buy.DeviceService.dto.request.PresignedUrlRequest;
import com.After_Buy.DeviceService.dto.response.PresignedUrlResponse;
import com.After_Buy.DeviceService.security.UserPrincipal;
import com.After_Buy.DeviceService.service.DeviceService;
import com.After_Buy.DeviceService.service.NaverSearchService;
import com.After_Buy.DeviceService.service.S3Service;
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
	private final S3Service s3Service;

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
	@PostMapping("/register")
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

	/**
	 * 미분류 기기 목록 조회 API
	 * 미분류(folder_id = NULL) 상태의 기기 목록을 조회합니다.
	 * 
	 * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
	 * @param sort : 정렬 조건 (created_desc: 최신등록순(기본값), expiry_asc: 보증만료임박순)
	 * @return : 200 OK + 미분류 기기 목록(DeviceListResponse)
	 * @since : 2026.04.08
	 * @author : 최준혁
	 */
	@Operation(summary = "미분류 기기 목록 조회", description = "폴더에 속하지 않은 미분류 기기 목록을 정렬 조건에 따라 조회합니다.")
	@GetMapping("/home")
	public ResponseEntity<ApiResponse<DeviceListResponse>> getDeviceList(
			@AuthenticationPrincipal UserPrincipal userPrincipal,
			@RequestParam(value = "sort", defaultValue = "created_desc") String sort) {
		log.info("미분류 기기 목록 조회 요청: userId={}, sort={}", userPrincipal.getUserId(), sort);
		DeviceListResponse response = deviceService.getDeviceList(userPrincipal.getUserId(), sort);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 기기 상세 내역 조회 API
	 * 기기 ID를 기반으로 해당 기기의 모든 상세 정보와 보증 잔여 일수 실시간 계산 결과를 조회합니다.
	 * 
	 * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
	 * @param deviceId : 조회할 대상 기기의 고유 ID
	 * @return : 200 OK + 상세 기기 정보(DeviceDetailResponse)
	 * @throws com.After_Buy.DeviceService.exception.CustomException :
	 *             본인 기기가 아닌 경우 DEVICE-002 (403), 존재하지 않는 경우 DEVICE-003 (404)
	 * @since : 2026.04.08
	 * @author : 최준혁
	 */
	@Operation(summary = "기기 상세 내역 조회", description = "특정 기기의 상세 정보와 실시간 보증 잔여 일수를 반환합니다. 본인의 기기만 조회할 수 있습니다.")
	@GetMapping("/{device_id}")
	public ResponseEntity<ApiResponse<DeviceDetailResponse>> getDeviceDetail(
			@AuthenticationPrincipal UserPrincipal userPrincipal,
			@PathVariable("device_id") Long deviceId) {
		log.info("기기 상세 정보 조회 요청: userId={}, deviceId={}", userPrincipal.getUserId(), deviceId);
		DeviceDetailResponse response = deviceService.getDeviceDetail(userPrincipal.getUserId(), deviceId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 기기 정보 전체 수정 API
	 * 기기의 전체 정보(model_name 제외)를 갱신합니다. 무상 보증 기간이나 구매일이 변경될 경우
	 * 보증 만료일은 서버에서 자동 재계산됩니다.
	 *
	 * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
	 * @param deviceId : 수정할 대상 기기의 고유 ID
	 * @param request : 기기 수정 데이터
	 * @return : 200 OK + 수정된 상세 기기 정보(DeviceDetailResponse)
	 * @since : 2026.04.08
	 * @author : 최준혁
	 */
	@Operation(summary = "기기 정보 갱신", description = "기기 정보를 전체 교체합니다. model_name은 수정할 수 없으며, 구매일/보증조건 변경 시 보증 만료일이 자동 갱신됩니다.")
	@PutMapping("/{device_id}")
	public ResponseEntity<ApiResponse<DeviceDetailResponse>> updateDevice(
			@AuthenticationPrincipal UserPrincipal userPrincipal,
			@PathVariable("device_id") Long deviceId,
			@Valid @RequestBody DeviceUpdateRequest request) {
		log.info("기기 정보 전체 수정 요청: userId={}, deviceId={}", userPrincipal.getUserId(), deviceId);
		DeviceDetailResponse response = deviceService.updateDevice(userPrincipal.getUserId(), deviceId, request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 기기 상품명 단일 수정 API
	 * 아이템 목록 화면 등의 메뉴에서 상품명만 간편하게 변경할 때 사용됩니다.
	 * 
	 * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
	 * @param deviceId : 수정할 대상 기기의 고유 ID
	 * @param request : 변경할 상품명 데이터
	 * @return : 200 OK + 단일 수정 응답 객체
	 * @since : 2026.04.08
	 * @author : 최준혁
	 */
	@Operation(summary = "기기 상품명 단일 수정", description = "아이템 목록 화면에서 상품명만 간단히 변경할 때 호출합니다.")
	@PatchMapping("/{device_id}/name")
	public ResponseEntity<ApiResponse<DeviceNameUpdateResponse>> updateDeviceName(
			@AuthenticationPrincipal UserPrincipal userPrincipal,
			@PathVariable("device_id") Long deviceId,
			@Valid @RequestBody DeviceNameUpdateRequest request) {
		log.info("기기 상품명 단일 수정 요청: userId={}, deviceId={}, newProductName={}", userPrincipal.getUserId(), deviceId, request.getProductName());
		DeviceNameUpdateResponse response = deviceService.updateDeviceName(userPrincipal.getUserId(), deviceId, request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/**
	 * 기기 삭제 API
	 * 기기를 영구 삭제합니다.
	 *
	 * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
	 * @param deviceId : 삭제할 기기의 고유 ID
	 * @return : 200 OK + 성공 메시지
	 * @since : 2026.04.09
	 * @author : 최준혁
	 */
	@Operation(summary = "기기 삭제", description = "기기를 영구 삭제합니다.")
	@DeleteMapping("/{device_id}")
	public ResponseEntity<ApiResponse<Void>> deleteDevice(
			@AuthenticationPrincipal UserPrincipal userPrincipal,
			@PathVariable("device_id") Long deviceId) {
		log.info("기기 삭제 요청: userId={}, deviceId={}", userPrincipal.getUserId(), deviceId);
		deviceService.deleteDevice(userPrincipal.getUserId(), deviceId);
		return ResponseEntity.ok(ApiResponse.successMessage("기기가 삭제되었습니다."));
	}

	/**
	 * 기기 이미지 S3 직접 업로드용 Presigned URL 발급 API
	 *
	 * @param request : 이미지 확장자 정보
	 * @return : 200 OK + Presigned URL 정보
	 * @since : 2026.04.12
	 * @author : 최준혁
	 */
	@Tag(name = "Image Upload", description = "이미지 업로드")
	@Operation(summary = "S3 Pre-signed URL 발급 (기기 이미지 업로드 전용)", description = "S3에 직접 이미지를 업로드하기 위한 Presigned URL을 발급합니다.")
	@PostMapping("/images/presigned-url")
	public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
			@Valid @RequestBody PresignedUrlRequest request) {
		log.info("S3 Presigned URL 발급 요청: ext={}", request.getFileExtension());
		PresignedUrlResponse response = s3Service.generatePresignedUrl(request);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
