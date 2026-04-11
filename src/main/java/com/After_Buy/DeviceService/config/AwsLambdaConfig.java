package com.After_Buy.DeviceService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

/**
 * AWS Lambda 클라이언트 설정
 * OcrService에 주입할 LambdaClient 빈을 등록합니다.
 * 인증 정보는 application.yaml의 spring.cloud.aws 설정을 공유합니다.
 *
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Configuration
public class AwsLambdaConfig {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * AWS Lambda SDK 클라이언트 빈 등록
     * IAM 키 기반 정적 인증 방식을 사용합니다.
     *
     * @return LambdaClient : Lambda 함수 호출 전용 클라이언트
     */
    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }
}
