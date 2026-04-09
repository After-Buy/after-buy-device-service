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

    /**
     * 특정 폴더 내부 아이템 목록 조회 및 Breadcrumb 생성
     *
     * @param userId   : 조회 요청을 한 사용자 ID
     * @param folderId : 현재 진입한 폴더 ID
     * @return : FolderItemsResponse (현재 폴더 정보, 하위 폴더, 내부 기기, Breadcrumb 경로)
     */
    @Transactional(readOnly = true)
    public com.After_Buy.DeviceService.dto.response.FolderItemsResponse getFolderItems(Long userId, Long folderId) {
        // 1. 현재 폴더 가져오기 및 소유권 검증
        com.After_Buy.DeviceService.entity.Folder currentFolder = folderRepository.findByFolderIdAndUserId(folderId, userId)
                .orElseThrow(() -> new com.After_Buy.DeviceService.exception.CustomException(com.After_Buy.DeviceService.exception.ErrorCode.DEVICE_FOLDER_NOT_FOUND));

        // 2. 현재 폴더 DTO 매핑
        Long childCount = folderRepository.countByParentFolderId(folderId) + deviceRepository.countByFolderId(folderId);
        FolderDto currentFolderDto = FolderDto.builder()
                .folderId(currentFolder.getFolderId())
                .folderName(currentFolder.getFolderName())
                .parentFolderId(currentFolder.getParentFolderId())
                .childCount(childCount)
                .createdAt(currentFolder.getCreatedAt())
                .updatedAt(currentFolder.getUpdatedAt())
                .build();

        // 3. Breadcrumb 경로 생성 (현재 위치 위로 역추적)
        java.util.List<com.After_Buy.DeviceService.dto.response.BreadcrumbDto> breadcrumb = new java.util.ArrayList<>();
        com.After_Buy.DeviceService.entity.Folder temp = currentFolder;
        while (temp != null) {
            breadcrumb.add(0, new com.After_Buy.DeviceService.dto.response.BreadcrumbDto(temp.getFolderId(), temp.getFolderName()));
            if (temp.getParentFolderId() == null) {
                break;
            }
            temp = folderRepository.findById(temp.getParentFolderId()).orElse(null);
        }

        // 4. 직속 하위 폴더 목록 조회 (각 child_count 포함)
        List<FolderDto> subFolders = folderRepository.findSubFoldersWithChildCount(folderId, userId);

        // 5. 현재 폴더에 소속된 기기 목록 반환
        List<Device> deviceEntities = deviceRepository.findByUserIdAndFolderIdOrderByCreatedAtDesc(userId, folderId);
        List<DeviceListItemDto> devices = deviceEntities.stream()
                .map(DeviceListItemDto::from)
                .collect(Collectors.toList());

        return com.After_Buy.DeviceService.dto.response.FolderItemsResponse.of(currentFolderDto, breadcrumb, subFolders, devices);
    }

    /**
     * 폴더 생성 기능
     * 부모 폴더가 지정된 경우, 부모 폴더의 소유권 및 존재 여부를 검사하고 하위로 생성합니다.
     *
     * @param userId  : 폴더를 생성하려는 유저 ID
     * @param request : 바디로 넘겨받은 폴더 생성 요청 속성(이름, 부모 폴더 ID)
     * @return : 생성 완료된 폴더 객체 반환
     */
    @org.springframework.transaction.annotation.Transactional
    public com.After_Buy.DeviceService.dto.response.FolderCreateResponse createFolder(Long userId, com.After_Buy.DeviceService.dto.request.FolderCreateRequest request) {
        if (request.getParentFolderId() != null) {
            folderRepository.findByFolderIdAndUserId(request.getParentFolderId(), userId)
                    .orElseThrow(() -> new com.After_Buy.DeviceService.exception.CustomException(com.After_Buy.DeviceService.exception.ErrorCode.PARENT_FOLDER_NOT_FOUND));
        }

        com.After_Buy.DeviceService.entity.Folder folder = com.After_Buy.DeviceService.entity.Folder.builder()
                .userId(userId)
                .folderName(request.getFolderName())
                .parentFolderId(request.getParentFolderId())
                .build();

        com.After_Buy.DeviceService.entity.Folder savedFolder = folderRepository.save(folder);
        return com.After_Buy.DeviceService.dto.response.FolderCreateResponse.from(savedFolder);
    }
}


