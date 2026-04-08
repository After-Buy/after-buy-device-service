package com.After_Buy.DeviceService.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역 에러 코드 Enum
 * 에러 발생 시 클라이언트에게 내려줄 HTTP 상태 코드, 커스텀 코드, 메시지를 관리합니다.
 * AdminService의 ErrorCode.java와 동일한 패턴을 사용합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	/* ===== 기기 관련 (DEVICE-0XX) ===== */

	/** 폴더가 존재하지 않거나 본인 소유가 아닌 경우 */
	DEVICE_FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "DEVICE-001", "존재하지 않거나 접근 권한이 없는 폴더입니다."),

	/** 본인의 기기가 아닌 경우 (403 Forbidden) */
	DEVICE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DEVICE-002", "본인의 기기에만 접근할 수 있습니다."),

	/** 기기가 존재하지 않는 경우 (404 Not Found) */
	DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "DEVICE-003", "존재하지 않는 기기입니다."),

	/* ===== 검색 대행 관련 (SEARCH-0XX) ===== */

	/** 네이버 쇼핑 API 검색 결과 없음 */
	SEARCH_NO_RESULT(HttpStatus.NOT_FOUND, "SEARCH-001", "검색 결과가 없습니다. 직접 입력해주세요."),

	/* ===== OCR 관련 (OCR-0XX) ===== */

	/** OCR 텍스트 인식 실패 */
	OCR_RECOGNITION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "OCR-001", "이미지에서 텍스트를 인식하지 못했습니다."),

	/* ===== 공통 (COMMON-0XX) ===== */

	/** @Valid 유효성 검사 실패 */
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다."),

	/** 인증 실패 (JWT 토큰 없음 또는 만료) */
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-401", "인증이 필요합니다."),

	/** 서버 내부 오류 */
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다.");

	/** HTTP 상태 코드 */
	private final HttpStatus httpStatus;

	/** 프론트엔드 분기용 커스텀 에러 코드 */
	private final String code;

	/** 클라이언트에게 전달할 에러 메시지 */
	private final String message;
}
