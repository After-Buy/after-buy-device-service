package com.After_Buy.DeviceService.service;

import com.After_Buy.DeviceService.dto.request.PresignedUrlRequest;
import com.After_Buy.DeviceService.dto.response.PresignedUrlResponse;
import com.After_Buy.DeviceService.exception.CustomException;
import com.After_Buy.DeviceService.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 기기 이미지 S3 전용 서비스
 * 클라이언트 다이렉트 업로드를 위한 Presigned URL 발급 기능을 수행합니다.
 * 
 * @since : 2026.04.12
 * @version : 1.0.0
 * @author : 최준혁
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    // 허용하는 이미지 확장자 목록
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    /**
     * 클라이언트가 직접 S3 버킷에 이미지를 업로드할 수 있는 Presigned URL을 발급합니다.
     * 
     * @param request 사용자 이미지 확장자 (ex. "jpg")
     * @return PresignedUrlResponse (URL 정보 객체)
     */
    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
        String ext = request.getFileExtension().trim().toLowerCase();
        
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            log.warn("[S3Service] 지원하지 않는 확장자 요청: {}", ext);
            throw new CustomException(ErrorCode.DEVICE_INVALID_IMAGE_EXTENSION);
        }

        // Object Key 생성 규칙: devices/UUID_image.ext
        String fileName = "devices/" + UUID.randomUUID() + "_image." + ext;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // 5분 (300초) 설정
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);

        // 생성된 전체 URL (쿼리 파람 포함)
        String presignedUrl = presignedPutObjectRequest.url().toString();
        // 쿼리 파라미터 제외하고 사용할 순수 외부 URL 추출
        String imageUrl = presignedUrl.split("\\?")[0];

        log.info("[S3Service] Pre-signed URL 생성 완료: {}", imageUrl);

        return PresignedUrlResponse.builder()
                .presignedUrl(presignedUrl)
                .imageUrl(imageUrl)
                .expiresIn(300L) 
                .build();
    }
}
