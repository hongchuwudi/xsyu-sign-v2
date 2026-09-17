package com.hongchu.qqrobotsign;

import com.hongchu.qqrobotsign.config.props.SignInfoConfig;
import com.hongchu.qqrobotsign.config.props.TestUserConfig;
import com.hongchu.qqrobotsign.config.props.UrlConfig;
import com.hongchu.qqrobotsign.pojo.DTO.SignDTO;
import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import com.hongchu.qqrobotsign.service.EmailService;
import com.hongchu.qqrobotsign.service.IUserService;
import com.hongchu.qqrobotsign.service.SignService;
import com.hongchu.qqrobotsign.webClient.BaseSignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QqRobotSignApplicationTests {
    @Autowired private IUserService userService;
    @Autowired private BaseSignService baseSignService;
    @Autowired private SignInfoConfig signInfoConfig;
    @Autowired private TestUserConfig testUserConfig;
    @Autowired private UrlConfig urlConfig;
    @Autowired private EmailService emailUtil;
    @Autowired private SignService signService;

    @Value("${spring.mail.username}")
    private String fromEmail;
    @Test
    void contextLoads() {
        // 使用配置的测试用户
        String username = testUserConfig.getUsername();

        // 获取所有签到
        String allSign = String.valueOf(baseSignService.getAllSign(username, 1, 2));
        System.out.println("所有签到: " + allSign);

//         使用配置的签到参数
        SignDTO signDTO = SignDTO.builder()
                .inArea(signInfoConfig.getInArea())
                .areaJSON(signInfoConfig.getAreaJson())
                .latitude(signInfoConfig.getLatitude())
                .longitude(signInfoConfig.getLongitude())
                .build();
        SignItem signItem = SignItem.builder()
                .id("911322444311171278")
                .signId("911322443652530176")
                .schoolId("17")
                .build();
//        // 执行签到
//        String sign = String.valueOf(baseSignService.sign(username,
//                "911322444311171278", "911322443652530176", "17", signDTO));
//        System.out.println("签到结果: " + sign);
//        signService.signAll(username);
        signService.processSingleSign(username,signItem, signDTO);
    }

    @Test
    void testConfigClasses() {
        // 单独测试配置类是否正确加载
        System.out.println("=== 配置类测试 ===");

        // 测试 URL 配置
        System.out.println("URL配置:");
        System.out.println("  基础URL: " + urlConfig.getBaseUrl());
        System.out.println("  接口前缀: " + urlConfig.getUriPri());
        System.out.println("  完整获取URL: " + urlConfig.getFullGetAllSignUrl());
        System.out.println("  完整详情URL: " + urlConfig.getFullGetOneSignUrl());
        System.out.println("  完整签到URL: " + urlConfig.getFullSignUrl());

        // 测试签到信息配置
        System.out.println("签到信息配置:");
        System.out.println("  是否在区域内: " + signInfoConfig.getInArea());
        System.out.println("  区域JSON: " + signInfoConfig.getAreaJson());
        System.out.println("  纬度: " + signInfoConfig.getLatitude());
        System.out.println("  经度: " + signInfoConfig.getLongitude());

        // 测试用户配置
        System.out.println("测试用户配置:");
        System.out.println("  用户名: " + testUserConfig.getUsername());
        System.out.println("  密码: " + testUserConfig.getPassword());

        System.out.println("=== 配置类测试完成 ===");
    }

    @Test
    void testSendEmail() {
        String to = fromEmail;  // 替换为你的测试邮箱
        String subject = "📧 油签机 - 测试邮件";
        String content = """
            🎉 邮件发送测试成功！
            
            📅 发送时间：%s
            🔧 项目名称：QQ Robot Sign
            📱 功能说明：自动化签到系统
            
            ✅ 此邮件用于测试邮件服务是否正常工作。
            
            祝使用愉快！
            🚀 油签机团队
            """.formatted(java.time.LocalDateTime.now());

        try {
            emailUtil.sendSimpleEmail(to, subject, content);
            System.out.println("✅ 邮件发送测试完成，请检查收件箱");
        } catch (Exception e) {
            System.out.println("❌ 邮件发送失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void testEmail(){
        emailUtil.sendSignResultNotice(fromEmail, "测试用户", true, "签到成功");

    }
}