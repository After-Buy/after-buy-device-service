package com.After_Buy.DeviceService.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공통 에러 응답 DTO
 * API 명세서(예외 처리 방법.md)에서 정의한 표준 에러 응답 JSON 형식을 구현합니다.
 * 성공 응답은 ApiResponse로 처리하고, 에러 응답은 이 클래스를 사용합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

	// 에러 발생 서버 시각
	private final String timestamp;

	// HTTP 상태 코드 (400, 401, 403, 404, 500 등)
	private final int status;

	// 팀 내 약속된 비즈니스 예외 코드 (예: DEVICE-001, COMMON-001)
	private final String code;

	// 클라이언트에게 전달할 한글 에러 안내 메시지
	private final String message;

	// @Valid 유효성 검사 실패 시 필드별 에러 상세 정보 (일반 비즈니스 에러 시 빈 배열)
	private final List<FieldError> errors;

	// 에러가 발생한 API 엔드포인트 경로
	private final String path;

	private ErrorResponse(int status, String code, String message, List<FieldError> errors, String path) {
		this.timestamp = LocalDateTime.now().toString();
		this.status = status;
		this.code = code;
		this.message = message;
		this.errors = errors;
		this.path = path;
	}

	/**
	 * 비즈니스 예외(CustomException) 발생 시 에러 응답 생성
	 * errors 필드는 빈 배열로 반환합니다.
	 *
	 * @param status  : HTTP 상태 코드
	 * @param code    : 비즈니스 예외 코드
	 * @param message : 에러 메시지
	 * @param path    : 요청 경로
	 * @return : 표준 에러 응답 객체
	 */
	public static ErrorResponse of(int status, String code, String message, String path) {
		return new ErrorResponse(status, code, message, List.of(), path);
	}

	/**
	 * @Valid 유효성 검사 실패 시 에러 응답 생성
	 * errors 필드에 필드별 상세 에러 정보를 담습니다.
	 *
	 * @param status  : HTTP 상태 코드
	 * @param code    : 비즈니스 예외 코드
	 * @param message : 에러 메시지
	 * @param errors  : 필드별 에러 상세 정보 목록
	 * @param path    : 요청 경로
	 * @return : 필드 에러가 포함된 표준 에러 응답 객체
	 */
	public static ErrorResponse of(int status, String code, String message, List<FieldError> errors, String path) {
		return new ErrorResponse(status, code, message, errors, path);
	}

	/**
	 * @Valid 유효성 검사 실패 필드별 에러 상세 정보 이너 클래스
	 */
	@Getter
	public static class FieldError {
		// 유효성 검사에 실패한 필드명
		private final String field;

		// 실패한 필드에 입력된 실제 값
		private final String value;

		// 실패 사유 메시지
		private final String reason;

		public FieldError(String field, String value, String reason) {
			this.field = field;
			this.value = value;
			this.reason = reason;
		}
	}
}
