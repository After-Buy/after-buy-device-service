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

    /**
     * 특정 폴더 내부 항목 조회 API
     * 현재 지정한 폴더 내부에 들어있는 하위 폴더들과 기기 목록을 가져옵니다.
     * 추가적으로 좌측 상단 Breadcrumb 경로 출력을 위해 루트부터 역추적한 경로 배열도 포함합니다.
     *
     * @param userPrincipal : JWT 토큰에서 추출된 인증 사용자 정보
     * @param folderId : 현재 진입한 폴더의 고유 식별자 ID
     * @return : 200 OK + 내부 폴더, 기기 목록, Breadcrumb 데이터 등이 담긴 객체 리스폰스 (FolderItemsResponse)
     * @since : 2026.04.09
     * @author : 최준혁
     */
    @Operation(summary = "특정 폴더 내용 조회", description = "특정 폴더 내의 직속 하위 폴더, 소속 기기 목록 및 Breadcrumb 경로를 반환합니다.")
    @GetMapping("/{folder_id}/items")
    public ResponseEntity<ApiResponse<com.After_Buy.DeviceService.dto.response.FolderItemsResponse>> getFolderItems(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @org.springframework.web.bind.annotation.PathVariable("folder_id") Long folderId) {
        log.info("특정 폴더 내용 조회 요청: userId={}, folderId={}", userPrincipal.getUserId(), folderId);
        com.After_Buy.DeviceService.dto.response.FolderItemsResponse response = folderService.getFolderItems(userPrincipal.getUserId(), folderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 폴더 생성 API
     * 루트 또는 특정 폴더 경로 내부에 새로운 폴더를 생성합니다.
     * 
     * @param userPrincipal : JWT에서 파싱된 인증 정보
     * @param request       : 생성 요청 정보 (폴더명 필수)
     * @return : 201 Created 상태코드 및 방금 생성된 폴더 내역
     * @since : 2026.04.09
     * @author : 최준혁
     */
    @Operation(summary = "폴더 생성", description = "새로운 기기 분류용 폴더를 생성합니다.")
    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<ApiResponse<com.After_Buy.DeviceService.dto.response.FolderCreateResponse>> createFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.After_Buy.DeviceService.dto.request.FolderCreateRequest request) {
        log.info("폴더 생성 요청: userId={}, folderName={}, parentId={}",
                 userPrincipal.getUserId(), request.getFolderName(), request.getParentFolderId());

        com.After_Buy.DeviceService.dto.response.FolderCreateResponse response = folderService.createFolder(userPrincipal.getUserId(), request);

        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                             .body(ApiResponse.success(response));
    }

    /**
     * 폴더명 수정 API
     * 기존에 생성된 특정 폴더의 이름을 변경합니다.
     * 본인 소유의 폴더만 수정 가능합니다.
     *
     * @param userPrincipal : 인증된 사용자
     * @param folderId      : 이름을 수정할 폴더 ID
     * @param request       : 새로운 폴더명 객체
     * @return : 200 OK + 수정 완료된 폴더 데이터
     * @since : 2026.04.09
     * @author : 최준혁
     */
    @Operation(summary = "폴더명 수정", description = "지정된 특정 폴더의 이름을 변경합니다.")
    @org.springframework.web.bind.annotation.PatchMapping("/{folder_id}")
    public ResponseEntity<ApiResponse<com.After_Buy.DeviceService.dto.response.FolderCreateResponse>> updateFolderName(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @org.springframework.web.bind.annotation.PathVariable("folder_id") Long folderId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.After_Buy.DeviceService.dto.request.FolderUpdateNameRequest request) {
        log.info("폴더명 수정 요청: userId={}, folderId={}, newName={}", userPrincipal.getUserId(), folderId, request.getFolderName());
        
        com.After_Buy.DeviceService.dto.response.FolderCreateResponse response = folderService.updateFolderName(userPrincipal.getUserId(), folderId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 폴더 삭제 API
     * 특정 폴더를 영구적으로 삭제합니다.
     * 폴더 삭제 시 내부에 속해 있는 하위 폴더 및 기기도 데이터베이스 CASCADE 정책에 의해 모두 삭제됩니다.
     *
     * @param userPrincipal : 인증된 사용자
     * @param folderId      : 삭제 대상 폴더 ID
     * @return : 200 OK + 단순 메시지 응답
     * @since : 2026.04.09
     * @author : 최준혁
     */
    @Operation(summary = "폴더 삭제", description = "명시된 폴더와 해당 폴더 내부의 모든 하위 폴더/기기들을 연쇄 삭제합니다.")
    @org.springframework.web.bind.annotation.DeleteMapping("/{folder_id}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @org.springframework.web.bind.annotation.PathVariable("folder_id") Long folderId) {
        log.info("폴더 삭제 요청: userId={}, folderId={}", userPrincipal.getUserId(), folderId);
        
        folderService.deleteFolder(userPrincipal.getUserId(), folderId);
        
        return ResponseEntity.ok(ApiResponse.successMessage("폴더 및 내부 항목이 모두 삭제되었습니다."));
    }
}




