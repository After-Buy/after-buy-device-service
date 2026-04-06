package com.After_Buy.DeviceService.repository;

import com.After_Buy.DeviceService.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

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
}
