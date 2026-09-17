package com.hongchu.qqrobotsign.pojo.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignDTO {
    @Builder.Default
    Integer inArea = 1;                 // 是否在区域内
    String areaJSON;                // 区域信息
    Double latitude;                // 纬度
    Double longitude;               // 经度
}
