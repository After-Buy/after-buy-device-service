package com.After_Buy.DeviceService.repository;

import com.After_Buy.DeviceService.entity.OcrLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * OCR 로그 레포지토리
 * ocr_logs 테이블에 대한 JPA 쿼리 인터페이스를 제공합니다.
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
public interface OcrLogRepository extends JpaRepository<OcrLog, Long> {

    /**
     * 특정 사용자 OCR 로그 일괄 삭제 (벌크 연산)
     */
    @Modifying
    @Query("DELETE FROM OcrLog o WHERE o.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * 특정 기간 동안의 전체 OCR 시도 횟수
     */
    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * 특정 기간 동안의 OCR 실패 횟수
     */
    long countByIsSuccessFalseAndCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * 특정 기간 내 지정된 필드가 배열로 저장되어 있는(유저가 수정한) 레코드들의 modifiedFields 문자열 리스트 반환
     */
    @Query("SELECT o.modifiedFields FROM OcrLog o WHERE o.modifiedFields IS NOT NULL AND o.createdAt >= :start AND o.createdAt <= :end")
    java.util.List<String> findModifiedFieldsByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    /**
     * 특정 기간 내의 일별 실패 트렌드 집계
     */
    @Query(value = "SELECT DATE(o.created_at) as date, COUNT(*) as failureCount " +
                   "FROM ocr_logs o " +
                   "WHERE o.is_success = false AND o.created_at >= :start AND o.created_at <= :end " +
                   "GROUP BY DATE(o.created_at) " +
                   "ORDER BY date", nativeQuery = true)
    java.util.List<Object[]> findDailyFailureTrendRaw(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    /**
     * 특정 기간 내의 일별 OCR 결과 집계
     */
    @Query(value = "SELECT DATE(o.created_at) as date, " +
                   "COUNT(*) as totalAttempts, " +
                   "SUM(CASE WHEN o.is_success = true THEN 1 ELSE 0 END) as successCount, " +
                   "SUM(CASE WHEN o.modified_fields IS NOT NULL AND JSON_LENGTH(o.modified_fields) > 0 THEN 1 ELSE 0 END) as modifiedCount, " +
                   "SUM(CASE WHEN o.is_success = false THEN 1 ELSE 0 END) as failureCount " +
                   "FROM ocr_logs o " +
                   "WHERE o.created_at >= :start AND o.created_at <= :end " +
                   "GROUP BY DATE(o.created_at) " +
                   "ORDER BY date", nativeQuery = true)
    java.util.List<Object[]> findDailyOcrResultTrendRaw(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    /**
     * 특정 기간 내의 OCR 유형별 실패 횟수 집계
     */
    @Query(value = "SELECT o.ocr_type as fieldName, COUNT(*) as failureCount " +
                   "FROM ocr_logs o " +
                   "WHERE o.is_success = false AND o.created_at >= :start AND o.created_at <= :end " +
                   "GROUP BY o.ocr_type " +
                   "ORDER BY failureCount DESC", nativeQuery = true)
    java.util.List<Object[]> findFieldFailureStatsRaw(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
