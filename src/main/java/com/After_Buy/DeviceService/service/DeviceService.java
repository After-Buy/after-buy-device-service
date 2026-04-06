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
}
