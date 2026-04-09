package com.After_Buy.DeviceService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 폴더 엔티티
 * 사용자가 기기를 분류하기 위해 만드는 폴더를 관리하는 JPA 엔티티입니다.
 * parent_folder_id 자기참조 구조로 계층형 폴더 구조를 구현합니다.
 * DB: device_db.folders
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "folders")
public class Folder {

	// 폴더 고유 ID (AUTO_INCREMENT)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "folder_id")
	private Long folderId;

	// 폴더 소유 사용자 ID (auth_db.users 논리 참조)
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// 상위 폴더 ID (NULL = 루트 폴더)
	@Column(name = "parent_folder_id")
	private Long parentFolderId;

	// 폴더명 (최대 100자)
	@Column(name = "folder_name", nullable = false, length = 100)
	private String folderName;

	// 생성 일시 (자동 설정)
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 마지막 수정 일시 (자동 업데이트)
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
