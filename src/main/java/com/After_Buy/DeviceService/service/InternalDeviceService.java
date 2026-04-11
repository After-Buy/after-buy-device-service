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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.After_Buy.DeviceService.dto.response.OcrStatsResponse;
import com.After_Buy.DeviceService.dto.response.FieldModifiedStatDto;
import com.After_Buy.DeviceService.dto.response.DailyFailureTrendDto;

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
        private final ObjectMapper objectMapper;

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

        /**
         * 지정된 기간 동안의 OCR 통계(시도/실패/수정) 정보 조회
         * @param startDate 시작일
         * @param endDate 종료일
         * @return OCR 통계 데이터가 담긴 OcrStatsResponse
         */
        @Transactional(readOnly = true)
        public OcrStatsResponse getOcrStats(LocalDate startDate, LocalDate endDate) {
                log.info("[InternalDeviceService] OCR 통계 집계 요청: {} ~ {}", startDate, endDate);

                // 파라미터로 넘어온 LocalDate를 00:00:00 ~ 23:59:59 형태의 LocalDateTime으로 변환
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

                // 1. 단순 aggregate count 도출
                long totalAttempts = ocrLogRepository.countByCreatedAtBetween(startDateTime, endDateTime);
                long failureCount = ocrLogRepository.countByIsSuccessFalseAndCreatedAtBetween(startDateTime, endDateTime);

                // 2. field_modified_stats 및 modified_count 도출
                List<String> modifiedFieldsJsonList = ocrLogRepository.findModifiedFieldsByCreatedAtBetween(startDateTime, endDateTime);

                // modified_count는 빈 JSON 배열이나 유효하지 않은 기록 등을 제외하고, 실제로 필드를 하나라도 수정한 '로그의 건수'라고 정의함
                long modifiedCount = 0;
                Map<String, Long> fieldFrequencyMap = new HashMap<>();

                for (String jsonText : modifiedFieldsJsonList) {
                        try {
                                List<String> fields = objectMapper.readValue(jsonText, new TypeReference<List<String>>() {});
                                if (fields != null && !fields.isEmpty()) {
                                        modifiedCount++;
                                        for (String field : fields) {
                                                fieldFrequencyMap.put(field, fieldFrequencyMap.getOrDefault(field, 0L) + 1L);
                                        }
                                }
                        } catch (JsonProcessingException e) {
                                log.warn("[InternalDeviceService] OCR 수정 빈도 JSON 파싱 오류: {}", jsonText, e);
                        }
                }

                // Map 형태의 빈도를 FieldModifiedStatDto 목록으로 치환 후, 내림차순 정렬
                List<FieldModifiedStatDto> fieldStats = fieldFrequencyMap.entrySet().stream()
                                .map(entry -> FieldModifiedStatDto.builder()
                                                .fieldName(entry.getKey())
                                                .modifiedCount(entry.getValue())
                                                .build())
                                .sorted((a, b) -> Long.compare(b.getModifiedCount(), a.getModifiedCount()))
                                .collect(Collectors.toList());

                // 3. daily_failure_trend 도출 (Native Query의 List<Object[]> 매핑)
                List<Object[]> rawTrend = ocrLogRepository.findDailyFailureTrendRaw(startDateTime, endDateTime);
                List<DailyFailureTrendDto> dailyTrend = rawTrend.stream().map(row -> {
                        // row[0]은 DATE 형태(java.sql.Date 또는 String), row[1]은 카운트 수(Number)
                        String dateStr = row[0].toString();
                        Long failCnt = ((Number) row[1]).longValue();

                        return DailyFailureTrendDto.builder()
                                        .date(dateStr)
                                        .failureCount(failCnt)
                                        .build();
                }).collect(Collectors.toList());

                return OcrStatsResponse.builder()
                                .totalAttempts(totalAttempts)
                                .failureCount(failureCount)
                                .modifiedCount(modifiedCount)
                                .fieldModifiedStats(fieldStats)
                                .dailyFailureTrend(dailyTrend)
                                .build();
        }
}
