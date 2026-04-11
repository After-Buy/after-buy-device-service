package com.After_Buy.DeviceService.service;

import com.After_Buy.DeviceService.dto.response.ExpiringDeviceDto;
import com.After_Buy.DeviceService.dto.response.InternalWarrantyExpiringResponse;
import com.After_Buy.DeviceService.entity.Device;
import com.After_Buy.DeviceService.repository.DeviceRepository;
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
}
