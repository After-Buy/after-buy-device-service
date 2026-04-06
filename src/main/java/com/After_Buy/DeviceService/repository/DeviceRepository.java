package com.After_Buy.DeviceService.repository;

import com.After_Buy.DeviceService.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
