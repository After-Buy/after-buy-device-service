package com.After_Buy.DeviceService.controller;

import com.After_Buy.DeviceService.dto.response.ApiResponse;
import com.After_Buy.DeviceService.dto.response.RootFolderListResponse;
import com.After_Buy.DeviceService.security.UserPrincipal;
import com.After_Buy.DeviceService.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 폴더 관리 컨트롤러
 * 시스템 내의 기기를 그룹화하는 계층형 폴더 구조 관리 API 엔드포인트를 제공합니다.
 * Base URL: /api/devices/folders
 * 포트: 8082
 * 모든 엔드포인트는 JWT Bearer 토큰 인증 필수입니다.
 *
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@RestController
@RequestMapping("/api/devices/folders")
@RequiredArgsConstructor
@Tag(name = "Folders", description = "폴더 관리 API")
public class FolderController {

    private final FolderService folderService;

    /**
     * 루트 폴더 목록 조회 API
     * 최상위(루트) 폴더 목록과 미분류 기기 목록을 조회합니다.
     * 각 폴더에는 직속 하위 항목 수(폴더 + 기기)가 함께 계산되어 포함됩니다.
     *
     * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
     * @return : 200 OK + 루트 폴더 및 미분류 기기 정보 통계 목록(RootFolderListResponse)
     * @since : 2026.04.09
     * @author : 최준혁
     */
    @Operation(summary = "루트 폴더 목록 조회", description = "최상위 폴더 목록과 내부 항목의 총 갯수 및 미분류 기기 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<RootFolderListResponse>> getRootFolders(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("루트 폴더 목록 조회 요청: userId={}", userPrincipal.getUserId());
        RootFolderListResponse response = folderService.getRootFolders(userPrincipal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
