package com.hongchu.qqrobotsign.pojo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 区域信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AreaInfo {
    private String id;          // 区域ID
    private String name;        // 区域名称
    private String latitude;    // 纬度（字符串格式）
    private String longitude;   // 经度（字符串格式）
    private Integer radius;     // 半径（米）
    private Integer shape;      // 形状（0:圆形, 1:多边形等）
    private String dataStr;     // 额外数据字符串

    /**
     * 获取纬度数值
     */
    public Double getLatitudeDouble() {
        if (latitude != null && !latitude.isEmpty()) {
            try {
                return Double.parseDouble(latitude);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取经度数值
     */
    public Double getLongitudeDouble() {
        if (longitude != null && !longitude.isEmpty()) {
            try {
                return Double.parseDouble(longitude);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 检查是否为圆形区域
     */
    public boolean isCircleArea() {
        return shape != null && shape == 0;
    }

    /**
     * 检查区域是否有效
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() &&
                name != null && !name.isEmpty() &&
                getLatitudeDouble() != null &&
                getLongitudeDouble() != null &&
                radius != null && radius > 0;
    }
}