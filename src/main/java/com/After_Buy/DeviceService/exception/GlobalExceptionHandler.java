package com.After_Buy.DeviceService.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AOP 기반 스프링 최상위 통합 시스템 오류 관제 매니저
 * 컨트롤러와 서비스 전역에서 발생하는 예외를 흡수하고
 * API 명세서(예외 처리 방법.md)에 정의된 표준 ErrorResponse 포맷으로 변환해 반환합니다.
 * AdminService의 GlobalExceptionHandler와 동일한 패턴을 사용합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 커스텀 도메인 특화 비즈니스 예외 핸들링
	 * Service에서 throw new CustomException(ErrorCode.XXX)으로 발생시킨 예외를 처리합니다.
	 *
	 * @param e       발생한 CustomException
	 * @param request 현재 HTTP 요청 (path 추출용)
	 * @return 표준 에러 응답 JSON
	 */
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletRequest request) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("[CustomException] code={}, message={}, path={}",
				errorCode.getCode(), errorCode.getMessage(), request.getRequestURI());

		ErrorResponse errorResponse = ErrorResponse.of(
				errorCode.getHttpStatus().value(),
				errorCode.getCode(),
				errorCode.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
	}

	/**
	 * @Valid 유효성 검사 실패 핸들링
	 * DTO 어노테이션 기반 파라미터 검증 실패 시 각 필드의 field, value, reason을 errors 배열에 담아 반환합니다.
	 *
	 * @param e       @Valid 유효성 검사 실패 시 스프링이 발생시키는 예외 객체
	 * @param request 현재 HTTP 요청 (path 추출용)
	 * @return 필드별 유효성 에러가 포함된 400 BAD REQUEST 표준 ErrorResponse
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e,
			HttpServletRequest request) {
		BindingResult bindingResult = e.getBindingResult();

		/* 모든 필드 에러를 순회하여 field, value, reason을 추출 후 FieldError 목록으로 변환 */
		List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
				.map(fe -> new ErrorResponse.FieldError(
						fe.getField(),
						fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : "",
						fe.getDefaultMessage()
				))
				.collect(Collectors.toList());

		log.warn("[ValidationException] path={}, errors={}", request.getRequestURI(), fieldErrors);

		ErrorResponse errorResponse = ErrorResponse.of(
				ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value(),
				ErrorCode.INVALID_INPUT_VALUE.getCode(),
				ErrorCode.INVALID_INPUT_VALUE.getMessage(),
				fieldErrors,
				request.getRequestURI()
		);
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * 알 수 없는 서버 내부 예외 핸들링 (최종 디펜스 라인)
	 * 예상치 못한 500급 예외를 처리합니다. 내부 구조 노출을 막고 500 응답으로 변환합니다.
	 *
	 * @param e       컨트롤러 통제 밖까지 도달한 예외
	 * @param request 현재 HTTP 요청 (path 추출용)
	 * @return 500 INTERNAL SERVER ERROR 응답
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
		log.error("[UnhandledException] path={}, message={}", request.getRequestURI(), e.getMessage(), e);

		ErrorResponse errorResponse = ErrorResponse.of(
				ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value(),
				ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
				ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.internalServerError().body(errorResponse);
	}
}
