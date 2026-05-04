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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.After_Buy.DeviceService.dto.response.FolderItemsResponse;
import com.After_Buy.DeviceService.dto.response.BreadcrumbDto;
import com.After_Buy.DeviceService.dto.response.FolderCreateResponse;
import com.After_Buy.DeviceService.dto.request.FolderCreateRequest;
import com.After_Buy.DeviceService.dto.request.FolderUpdateNameRequest;
import com.After_Buy.DeviceService.dto.request.BulkMoveRequest;
import com.After_Buy.DeviceService.dto.request.BulkDeleteRequest;
import com.After_Buy.DeviceService.dto.response.SearchResponse;
import com.After_Buy.DeviceService.dto.response.SearchFolderResultDto;
import com.After_Buy.DeviceService.dto.response.SearchDeviceResultDto;
import com.After_Buy.DeviceService.entity.Folder;
import com.After_Buy.DeviceService.exception.CustomException;
import com.After_Buy.DeviceService.exception.ErrorCode;

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
    public FolderItemsResponse getFolderItems(Long userId, Long folderId) {
        // 1. 현재 폴더 가져오기 (존재 확인 DEVICE-005)
        Folder currentFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOLDER_NOT_FOUND));

        // 소유권 검증 (권한 접근 에러 DEVICE-004)
        if (!currentFolder.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FOLDER_ACCESS_DENIED);
        }

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
        List<BreadcrumbDto> breadcrumb = new ArrayList<>();
        Folder temp = currentFolder;
        while (temp != null) {
            breadcrumb.add(0, new BreadcrumbDto(temp.getFolderId(), temp.getFolderName()));
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

        return FolderItemsResponse.of(currentFolderDto, breadcrumb, subFolders, devices);
    }

    /**
     * 폴더 생성 기능
     * 부모 폴더가 지정된 경우, 부모 폴더의 소유권 및 존재 여부를 검사하고 하위로 생성합니다.
     *
     * @param userId  : 폴더를 생성하려는 유저 ID
     * @param request : 바디로 넘겨받은 폴더 생성 요청 속성(이름, 부모 폴더 ID)
     * @return : 생성 완료된 폴더 객체 반환
     */
    @Transactional
    public FolderCreateResponse createFolder(Long userId, FolderCreateRequest request) {
        if (request.getParentFolderId() != null) {
            folderRepository.findByFolderIdAndUserId(request.getParentFolderId(), userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PARENT_FOLDER_NOT_FOUND));
        }

        Folder folder = Folder.builder()
                .userId(userId)
                .folderName(request.getFolderName())
                .parentFolderId(request.getParentFolderId())
                .build();

        Folder savedFolder = folderRepository.save(folder);
        return FolderCreateResponse.from(savedFolder);
    }

    /**
     * 폴더 이름 수정 로직
     * 폴더의 존재 유무(404) 및 사용자의 소유 권한(403)을 각각 분리하여 검증 후 이름을 변경합니다.
     *
     * @param userId   : 폴더를 수정하려는 유저 고유 ID
     * @param folderId : 수정 대상 폴더 ID
     * @param request  : 변경할 새로운 이름 정보
     * @return : 수정된 폴더의 응답용 DTO
     */
    @Transactional
    public FolderCreateResponse updateFolderName(Long userId, Long folderId, FolderUpdateNameRequest request) {
        // 1. 존재 여부 점검 (DEVICE-005)
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOLDER_NOT_FOUND));

        // 2. 소유권 점검 (DEVICE-004)
        if (!folder.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FOLDER_ACCESS_DENIED);
        }

        // 3. 엔티티 상태 변경 (스프링 더티체킹)
        folder.updateFolderName(request.getFolderName());
        
        return FolderCreateResponse.from(folder);
    }

    /**
     * 폴더 삭제 로직
     * DB의 ON DELETE CASCADE가 없을 경우(ddl-auto: update 환경 등)를 대비하여,
     * 애플리케이션 레벨에서 하위 폴더 및 기기들을 재귀적으로 먼저 삭제한 뒤 부모를 삭제합니다.
     *
     * @param userId   : 삭제 요청 유저 ID
     * @param folderId : 삭제 대상 폴더 ID
     */
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        // 1. 존재 여부 점검 (DEVICE-005)
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOLDER_NOT_FOUND));

        // 2. 소유권 점검 (DEVICE-004)
        if (!folder.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FOLDER_ACCESS_DENIED);
        }

        // 3. 애플리케이션 레벨 CASCADE: 하위 기기 및 폴더 재귀 삭제 진행
        deleteSubFoldersAndDevicesRecursively(folderId);

        // 4. 마지막으로 최상위 부모 요청 폴더 삭제
        folderRepository.delete(folder);
    }

    /**
     * 특정 폴더의 모든 하위 항목(기기 및 하위 폴더)들을 DFS 방식으로 일괄 삭제합니다.
     */
    private void deleteSubFoldersAndDevicesRecursively(Long currentFolderId) {
        // 현재 폴더에 속한 모든 내부 기기 삭제
        deviceRepository.deleteByFolderId(currentFolderId);

        // 직속 하위 폴더 ID 목록 추출
        List<Long> subFolderIds = folderRepository.findFolderIdsByParentFolderId(currentFolderId);

        // 자식 폴더들의 자식을 먼저 지우고(재귀) 본인을 지우는 Bottom-Up 삭제 수행
        for (Long subId : subFolderIds) {
            deleteSubFoldersAndDevicesRecursively(subId);
            folderRepository.deleteById(subId);
        }
    }

    /**
     * 다중 항목 일괄 이동 로직
     * 폴더와 기기들을 일괄 이동시킵니다.
     *
     * @param userId  : 이동을 요청하는 유저 ID
     * @param request : 이동 항목들의 리스트와 최종 도달 폴더 ID
     * @since : 2026.04.09
     * @author : 최준혁
     */
    @Transactional
    public void bulkMove(Long userId, BulkMoveRequest request) {
        List<Long> folderIds = request.getFolderIds();
        List<Long> deviceIds = request.getDeviceIds();

        boolean isFolderIdsEmpty = (folderIds == null || folderIds.isEmpty());
        boolean isDeviceIdsEmpty = (deviceIds == null || deviceIds.isEmpty());

        // 1. 빈 리스트 점검 (DEVICE-006)
        if (isFolderIdsEmpty && isDeviceIdsEmpty) {
            throw new CustomException(ErrorCode.BULK_ACTION_EMPTY_SELECTION);
        }

        // 2. 타겟 폴더 상태 및 모순 검증
        Long targetFolderId = request.getTargetFolderId();
        if (targetFolderId != null) {
            // 대상 폴더 존재 유무(DEVICE-005) 및 소유권 여부(DEVICE-004) 확인
            Folder targetFolder = folderRepository.findById(targetFolderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.FOLDER_NOT_FOUND));

            if (!targetFolder.getUserId().equals(userId)) {
                throw new CustomException(ErrorCode.FOLDER_ACCESS_DENIED);
            }

            // 폴더를 본인 스스로 포함하도록 이동 요청하는 루프 차단
            if (!isFolderIdsEmpty && folderIds.contains(targetFolderId)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        // 3. 폴더들 소유권 검수 및 부모 변경
        if (!isFolderIdsEmpty) {
            List<Folder> folders = folderRepository.findAllById(folderIds);
            for (Folder folder : folders) {
                if (!folder.getUserId().equals(userId)) {
                    throw new CustomException(ErrorCode.BULK_ACTION_ACCESS_DENIED);
                }
                folder.updateParentFolderId(targetFolderId);
            }
        }

        // 4. 기기들 소유권 검수 및 폴더 주소 변경
        if (!isDeviceIdsEmpty) {
            List<Device> devices = deviceRepository.findAllById(deviceIds);
            for (Device device : devices) {
                if (!device.getUserId().equals(userId)) {
                    throw new CustomException(ErrorCode.BULK_ACTION_ACCESS_DENIED);
                }
                device.updateFolderId(targetFolderId);
            }
        }
    }

    /**
     * 다중 항목 일괄 삭제 로직
     * 폴더와 기기들을 일괄 삭제시킵니다. 대상 폴더는 하위 구조까지 재귀적으로 삭제됩니다.
     *
     * @param userId  : 삭제를 요청하는 유저 ID
     * @param request : 삭제 항목들의 리스트
     * @since : 2026.04.09
     * @author : 최준혁
     */
    @Transactional
    public void bulkDelete(Long userId, BulkDeleteRequest request) {
        List<Long> folderIds = request.getFolderIds();
        List<Long> deviceIds = request.getDeviceIds();

        boolean isFolderIdsEmpty = (folderIds == null || folderIds.isEmpty());
        boolean isDeviceIdsEmpty = (deviceIds == null || deviceIds.isEmpty());

        if (isFolderIdsEmpty && isDeviceIdsEmpty) {
            throw new CustomException(ErrorCode.BULK_ACTION_EMPTY_SELECTION);
        }

        // 1. 기기 삭제 로직
        if (!isDeviceIdsEmpty) {
            List<Device> devices = deviceRepository.findAllById(deviceIds);
            for (Device device : devices) {
                if (!device.getUserId().equals(userId)) {
                    throw new CustomException(ErrorCode.BULK_ACTION_ACCESS_DENIED);
                }
            }
            deviceRepository.deleteAll(devices);
        }

        // 2. 폴더 하위 연쇄 삭제 로직
        if (!isFolderIdsEmpty) {
            List<Folder> folders = folderRepository.findAllById(folderIds);
            
            // 삭제 전 모든 폴더의 소유권 먼저 검증 (도중 실패하여 DB 정합성 꺾임 방지)
            for (Folder folder : folders) {
                if (!folder.getUserId().equals(userId)) {
                    throw new CustomException(ErrorCode.BULK_ACTION_ACCESS_DENIED);
                }
            }

            // 소유권 통과 시 개별 폴더에 대해 DFS 하위 연쇄 삭제 적용
            for (Folder folder : folders) {
                if (folderRepository.existsById(folder.getFolderId())) {
                    deleteSubFoldersAndDevicesRecursively(folder.getFolderId());
                    folderRepository.delete(folder);
                }
            }
        }
    }

    /**
     * 폴더 및 기기 통합 검색
     * 검색어(keyword)로 폴더명, 상품명, 브랜드를 부분 문자열(LIKE) 방식으로 검색합니다.
     * 검색어가 비어있으면 빈 결과를 반환합니다.
     *
     * @param userId  : 조회를 요청하는 사용자 ID
     * @param keyword : 검색어 (빈 문자열이면 빈 결과 반환)
     * @return : 폴더 검색 결과 + 기기 검색 결과를 담은 SearchResponse
     * @since : 2026.04.10
     * @author : 최준혁
     */
    @Transactional(readOnly = true)
    public SearchResponse search(Long userId, String keyword) {

        /* 검색어가 null이거나 공백인 경우 빈 결과 반환 (방안 B 적용) */
        if (keyword == null || keyword.trim().isEmpty()) {
            return new SearchResponse(new ArrayList<>(), new ArrayList<>());
        }

        /* 1. 폴더명 검색 (대소문자 무시) */
        List<Folder> matchedFolders = folderRepository.findByUserIdAndFolderNameContainingIgnoreCase(userId, keyword);
        List<SearchFolderResultDto> folderResults = matchedFolders.stream()
                .map(f -> new SearchFolderResultDto(f.getFolderId(), f.getFolderName(), "folder_name"))
                .collect(Collectors.toList());

        /* 2. 기기 상품명/브랜드 검색 */
        List<Device> matchedDevices = deviceRepository.searchByUserIdAndKeyword(userId, keyword);
        List<SearchDeviceResultDto> deviceResults = matchedDevices.stream()
                .map(d -> {
                    /* 미분류(folder_id = null)인 경우 folderName도 null 반환 */
                    String folderName = null;
                    if (d.getFolderId() != null) {
                        folderName = folderRepository.findById(d.getFolderId())
                                .map(Folder::getFolderName)
                                .orElse(null);
                    }
                    return new SearchDeviceResultDto(
                            d.getDeviceId(),
                            d.getProductName(),
                            d.getBrand(),
                            d.getImageUrl(),
                            d.getWarrantyExpiryDate(),
                            d.getFolderId(),
                            folderName
                    );
                })
                .collect(Collectors.toList());

        return new SearchResponse(folderResults, deviceResults);
    }
}
