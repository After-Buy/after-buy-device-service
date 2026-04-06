package com.After_Buy.DeviceService.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 공통 API 성공 응답 DTO
 * Device Service의 성공 응답을 {"success": true, "data": {...}} 형식으로 통일하는 래퍼 클래스입니다.
 * 에러 응답은 ErrorResponse 클래스를 사용합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	// 요청 성공 여부
	private final boolean success;

	// 응답 데이터 제네릭 (성공 시 존재, 실패 시 null)
	private final T data;

	// 성공 메시지 (데이터 없는 단순 성공 응답 시 사용)
	private final String message;

	private ApiResponse(boolean success, T data, String message) {
		this.success = success;
		this.data = data;
		this.message = message;
	}

	/**
	 * 성공 응답 생성 (데이터만 존재)
	 *
	 * @param data : 응답 데이터
	 * @return : success=true, data=data 인 ApiResponse
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	/**
	 * 성공 응답 생성 (메시지만 존재, 단순 성공 알림용)
	 *
	 * @param message : 성공 알림 메시지
	 * @return : success=true, message=message 인 ApiResponse
	 */
	public static ApiResponse<Void> successMessage(String message) {
		return new ApiResponse<>(true, null, message);
	}
}
