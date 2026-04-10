package com.After_Buy.DeviceService.controller;

import com.After_Buy.DeviceService.dto.response.ApiResponse;
import com.After_Buy.DeviceService.dto.response.SearchResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통합 검색 컨트롤러
 * 폴더명, 상품명, 브랜드를 대상으로 하는 통합 검색 API를 제공합니다.
 * Base URL: /api/devices
 * 포트: 8082
 * 모든 엔드포인트는 JWT Bearer 토큰 인증 필수입니다.
 *
 * @since : 2026.04.10
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Search", description = "폴더 및 기기 통합 검색 API")
public class SearchController {

    private final FolderService folderService;

    /**
     * 폴더/기기 통합 검색 API
     * folder_name, product_name, brand를 대상으로 검색어가 포함된 항목을 반환합니다.
     * 검색 결과가 없으면 빈 배열([])을 반환합니다.
     *
     * @param userPrincipal : 인증된 사용자
     * @param q             : 검색어 (빈 문자열이면 빈 결과 반환)
     * @return : 200 OK + { folders: [...], devices: [...] }
     * @since : 2026.04.10
     * @author : 최준혁
     */
    @Operation(
        summary = "폴더/기기 통합 검색",
        description = "검색어로 폴더명·상품명·브랜드를 검색합니다. 결과 없을 시 빈 배열 반환."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(name = "q", required = false, defaultValue = "") String q) {

        log.info("통합 검색 요청: userId={}, keyword={}", userPrincipal.getUserId(), q);

        SearchResponse result = folderService.search(userPrincipal.getUserId(), q);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
