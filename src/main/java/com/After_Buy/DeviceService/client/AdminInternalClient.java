package com.After_Buy.DeviceService.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Admin Service 내부 API 호출 클라이언트
 * 500급 서버 예외 발생 시 Admin Service의 에러 로그 수집 API로 비동기 전송합니다.
 *
 * 설계 원칙:
 * - Fire & Forget: 에러 전송 지연이 원래 API 처리를 지연시켜서는 안 됩니다.
 * - .onErrorResume: Admin Service 다운 시에도 타 서비스 가용성에 영향 없도록 에러 무시.
 * - 오직 Exception.class 핸들러에서만 호출됩니다. (4xx 에러는 호출 제외)
 *
 * @since : 2026.04.26
 * @version : 1.0.0
 * @author : 신태훈
 */
@Slf4j
@Component
public class AdminInternalClient {

    private final WebClient webClient;

    public AdminInternalClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.admin-url}") String adminUrl,
            @Value("${internal.secret-key}") String internalSecretKey) {
        this.webClient = webClientBuilder
                .baseUrl(adminUrl)
                .defaultHeader("X-Internal-Secret", internalSecretKey)
                .build();
    }

    /**
     * Admin Service로 500급 서버 크래시 에러 로그를 비동기 전송합니다. (Fire & Forget)
     * Admin Service 다운 시에도 .onErrorResume으로 에러를 무시하여 Device Service 가용성을 보호합니다.
     *
     * @param endpointPath : 에러 발생 엔드포인트 경로
     * @param e            : 발생한 예외
     */
    public void sendErrorLogAsync(String endpointPath, Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

        Map<String, Object> body = Map.of(
                "service_name", "DEVICE",
                "endpoint_path", endpointPath,
                "error_type", e.getClass().getSimpleName(),
                "error_message", errorMessage.length() > 500 ? errorMessage.substring(0, 497) + "..." : errorMessage,
                "occurred_at", LocalDateTime.now().toString()
        );

        webClient.post()
                .uri("/internal/error-logs")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(error -> {
                    /* Admin Service 연결 실패 시 경고 로그만 출력하고 무시 (2차 장애 방지) */
                    log.warn("[AdminInternalClient] 에러 로그 전송 실패 (무시): {}", error.getMessage());
                    return Mono.empty();
                })
                .subscribe(); // 비동기 실행 (Fire & Forget)
    }
}
