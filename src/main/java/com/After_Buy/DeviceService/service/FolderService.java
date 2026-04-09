package com.After_Buy.DeviceService.service;

import com.After_Buy.DeviceService.dto.response.DeviceListItemDto;
import com.After_Buy.DeviceService.dto.response.FolderDto;
import com.After_Buy.DeviceService.dto.response.RootFolderListResponse;
import com.After_Buy.DeviceService.entity.Device;
import com.After_Buy.DeviceService.repository.DeviceRepository;
import com.After_Buy.DeviceService.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 폴더 비즈니스 로직 서비스
 * 폴더 관리와 관련된 핵심 비즈니스 로직을 처리합니다.
 *
 * @since : 2026.04.09
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 루트 폴더 목록 및 미분류 기기 조회
     * parent_folder_id IS NULL인 최상위(루트) 폴더 목록과 직속 하위 항목 수를 반환합니다.
     * folder_id IS NULL인 미분류 기기 목록도 함께 반환합니다.
     *
     * @param userId : 조회 요청을 한 사용자 ID (JWT)
     * @return : RootFolderListResponse (루트 폴더 목록과 미분류 기기 목록)
     */
    @Transactional(readOnly = true)
    public RootFolderListResponse getRootFolders(Long userId) {
        // 1. 루트 폴더 목록(+하위 항목 수 포함) 조회
        List<FolderDto> rootFolders = folderRepository.findRootFoldersWithChildCount(userId);

        // 2. 미분류 기기 목록(folder_id IS NULL) 조회
        List<Device> unclassifiedDeviceEntities = deviceRepository.findByUserIdAndFolderIdIsNullOrderByCreatedAtDesc(userId);
        
        List<DeviceListItemDto> unclassifiedDevices = unclassifiedDeviceEntities.stream()
                .map(DeviceListItemDto::from)
                .collect(Collectors.toList());

        return RootFolderListResponse.of(rootFolders, unclassifiedDevices);
    }
}
