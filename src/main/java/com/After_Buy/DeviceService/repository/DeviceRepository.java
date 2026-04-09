package com.After_Buy.DeviceService.repository;

import com.After_Buy.DeviceService.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 기기 레포지토리
 * devices 테이블에 대한 JPA 데이터 액세스 레이어입니다.
 * 기기 CRUD 전반에 걸쳐 사용되며, 현재는 기기 등록(save)을 위한 기본 메서드를 제공합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
public interface DeviceRepository extends JpaRepository<Device, Long> {

    // 사용자의 전체 기기 개수 조회
    Long countByUserId(Long userId);

    // 사용자의 등록 기기 가격들(가치)의 총합 조회
    @Query("SELECT COALESCE(SUM(d.purchasePrice), 0) FROM Device d WHERE d.userId = :userId")
    BigDecimal sumPurchasePriceByUserId(@Param("userId") Long userId);

    // 보증 만료 기간이 다가오는(오늘부터 end_date 사이) 기기 개수 조회
    @Query("SELECT COUNT(d) FROM Device d WHERE d.userId = :userId AND d.warrantyExpiryDate BETWEEN CURRENT_DATE AND :endDate")
    Long countExpiringSoonByUserId(@Param("userId") Long userId, @Param("endDate") LocalDate endDate);

    // 사용자의 가장 최근에 등록된 기기 최대 3대 조회
    List<Device> findTop3ByUserIdOrderByCreatedAtDesc(Long userId);

    // 현재 기점 가장 만료일이 근접한 1개의 기기 조회 (Native Query 이용)
    @Query(value = "SELECT * FROM devices WHERE user_id = :userId ORDER BY ABS(DATEDIFF(warranty_expiry_date, CURRENT_DATE)) ASC LIMIT 1", nativeQuery = true)
    Optional<Device> findTopByUserIdOrderByWarrantyExpiryDateClosest(@Param("userId") Long userId);

    // 미분류 기기 목록 조회 (최신 등록순)
    List<Device> findByUserIdAndFolderIdIsNullOrderByCreatedAtDesc(Long userId);

    // 미분류 기기 목록 조회 (보증 만료 임박순)
    List<Device> findByUserIdAndFolderIdIsNullOrderByWarrantyExpiryDateAsc(Long userId);

    // 특정 폴더 내부의 분류된 기기 목록 반환 (최신순)
    List<Device> findByUserIdAndFolderIdOrderByCreatedAtDesc(Long userId, Long folderId);

    // 특정 폴더 내부에 포함된 기기의 개수 계산
    Long countByFolderId(Long folderId);

    // 특정 폴더 내부에 포함된 모든 기기를 일괄 삭제 (애플리케이션 레벨 CASCADE 용)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Device d WHERE d.folderId = :folderId")
    void deleteByFolderId(@org.springframework.data.repository.query.Param("folderId") Long folderId);
}
