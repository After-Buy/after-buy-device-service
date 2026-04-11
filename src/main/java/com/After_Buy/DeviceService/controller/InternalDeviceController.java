package com.After_Buy.DeviceService.controller;

import com.After_Buy.DeviceService.dto.response.InternalWarrantyExpiringResponse;
import com.After_Buy.DeviceService.dto.response.InternalDeleteUserDataResponse;
import com.After_Buy.DeviceService.service.InternalDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 인프라 및 서비스 간 내부 통신 제어 컨트롤러
 * 클라이언트 엔드포인트가 아닌 MSA 아키텍처 상의 타 서비스(Notification, Admin 등)에서 
 * 자체 통신을 위해 사용합니다. (외부 Nginx 라우팅 미적용)
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Tag(name = "Internal Device", description = "MSA 내부 통신 전용 Device API (Notification/Admin/Auth -> Device)")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalDeviceController {

    private final InternalDeviceService internalDeviceService;

    /**
     * 보증 만료 임박 기기 목록 조회 API
     * Notification Service의 보증기간 알림 스케줄러가 매일 정해진 시간에 호출합니다.
     * 결과가 다음 발송 로직의 입력값이므로 동기(Synchronous) 방식으로 처리합니다.
     *
     * @param days 남은 일수 지정 (ex: 30, 14, 1, 0)
     * @return 일자 조건에 해당하는 기기 목록
     */
    @Operation(summary = "[Internal] 보증 만료 목록 조회",
               description = "보증 만료일이 정확히 N일 남은(D-N) 기기 목록을 반환합니다. (Notification 스케줄러 호출용)")
    @Parameter(name = "X-Internal-Secret", description = "내부 보안 키", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string"))
    @GetMapping("/devices/warranty-expiring")
    public ResponseEntity<InternalWarrantyExpiringResponse> getWarrantyExpiringDevices(
            @RequestParam("days") int days) {
        
        log.info("Internal API 호출 - 보증 만료 {}일 남은 기기 조회", days);
        return ResponseEntity.ok(internalDeviceService.getWarrantyExpiringDevices(days));
    }

    /**
     * 회원 탈퇴 시 사용자 연관 데이터 전체 삭제 API
     * Auth Service가 회원 탈퇴 처리 시 호출합니다.
     * 
     * @param userId 탈퇴하려는 사용자 ID
     * @return 즉시 200 OK와 삭제 접수 상태를 반환
     */
    @Operation(summary = "[Internal] 회원 전체 데이터 삭제 (비동기)",
               description = "회원 탈퇴 시 호출되어 해당 사용자의 OCR, 기기, 폴더 데이터를 연쇄 삭제합니다. (Auth 호출용)")
    @Parameter(name = "X-Internal-Secret", description = "내부 보안 키", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string"))
    @DeleteMapping("/devices/users/{userId}")
    public ResponseEntity<InternalDeleteUserDataResponse> deleteUserData(
            @PathVariable("userId") Long userId) {
        
        log.info("Internal API 호출 - 회원(userId={}) 탈퇴 데이터 삭제 요청 접수 (비동기 처리 진행)", userId);
        
        // Fire & Forget 적용: 응답에 지장을 주지 않도록 백그라운드 스레드에서 삭제 수행
        CompletableFuture.runAsync(() -> internalDeviceService.deleteAllUserData(userId));

        // 삭제 작업 완료를 기다리지 않고 명세서 기준 즉시 200 OK를 반환합니다.
        return ResponseEntity.ok(
                InternalDeleteUserDataResponse.builder()
                        .deleted(true)
                        .user_id(userId)
                        .build()
        );
    }
}
