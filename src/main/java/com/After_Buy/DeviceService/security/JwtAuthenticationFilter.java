package com.After_Buy.DeviceService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 토큰 검증 필터
 * 스프링 시큐리티 인증 필터 체인 앞단에서 매 요청마다 Authorization 헤더의 Bearer 토큰을 파싱하고,
 * 유효한 경우 SecurityContextHolder에 UserPrincipal 인증 객체를 등록합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private static final String TOKEN_EXPIRED_RESPONSE = """
			{
			  "success": false,
			  "error": {
			    "code": "TOKEN_EXPIRED",
			    "message": "Access Token이 만료되었거나 유효하지 않습니다."
			  }
			}
			""";

	/**
	 * 필터 체인 내부 검증 로직 구현부
	 * 서블릿 컨테이너로 들어오는 모든 요청에 대해 Authorization 헤더를 확인하고,
	 * 정상 토큰일 시 UserPrincipal을 시큐리티 컨텍스트에 주입합니다.
	 *
	 * @param request     : 클라이언트가 보낸 HTTP 서블릿 리퀘스트
	 * @param response    : 클라이언트에게 보낼 HTTP 서블릿 리스폰스
	 * @param filterChain : 뒤에 이어질 보안 및 컨트롤러 체인 라인
	 * @throws ServletException : 서블릿 단 예외 발생 시
	 * @throws IOException      : I/O 네트워크 파이프 단절 시 발생
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = extractToken(request);
		if (StringUtils.hasText(token)) {
			if (!jwtTokenProvider.validateToken(token)) {
				if (isPublicPath(request)) {
					filterChain.doFilter(request, response);
					return;
				}
				SecurityContextHolder.clearContext();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().write(TOKEN_EXPIRED_RESPONSE);
				return;
			}
			try {
				Long userId = jwtTokenProvider.getUserIdFromToken(token);
				UserPrincipal userPrincipal = new UserPrincipal(userId);
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (Exception e) {
				log.debug("JWT 인증 처리 중 오류: {}", e.getMessage());
			}
		}
		filterChain.doFilter(request, response);
	}

	private boolean isPublicPath(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/internal/")
				|| path.equals("/actuator/health")
				|| path.equals("/swagger-ui.html")
				|| path.startsWith("/swagger-ui/")
				|| path.startsWith("/v3/api-docs/")
				|| path.equals("/api/devices/swagger-ui.html")
				|| path.startsWith("/api/devices/swagger-ui/")
				|| path.startsWith("/api/devices/v3/api-docs/");
	}

	/**
	 * Authorization 헤더 내 순수 토큰 정제 추출 헬퍼
	 * Bearer 타입 규격의 토큰 문자열에서 프리픽스(Bearer )를 잘라내고 JWT 본체만 반환합니다.
	 *
	 * @param request : 토큰 헤더가 심겨있는 클라이언트의 요청 본문
	 * @return : 정제된 JWT 서명 텍스트 (규격 불일치 또는 미존재 시 null 반환)
	 */
	private String extractToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
