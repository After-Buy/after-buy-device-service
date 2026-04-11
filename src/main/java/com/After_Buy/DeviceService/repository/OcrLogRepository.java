package com.After_Buy.DeviceService.repository;

import com.After_Buy.DeviceService.entity.OcrLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * OCR 로그 레포지토리
 * ocr_logs 테이블에 대한 JPA 쿼리 인터페이스를 제공합니다.
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
public interface OcrLogRepository extends JpaRepository<OcrLog, Long> {
}
