package com.After_Buy.DeviceService.service;

import com.After_Buy.DeviceService.dto.request.DeviceRegisterRequest;
import com.After_Buy.DeviceService.dto.response.DeviceResponse;
import com.After_Buy.DeviceService.entity.Device;
import com.After_Buy.DeviceService.exception.CustomException;
import com.After_Buy.DeviceService.exception.ErrorCode;
import com.After_Buy.DeviceService.repository.DeviceRepository;
import com.After_Buy.DeviceService.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 기기 비즈니스 로직 서비스
 * 기기 CRUD 관련 핵심 비즈니스 로직을 처리합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

	private final DeviceRepository deviceRepository;
	private final FolderRepository folderRepository;

	/**
	 * 기기 등록 메소드
	 * 클라이언트로부터 받은 등록 요청으로 새 기기를 DB에 저장합니다.
	 *
	 * 처리 흐름:
	 * 1. folder_id가 있을 경우 폴더 존재 여부 및 소유권 검증 (실패 시 DEVICE-001 404)
	 * 2. image_url 정보는 null을 허용합니다 (선택 사항).
	 * 3. warranty_expiry_date = purchase_date + warranty_months 자동 계산
	 * 4. Device 엔티티 저장 후 DeviceResponse 반환
	 *
	 * @param userId  : JWT 토큰에서 추출한 인증된 사용자 ID
	 * @param request : 기기 등록 요청 DTO
	 * @return : 저장된 기기 전체 정보 응답 DTO
	 * @throws CustomException : folder_id가 존재하지 않거나 본인 폴더가 아닌 경우 (DEVICE-001, 404)
	 */
	@Transactional
	public DeviceResponse registerDevice(Long userId, DeviceRegisterRequest request) {
		/* 1. folder_id 검증: null이 아닐 때만 검증 수행 */
		if (request.getFolderId() != null) {
			boolean isFolderValid = folderRepository
					.findByFolderIdAndUserId(request.getFolderId(), userId)
					.isPresent();

			if (!isFolderValid) {
				/* folder_id가 존재하지 않거나 본인 폴더가 아닌 경우 → API 명세서: DEVICE-001, 404 */
				log.warn("폴더 검증 실패: folderId={}, userId={}", request.getFolderId(), userId);
				throw new CustomException(ErrorCode.DEVICE_FOLDER_NOT_FOUND);
			}
		}

		/* 2. warranty_expiry_date 자동 계산: purchase_date + warranty_months */
		LocalDate warrantyExpiryDate = request.getPurchaseDate().plusMonths(request.getWarrantyMonths());

		/* 4. Device 엔티티 Builder 패턴으로 생성 후 저장 */
		Device device = Device.builder()
				.userId(userId)
				.folderId(request.getFolderId())
				.productName(request.getProductName())
				.modelName(request.getModelName())
				.brand(request.getBrand())
				.imageUrl(request.getImageUrl())
				.productLinkUrl(request.getProductLinkUrl())
				.purchaseDate(request.getPurchaseDate())
				.purchasePrice(request.getPurchasePrice())
				.purchaseStore(request.getPurchaseStore())
				.warrantyMonths(request.getWarrantyMonths())
				.warrantyExpiryDate(warrantyExpiryDate)
				.serialNumber(request.getSerialNumber())
				.memo(request.getMemo())
				.build();

		Device savedDevice = deviceRepository.save(device);
		log.info("기기 등록 완료: deviceId={}, userId={}", savedDevice.getDeviceId(), userId);

		return DeviceResponse.from(savedDevice);
	}

	/**
	 * 홈 화면 데이터 요약 조회
	 * 사용자의 기기 통계(총 기기 수, 총 가치, 30일 내 만료), 기기 리스트(최근 3대), 보증 임박 기기(1대)를 반환합니다.
	 *
	 * @param userId : 조회할 사용자 ID
	 * @return : 홈 화면 요약 응답 객체
	 */
	@Transactional(readOnly = true)
	public com.After_Buy.DeviceService.dto.response.HomeSummaryResponse getHomeSummary(Long userId) {
		// 1. 자산 통계 조회
		Long totalDevices = deviceRepository.countByUserId(userId);
		java.math.BigDecimal totalValue = deviceRepository.sumPurchasePriceByUserId(userId);
		LocalDate endDate = LocalDate.now().plusDays(30);
		Long expiringSoonCount = deviceRepository.countExpiringSoonByUserId(userId, endDate);

		com.After_Buy.DeviceService.dto.response.SummaryDto summary = com.After_Buy.DeviceService.dto.response.SummaryDto.builder()
				.total_devices(totalDevices.intValue())
				.total_value(totalValue)
				.expiring_soon_count(expiringSoonCount.intValue())
				.build();

		// 2. 최근 등록된 기기 조회 (최대 3개)
		java.util.List<Device> recents = deviceRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId);
		java.util.List<com.After_Buy.DeviceService.dto.response.HomeDeviceDto> recentDevices = recents.stream()
				.map(device -> mapToHomeDeviceDto(device, true))
				.collect(java.util.stream.Collectors.toList());

		// 3. 가장 보증 만료가 임박한 기기 조회 (Native Query 이용)
		java.util.Optional<Device> urgentDeviceOpt = deviceRepository.findTopByUserIdOrderByWarrantyExpiryDateClosest(userId);
		com.After_Buy.DeviceService.dto.response.HomeDeviceDto urgentDevice = urgentDeviceOpt.map(device -> mapToHomeDeviceDto(device, false)).orElse(null);

		return com.After_Buy.DeviceService.dto.response.HomeSummaryResponse.builder()
				.summary(summary)
				.recent_devices(recentDevices)
				.urgent_device(urgentDevice)
				.build();
	}

	/**
	 * Device Entity -> HomeDeviceDto 매핑 유틸 메서드
	 *
	 * @param device   변환할 기기 엔티티
	 * @param isRecent 최근 기기 리스트인지 여부 (true: modelName, createdAt 포함 / false: productLinkUrl 포함)
	 * @return HomeDeviceDto 객체
	 */
	private com.After_Buy.DeviceService.dto.response.HomeDeviceDto mapToHomeDeviceDto(Device device, boolean isRecent) {
		long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), device.getWarrantyExpiryDate());

		com.After_Buy.DeviceService.dto.response.HomeDeviceDto.HomeDeviceDtoBuilder builder = com.After_Buy.DeviceService.dto.response.HomeDeviceDto.builder()
				.device_id(device.getDeviceId())
				.product_name(device.getProductName())
				.brand(device.getBrand())
				.image_url(device.getImageUrl())
				.warranty_expiry_date(device.getWarrantyExpiryDate())
				.days_remaining(remainingDays);

		if (isRecent) {
			builder.model_name(device.getModelName());
			builder.created_at(device.getCreatedAt());
		} else {
			builder.product_link_url(device.getProductLinkUrl());
		}

		return builder.build();
	}
}
