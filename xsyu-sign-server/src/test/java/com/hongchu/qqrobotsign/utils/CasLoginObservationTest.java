package com.hongchu.qqrobotsign.utils;

import com.hongchu.qqrobotsign.pojo.entity.CasCaptchaState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * 真实 CAS 登录可观测测试。
 * <p>
 * 不做断言，只输出真实请求的分类结果。为避免普通测试误触学校风控，必须显式设置运行开关和凭据环境变量。
 */
class CasLoginObservationTest {

    private static final String RUN_LOGIN = "XSYU_RUN_LOGIN_OBSERVATION";
    private static final String RUN_WRONG_PASSWORD = "XSYU_RUN_WRONG_PASSWORD_OBSERVATION";
    private static final String USERNAME = "XSYU_TEST_USERNAME";
    private static final String PASSWORD = "XSYU_TEST_PASSWORD";
    private static final String WRONG_PASSWORD = "XSYU_TEST_WRONG_PASSWORD";

    @Test
    void observeConfiguredPasswordLogin() {
        if (!isEnabled(RUN_LOGIN)) {
            printSkipped("真实登录", "请先设置 " + RUN_LOGIN + "=true");
            return;
        }

        String username = requiredEnvironment(USERNAME);
        String password = requiredEnvironment(PASSWORD);
        if (username == null || password == null) return;

        observe("配置密码登录", username, password);
    }

    @Test
    void observeExplicitWrongPasswordLogin() {
        if (!isEnabled(RUN_WRONG_PASSWORD)) {
            printSkipped("错误密码登录", "该测试会增加一次登录失败计数；需显式设置 "
                    + RUN_WRONG_PASSWORD + "=true");
            return;
        }

        String username = requiredEnvironment(USERNAME);
        String wrongPassword = requiredEnvironment(WRONG_PASSWORD);
        if (username == null || wrongPassword == null) return;

        System.out.println("[警告] 即将真实提交一次错误密码，可能触发验证码或账号锁定。");
        observe("错误密码登录", username, wrongPassword);
    }

    private static void observe(String title, String username, String password) {
        StringBuilder report = new StringBuilder();
        report.append(System.lineSeparator());
        report.append("========== CAS ").append(title).append("可观测测试 ==========").append(System.lineSeparator());
        report.append("测试账号: ").append(mask(username)).append(System.lineSeparator());
        report.append("开始时间: ").append(Instant.now()).append(System.lineSeparator());

        Instant startedAt = Instant.now();
        CasLoginResult result = XSYULoginUtil.login(username, password);
        long durationMillis = Duration.between(startedAt, Instant.now()).toMillis();

        report.append("耗时: ").append(durationMillis).append(" ms").append(System.lineSeparator());
        report.append("errorType: ").append(result.getErrorType()).append(System.lineSeparator());
        report.append("CAS errorMessage: ").append(valueOrNone(result.getErrorMessage())).append(System.lineSeparator());
        report.append("JWSESSION: ").append(maskToken(result.getJwsession())).append(System.lineSeparator());
        appendCaptchaReport(result, report);
        report.append("========== 观测结束 ==========").append(System.lineSeparator());

        System.out.print(report);
        Path reportOutput = writeObservationReport(report.toString());
        System.out.println("UTF-8观测报告: "
                + (reportOutput == null ? "写入失败" : reportOutput.toAbsolutePath()));
        System.out.println();
    }

    private static void appendCaptchaReport(CasLoginResult result, StringBuilder report) {
        CasCaptchaState state = result.getCaptchaState();
        String image = result.getCaptchaImageBase64();
        report.append("captchaRequired: ").append(state != null).append(System.lineSeparator());
        if (state != null) {
            report.append("captcha execution: ").append(maskToken(state.getExecution())).append(System.lineSeparator());
            report.append("captcha cookie count: ")
                    .append(state.getCookies() == null ? 0 : state.getCookies().size())
                    .append(System.lineSeparator());
        }
        if (image == null || image.isBlank()) {
            report.append("captcha image: 无").append(System.lineSeparator());
            return;
        }

        Path output = writeCaptchaImage(image);
        report.append("captcha image data length: ").append(image.length()).append(System.lineSeparator());
        report.append("captcha image file: ")
                .append(output == null ? "写入失败" : output.toAbsolutePath())
                .append(System.lineSeparator());
    }

    private static Path writeCaptchaImage(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0 || comma == dataUrl.length() - 1) return null;

        String extension = dataUrl.startsWith("data:image/jpeg") ? ".jpg" : ".png";
        Path output = Path.of("target", "cas-observation-captcha" + extension);
        try {
            Files.createDirectories(output.getParent());
            Files.write(output, Base64.getDecoder().decode(dataUrl.substring(comma + 1)));
            return output;
        } catch (IllegalArgumentException | IOException exception) {
            System.out.println("captcha image write error: " + exception.getMessage());
            return null;
        }
    }

    private static Path writeObservationReport(String report) {
        Path output = Path.of("target", "cas-login-observation.txt");
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, report, StandardCharsets.UTF_8);
            return output;
        } catch (IOException exception) {
            System.out.println("observation report write error: " + exception.getMessage());
            return null;
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            printSkipped("真实登录", "缺少环境变量 " + name);
            return null;
        }
        return value;
    }

    private static boolean isEnabled(String name) {
        return "true".equalsIgnoreCase(System.getenv(name));
    }

    private static void printSkipped(String title, String reason) {
        System.out.println("[跳过] CAS " + title + "可观测测试：" + reason);
    }

    private static String valueOrNone(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }

    private static String mask(String value) {
        if (value == null || value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private static String maskToken(String value) {
        if (value == null || value.isBlank()) return "无";
        if (value.length() <= 12) return "**** (length=" + value.length() + ")";
        return value.substring(0, 6) + "..." + value.substring(value.length() - 4)
                + " (length=" + value.length() + ")";
    }
}
