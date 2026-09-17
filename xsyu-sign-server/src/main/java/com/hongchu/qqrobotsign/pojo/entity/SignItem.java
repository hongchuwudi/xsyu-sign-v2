package com.hongchu.qqrobotsign.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 签到列表项
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // 避免序列化null字段
public class SignItem {
    // 基础信息
    private String id;           // 记录ID                                --核心字段
    private String signId;       // 签到ID                                --核心字段
    private String signTitle;    // 签到标题
    private String signContext;  // 签到内容
    private Integer signMode;    // 签到模式
    private Integer signStatus;  // 签到状态 (1:未签到, 2:已签到) --注意：两个JSON都是2，可能需要重新确认含义
    private Long start;          // 开始时间戳
    private Long end;            // 结束时间戳
    private String signDay;      // 签到日期 (格式: yyyyMMdd) --只在签到成功后存在

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Long date;           // 实际签到时间戳 --只在签到成功后存在

    private Long readDate;       // 阅读时间戳
    private Integer isRead;      // 是否已读 (1:是, 0:否)
    private Integer type;        // 类型 (0:待处理/未完成, 1:已完成签到)
    private Integer mode;        // 模式

    // 用户信息
    private String userId;       // 用户ID
    private String name;         // 用户姓名
    private String number;       // 学号/工号
    private String phone;        // 手机号
    private String userType;     // 用户类型 (在校学生等)
    private String userArea;     // 用户所在区域
    private String head;         // 头像URL
    private String nation;       // 民族

    // 学校信息
    private String schoolId;     // 学校ID
    private String year;         // 入学年份
    private String degree;       // 学历 (本科等)
    private String college;      // 学院
    private String major;        // 专业
    private String classes;      // 班级
    private String classesId;    // 班级ID

    // 位置信息（这些字段只在签到成功后存在）
    private String area;         // 实际签到区域名称
    private String areaId;       // 实际签到区域ID
    private String latitude;     // 实际签到纬度
    private String longitude;    // 实际签到经度
    private List<AreaInfo> areaList;  // 允许签到的区域列表

    // 地址详情（可选字段）
    private String country;      // 国家
    private String province;     // 省份
    private String city;         // 城市
    private String district;     // 区县
    private String township;     // 乡镇
    private String street;       // 街道

    // 创建者信息
    private String createName;   // 创建者姓名
    private String createCollege;// 创建者学院
    private String createHead;   // 创建者头像
    private String teacher;      // 教师姓名
    private String teacherId;    // 教师ID

    // 目标信息
    private String targetId;     // 目标ID
    private String targetName;   // 目标名称
    private Integer targetType;  // 目标类型

    // 其他字段
    private Integer leaderSign;  // 领导签到标识
    private Integer qrCode;      // 二维码标识
    private String signUserId;   // 签到用户ID --只在签到成功后存在
    private String signUserName; // 签到用户姓名 --只在签到成功后存在
    private String signUserNumber; // 签到用户学号 --只在签到成功后存在
    private String signUserType; // 签到用户类型 --只在签到成功后存在

    // JSON中的额外字段（可选）
    private Boolean signed;      // 是否已签到（布尔值）--JSON中的扩展字段
    private Boolean inSignTime;  // 是否在签到时间内（布尔值）--JSON中的扩展字段
}