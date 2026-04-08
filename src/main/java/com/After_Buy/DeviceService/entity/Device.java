package com.After_Buy.DeviceService.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 기기 엔티티
 * 사용자가 등록한 전자기기의 모든 정보를 관리하는 JPA 엔티티입니다.
 * 네이버 쇼핑 API·OCR·사용자 직접 입력 데이터를 저장하며,
 * folder_id = NULL은 미분류 상태를 의미합니다.
 * DB: device_db.devices
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "devices")
public class Device {

	// 기기 고유 ID (AUTO_INCREMENT)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "device_id")
	private Long deviceId;

	// 소유 사용자 ID (auth_db.users 논리 참조)
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// 소속 폴더 ID (NULL = 미분류, folders.folder_id FK)
	@Column(name = "folder_id")
	private Long folderId;

	// 상품명 (필수, 최대 200자)
	@Column(name = "product_name", nullable = false, length = 200)
	private String productName;

	// 모델명 (필수, 최대 100자, OCR 또는 직접 입력)
	@Column(name = "model_name", nullable = false, length = 100)
	private String modelName;

	// 브랜드명 (필수, 최대 100자)
	@Column(name = "brand", nullable = false, length = 100)
	private String brand;

	// 기기 이미지 S3 URL (선택, 최대 500자)
	@Column(name = "image_url", length = 500)
	private String imageUrl;

	// 공식 제품 페이지 URL (선택, 네이버 쇼핑 API 연동, 최대 500자)
	@Column(name = "product_link_url", length = 500)
	private String productLinkUrl;

	// 구매일 (필수, 영수증 OCR 또는 직접 입력)
	@Column(name = "purchase_date", nullable = false)
	private LocalDate purchaseDate;

	// 구매 가격 (필수, 최대 DECIMAL(12,2))
	@Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal purchasePrice;

	// 구매처 (선택, 최대 100자)
	@Column(name = "purchase_store", length = 100)
	private String purchaseStore;

	// 무상 보증 기간 개월 수 (필수, INT UNSIGNED)
	@Column(name = "warranty_months", nullable = false)
	private Integer warrantyMonths;

	/*
	 * 보증 만료일 (필수, purchase_date + warranty_months 자동 계산 후 저장)
	 * 서버에서 자동 계산하므로 클라이언트로부터 받지 않습니다.
	 */
	@Column(name = "warranty_expiry_date", nullable = false)
	private LocalDate warrantyExpiryDate;

	// 시리얼 넘버 (선택, S/N, OCR 또는 직접 입력, 최대 100자)
	@Column(name = "serial_number", length = 100)
	private String serialNumber;

	// 사용자 메모 (선택, TEXT)
	@Column(name = "memo", columnDefinition = "TEXT")
	private String memo;

	// 등록 일시 (자동 설정)
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// 마지막 수정 일시 (자동 업데이트)
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * 기기 등록 Builder 생성자
	 * Service 계층에서 등록 시 사용하며, warrantyExpiryDate는 Service에서 자동 계산하여 주입합니다.
	 *
	 * @param userId             : 소유 사용자 ID
	 * @param folderId           : 소속 폴더 ID (null = 미분류)
	 * @param productName        : 상품명
	 * @param modelName          : 모델명
	 * @param brand              : 브랜드명
	 * @param imageUrl           : 이미지 URL
	 * @param productLinkUrl     : 제품 링크 URL
	 * @param purchaseDate       : 구매일
	 * @param purchasePrice      : 구매 가격
	 * @param purchaseStore      : 구매처
	 * @param warrantyMonths     : 보증 기간(개월)
	 * @param warrantyExpiryDate : 보증 만료일 (자동 계산)
	 * @param serialNumber       : 시리얼 넘버
	 * @param memo               : 메모
	 */
	@Builder
	public Device(Long userId, Long folderId, String productName, String modelName, String brand,
			String imageUrl, String productLinkUrl, LocalDate purchaseDate, BigDecimal purchasePrice,
			String purchaseStore, Integer warrantyMonths, LocalDate warrantyExpiryDate,
			String serialNumber, String memo) {
		this.userId = userId;
		this.folderId = folderId;
		this.productName = productName;
		this.modelName = modelName;
		this.brand = brand;
		this.imageUrl = imageUrl;
		this.productLinkUrl = productLinkUrl;
		this.purchaseDate = purchaseDate;
		this.purchasePrice = purchasePrice;
		this.purchaseStore = purchaseStore;
		this.warrantyMonths = warrantyMonths;
		this.warrantyExpiryDate = warrantyExpiryDate;
		this.serialNumber = serialNumber;
		this.memo = memo;
	}

	/**
	 * 기기 정보 전체 수정 (model_name 제외)
	 * API 명세서 요구사항에 따라 warranty_months 또는 purchase_date 변경 시
	 * warranty_expiry_date가 자동 재계산됩니다.
	 *
	 * @param folderId       : 소속 폴더 ID (미분류 시 null)
	 * @param productName    : 상품명
	 * @param brand          : 브랜드명
	 * @param imageUrl       : 기기 이미지 URL
	 * @param productLinkUrl : 공식 제품 페이지 URL
	 * @param purchaseDate   : 구매일
	 * @param purchasePrice  : 구매 가격
	 * @param purchaseStore  : 구매처
	 * @param warrantyMonths : 무상 보증 기간 개월 수
	 * @param serialNumber   : 시리얼 넘버
	 * @param memo           : 사용자 메모
	 * @since : 2026.04.08
	 * @author : 최준혁
	 */
	public void update(Long folderId, String productName, String brand, String imageUrl,
			String productLinkUrl, LocalDate purchaseDate, BigDecimal purchasePrice,
			String purchaseStore, Integer warrantyMonths, String serialNumber, String memo) {
		this.folderId = folderId;
		this.productName = productName;
		this.brand = brand;
		this.imageUrl = imageUrl;
		this.productLinkUrl = productLinkUrl;
		this.purchaseDate = purchaseDate;
		this.purchasePrice = purchasePrice;
		this.purchaseStore = purchaseStore;
		this.warrantyMonths = warrantyMonths;
		this.serialNumber = serialNumber;
		this.memo = memo;

		/* warranty_expiry_date 자동 갱신: 구매일 + 보증기간(개월) */
		this.warrantyExpiryDate = this.purchaseDate.plusMonths(this.warrantyMonths);
	}

	/**
	 * 기기 상품명 단일 수정
	 * PATCH /api/devices/{device_id}/name 용
	 *
	 * @param productName : 변경할 상품명
	 * @since : 2026.04.08
	 * @author : 최준혁
	 */
	public void updateName(String productName) {
		this.productName = productName;
	}
}