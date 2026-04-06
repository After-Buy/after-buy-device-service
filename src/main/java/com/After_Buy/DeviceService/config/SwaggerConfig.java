package com.After_Buy.DeviceService.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(SpringDoc OpenAPI) 설정 파일
 * Swagger UI에서 JWT Bearer Token 입력란을 제공하여 인증이 필요한 API를 테스트할 수 있도록 설정합니다.
 *
 * @since : 2026.04.06
 * @version : 1.0.0
 * @author : 최준혁
 */
@Configuration
public class SwaggerConfig {

	/**
	 * OpenAPI 명세 빈 등록 메소드
	 * Device Service API 정보 및 JWT Bearer Token 인증 스키마를 Swagger UI에 등록합니다.
	 *
	 * @return : JWT 인증이 설정된 OpenAPI 명세 빈
	 */
	@Bean
	public OpenAPI openAPI() {
		// JWT Bearer Token 인증 스키마 정의
		String securitySchemeName = "BearerAuth";
		SecurityScheme securityScheme = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.name(securitySchemeName);

		return new OpenAPI()
				.info(new Info()
						.title("AfterBuy - Device Service API")
						.description("기기 관리(CRUD), OCR, 네이버 쇼핑 검색, S3 이미지 업로드, 폴더 관리 API")
						.version("v1.0.0"))
				.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
				.components(new Components().addSecuritySchemes(securitySchemeName, securityScheme));
	}
}
