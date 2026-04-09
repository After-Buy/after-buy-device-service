package com.After_Buy.DeviceService.repository;

import com.After_Buy.DeviceService.dto.response.FolderDto;
import com.After_Buy.DeviceService.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

/**
 * 폴더 레포지토리
 * folders 테이블에 대한 JPA 데이터 액세스 레이어입니다.
 * 기기 등록 시 folder_id 유효성 및 소유권 검증에 사용됩니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
public interface FolderRepository extends JpaRepository<Folder, Long> {

	/**
	 * 폴더 ID와 사용자 ID로 폴더 조회
	 * 기기 등록 시 folder_id가 존재하는지, 그리고 현재 사용자 소유인지를 동시에 검증합니다.
	 * (API 명세서: "folder_id가 존재하지 않거나 본인 폴더가 아닌 경우 → DEVICE-001, 404")
	 *
	 * @param folderId : 검증할 폴더의 고유 ID
	 * @param userId   : 현재 인증된 사용자 ID
	 * @return : 조건에 맞는 폴더 Optional (없으면 empty)
	 */
	Optional<Folder> findByFolderIdAndUserId(Long folderId, Long userId);

	/**
	 * 사용자 ID 기반 루트 폴더(최상위) 목록 조회 및 직속 하위 항목 수 계산
	 * 
	 * @param userId : 조회할 폴더의 소유자 ID
	 * @return : 루트 폴더와 그 자식 개수가 포함된 DTO 목록 반환
	 */
	@Query("SELECT new com.After_Buy.DeviceService.dto.response.FolderDto(" +
			"f.folderId, f.folderName, f.parentFolderId, " +
			"(COALESCE((SELECT COUNT(subF) FROM Folder subF WHERE subF.parentFolderId = f.folderId), 0L) + " +
			"COALESCE((SELECT COUNT(d) FROM Device d WHERE d.folderId = f.folderId), 0L)), " +
			"f.createdAt, f.updatedAt) " +
			"FROM Folder f " +
			"WHERE f.userId = :userId AND f.parentFolderId IS NULL " +
			"ORDER BY f.createdAt DESC")
	List<FolderDto> findRootFoldersWithChildCount(@Param("userId") Long userId);

}
