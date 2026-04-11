package com.After_Buy.DeviceService.service;

import com.After_Buy.DeviceService.dto.response.ExpiringDeviceDto;
import com.After_Buy.DeviceService.dto.response.InternalWarrantyExpiringResponse;
import com.After_Buy.DeviceService.entity.Device;
import com.After_Buy.DeviceService.repository.DeviceRepository;
import com.After_Buy.DeviceService.repository.FolderRepository;
import com.After_Buy.DeviceService.repository.OcrLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 인프라 및 서비스 간 내부 통신용 서비스
 * 
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalDeviceService {

        private final DeviceRepository deviceRepository;
        private final FolderRepository folderRepository;
        private final OcrLogRepository ocrLogRepository;

        /**
         * 보증 만료 D-day 기기 목록 조회
         * 
         * @param days : 남은 보증 기간 일수 (ex: 30, 14, 1, 0)
         * @return InternalWarrantyExpiringResponse
         */
        @Transactional(readOnly = true)
        public InternalWarrantyExpiringResponse getWarrantyExpiringDevices(int days) {
                log.info("[InternalDeviceService] 보증 만료 임박(D-{}) 기기 조회", days);

                // DB 서버의 타임존(UTC)과 애플리케이션 서버(KST)의 시차가 발생할 수 있으므로,
                // 확실히 KST 기준의 Spring Boot 시간으로 타겟 날짜를 계산해서 쿼리에 넣습니다.
                LocalDate targetDate = LocalDate.now().plusDays(days);

                List<Device> devices = deviceRepository.findByWarrantyExpiryDateDday(targetDate);

                List<ExpiringDeviceDto> dtos = devices.stream()
                                .map(device -> ExpiringDeviceDto.builder()
                                                .deviceId(device.getDeviceId())
                                                .userId(device.getUserId())
                                                .productName(device.getProductName())
                                                .imageUrl(device.getImageUrl())
                                                .warrantyExpiryDate(device.getWarrantyExpiryDate() != null
                                                                ? device.getWarrantyExpiryDate().toString()
                                                                : null)
                                                .build())
                                .collect(Collectors.toList());

                return InternalWarrantyExpiringResponse.builder()
                                .devices(dtos)
                                .build();
        }

        /**
         * 회원 탈퇴 시 해당 사용자의 모든 기기 정보(OCR, 기기, 폴더)를 연쇄 삭제
         * 
         * @param userId 회원 탈퇴를 요청한 사용자 ID
         */
        @Transactional
        public void deleteAllUserData(Long userId) {
                log.info("[InternalDeviceService] 회원 탈퇴에 따른 사용자(userId={}) 데이터 전체 삭제 시작", userId);
                try {
                        // 1. 부모 객체를 가진 devices 삭제 (OCR 로그는 통계 집계용으로 삭제하지 않고 보존)
                        deviceRepository.deleteAllByUserId(userId);
                        // 2. 최상위(단, 재귀 부모를 가질 수 있는) folders 삭제
                        folderRepository.deleteAllByUserId(userId);

                        log.info("[InternalDeviceService] 삭제 성공 - 사용자(userId={})의 모든 데이터 영구 정리 완료 (OCR 로그 제외)", userId);
                } catch (Exception e) {
                        log.error("[InternalDeviceService] 삭제 중 심각한 오류 발생 - 사용자(userId={})의 기기 데이터 제거 실패", userId, e);
                }
        }
}
