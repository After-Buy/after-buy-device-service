package com.After_Buy.DeviceService.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인트라넷 내부망 통신 전용 커스텀 보안 필터
 * 내부 MSA를 위해 /internal/** 경로로 들어오는 마이크로 서비스 간의 은밀한 호출을 타 외부인의 침입에 노출시키지 않도록 헤더 비밀키를 검수합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Component
@Order(1)
public class InternalSecretFilter implements Filter {

	// 애플리케이션 환경설정에 지정된 관리자 전용 내부 통신 시크릿 키 평문
	private final String internalSecretKey;

	public InternalSecretFilter(@Value("${internal.secret-key}") String internalSecretKey) {
		this.internalSecretKey = internalSecretKey;
	}

	/**
	 * 커스텀 필터 코어 체이닝 로직
	 * 서블릿의 URI가 internal로 시작할 경우 무조건 X-Internal-Secret 헤더의 무결성을 대조해본 뒤 통과시킵니다.
	 *
	 * @param req : 필터를 거쳐갈 1차 정제 전 날것의 서블릿 요청 객체
	 * @param res : 처리 후 클라이언트 네트워크로 분출될 반환용 응답 객체
	 * @param chain : 검문소를 지날 시 다음 컨트롤러나 필터로 향하게 해줄 허가 스프링 체인
	 * @throws IOException : 네트워크 단선 등 I/O 문제 시 방출
	 * @throws ServletException : 서블릿 컨테이너 내부 런타임 처리 도중 발생한 알 수 없는 침체 에러들 방어
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();

		if (!uri.startsWith("/internal/")) {
			chain.doFilter(req, res);
			return;
		}

		String secretHeader = request.getHeader("X-Internal-Secret");
		if (secretHeader == null || !secretHeader.equals(this.internalSecretKey)) {
			log.warn("내부 API 접근 차단: 유효하지 않은 X-Internal-Secret 헤더, URI={}", uri);
			response.setStatus(403);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"내부 API 접근 권한이 없습니다.\"}}");
			return;
		}
		chain.doFilter(req, res);
	}
}
