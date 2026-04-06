package com.After_Buy.DeviceService.exception;

import lombok.Getter;

/**
 * 마이크로 서비스 도메인 정책 전용 예외 클래스
 * 비즈니스 로직에서 의도적으로 발생시키는 예외입니다.
 * ErrorCode Enum을 담아 GlobalExceptionHandler에서 표준 에러 응답으로 변환합니다.
 * AdminService의 CustomException.java와 동일한 패턴을 사용합니다.
 *
 * 사용 예시: throw new CustomException(ErrorCode.DEVICE_FOLDER_NOT_FOUND);
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
public class CustomException extends RuntimeException {

	/** 에러 상태 코드, 커스텀 코드, 메시지를 담은 Enum */
	private final ErrorCode errorCode;

	public CustomException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
