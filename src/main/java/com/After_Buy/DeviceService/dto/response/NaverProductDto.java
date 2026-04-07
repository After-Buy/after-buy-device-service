package com.After_Buy.DeviceService.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NaverProductDto {
    private String productName;
    private String modelName;
    private String brand;
    private String imageUrl;
    private String productLinkUrl;
}
