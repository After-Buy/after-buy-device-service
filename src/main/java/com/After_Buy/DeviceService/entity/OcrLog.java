package com.After_Buy.DeviceService.entity;

import com.After_Buy.DeviceService.entity.enums.OcrType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * OCR 처리 로그 엔티티
 * 사용자가 OCR을 호출할 때마다 유형, 성공 여부, 수정 필드를 기록합니다.
 * DB: device_db.ocr_logs
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "ocr_logs")
public class OcrLog {

	// OCR 로그 고유 ID (AUTO_INCREMENT)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ocr_log_id")
	private Long ocrLogId;

	// OCR 요청 사용자 ID (auth_db.users 논리 참조)
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// 연결된 기기 ID (등록/수정 후 modified-fields 호출 시 업데이트)
	@Column(name = "device_id")
	private Long deviceId;

	// OCR 처리 유형 (MODEL, SERIAL, RECEIPT)
	@Enumerated(EnumType.STRING)
	@Column(name = "ocr_type", nullable = false, length = 10)
	private OcrType ocrType;

	// OCR 인식 성공 여부
	@Column(name = "is_success", nullable = false)
	private Boolean isSuccess;

	// 사용자가 수정한 필드명 목록 (JSON 배열 형식으로 저장, 예: ["purchase_date", "purchase_price"])
	@Column(name = "modified_fields", columnDefinition = "JSON")
	private String modifiedFields;

	// 생성 시각 (자동 설정)
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * OCR 최초 호출 시 로그 생성용 빌더
	 *
	 * @param userId    : 요청 사용자 ID
	 * @param ocrType   : OCR 유형
	 * @param isSuccess : 인식 성공 여부
	 */
	@Builder
	public OcrLog(Long userId, OcrType ocrType, Boolean isSuccess) {
		this.userId = userId;
		this.ocrType = ocrType;
		this.isSuccess = isSuccess;
	}

	/**
	 * 오인식 필드 기록 업데이트 메서드
	 * OCR 결과를 수정하여 기기를 저장한 후 호출합니다.
	 *
	 * @param deviceId       : 연결된 기기 ID
	 * @param modifiedFields : 수정된 필드 목록 (JSON 배열 문자열)
	 */
	public void updateModifiedFields(Long deviceId, String modifiedFields) {
		this.deviceId = deviceId;
		this.modifiedFields = modifiedFields;
	}
}
