package com.After_Buy.DeviceService.config;

import com.After_Buy.DeviceService.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 스프링 시큐리티 및 인가 방화벽 코어 구성 파일
 * JWT 검증 필터를 체인에 등록하고 CSRF 비활성화, Stateless 세션, CORS 정책을 설정합니다.
 * Device Service 전용 공개 엔드포인트(Actuator, Swagger, 내부 API)를 허용합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	/**
	 * 통합 서블릿 통행 제어 및 권한 결정 빈
	 * CSRF를 끄고 JWT 필터를 최앞단에 장착하며 공개 허용 엔드포인트 예외를 처리하고
	 * 나머지 모든 요청에 대해 인증을 요구합니다.
	 *
	 * @param http : 방어 체인을 빌드할 스프링의 HttpSecurity 원시 세팅 도구
	 * @return : 각종 예외 정책과 룰이 버무려진 보안 필터 체인 반환
	 * @throws Exception : 초기화 중 보안 객체 구성 불발 시 예외 강제 보고
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// 헬스 체크 및 Swagger UI 공개
				.requestMatchers("/actuator/health").permitAll()
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api/devices/swagger-ui/**", "/api/devices/swagger-ui.html", "/api/devices/v3/api-docs/**").permitAll()
				// MSA 내부 서비스 간 통신 경로 공개 (별도 InternalSecretFilter로 보호)
				.requestMatchers("/internal/**").permitAll()
				// 나머지 모든 Device API는 JWT 인증 필수
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * CORS 횡단 자원 공유 정책 빈
	 * 모바일 앱(React Native) 및 웹 클라이언트 양방향 환경에서 차단 없이 접속 가능하도록 설정합니다.
	 *
	 * @return : 모든 출처 및 헤더가 수락되도록 구성된 개방형 CORS 소스 집합 빈
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
