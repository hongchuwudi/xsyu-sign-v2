package com.hongchu.qqrobotsign;

import com.hongchu.qqrobotsign.enums.CasErrorType;
import com.hongchu.qqrobotsign.pojo.entity.CasCaptchaState;
import com.hongchu.qqrobotsign.utils.CasLoginResult;
import com.hongchu.qqrobotsign.utils.XSYULoginUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

/**
 * 测试验证码图片获取流程，同时输出详细的步骤日志
 * 运行 main 方法即可
 */
public class TestCaptchaImage {

    private static final String USERNAME = "202307070211";
    private static final String PASSWORD = "liu.2005";

    private static final String SERVICE = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin";
    private static final String CAS_LOGIN_URL = "https://ids.xsyu.edu.cn/authserver/login";
    private static final String CAS_HOST = "ids.xsyu.edu.cn";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    public static void main(String[] args) {
        System.out.println("========== CAS 登录详细测试 ==========");
        System.out.println("用户: " + USERNAME);
        System.out.println();

        // 先用 XSYULoginUtil 快速测试
        System.out.println("--- 方式1: XSYULoginUtil.login() ---");
        CasLoginResult result = XSYULoginUtil.login(USERNAME, PASSWORD);
        System.out.println("结果: " + result.getErrorType() + " | " + result.getErrorMessage());
        if (result.getCaptchaImageBase64() != null) {
            System.out.println("验证码图片: " + result.getCaptchaImageBase64().substring(0, 60) + "...");
        }
        if (result.getJwsession() != null) {
            System.out.println("JWSESSION: " + result.getJwsession());
        }
        System.out.println();

        // 详细逐步测试
        System.out.println("--- 方式2: 逐步调试 ---");
        Map<String, String> cookies = new LinkedHashMap<>();
        try {
            // 步骤1: 获取CAS登录URL
            System.out.println("[步骤1] 获取CAS登录URL...");
            String casUrl = getCasLoginUrl(cookies);
            if (casUrl == null) {
                System.out.println("  [失败] 初始302重定向失败，打印响应信息...");
                dumpCasPage();
                return;
            }
            System.out.println("  [成功] CAS URL: " + casUrl);
            System.out.println("  Cookies(" + cookies.size() + "): " + cookies.keySet());

            // 步骤2: 获取execution
            System.out.println("\n[步骤2] 获取execution参数...");
            String execution = getExecution(casUrl, cookies);
            if (execution == null) {
                System.out.println("  [失败] 未能从CAS页面提取execution");
                return;
            }
            System.out.println("  [成功] execution: " + execution);

            // 模拟延迟
            Thread.sleep(1000);

            // 步骤3: 提交登录表单
            System.out.println("\n[步骤3] 提交CAS登录表单...");
            submitAndAnalyze(USERNAME, PASSWORD, execution, cookies);

        } catch (Exception e) {
            System.err.println("异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============ 逐步调试方法 ============

    private static String getCasLoginUrl(Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(SERVICE).openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        captureCookies(conn, cookies);
        int code = conn.getResponseCode();
        System.out.println("  响应码: " + code);
        if (code == 302) {
            return conn.getHeaderField("Location");
        }
        // 读取响应体
        String body = readBody(conn);
        System.out.println("  响应体前200字符: " + body.substring(0, Math.min(200, body.length())));
        return null;
    }

    private static String getExecution(String casUrl, Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(casUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        applyCookies(conn, cookies);
        String html = readBody(conn);
        captureCookies(conn, cookies);

        System.out.println("  响应长度: " + html.length());

        // 检查是否有错误提示
        if (html.contains("message")) {
            String msg = extractPattern(html, "<span[^>]*id=\"msg\"[^>]*>([^<]+)</span>");
            if (msg != null) System.out.println("  页面消息: " + msg);
            msg = extractPattern(html, "<div[^>]*class=\"[^\"]*error[^\"]*\"[^>]*>([^<]+)</div>");
            if (msg != null) System.out.println("  错误信息: " + msg);
        }
        if (html.contains("expired") || html.contains("过期")) {
            System.out.println("  [注意] 页面可能已过期");
        }

        return extractExecution(html);
    }

    private static void submitAndAnalyze(String username, String password, String execution,
                                          Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(CAS_LOGIN_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Origin", "https://" + CAS_HOST);
        conn.setRequestProperty("Referer", "https://" + CAS_HOST + "/authserver/login?service="
                + URLEncoder.encode(SERVICE, StandardCharsets.UTF_8));
        applyCookies(conn, cookies);

        StringBuilder body = new StringBuilder();
        body.append("username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));
        body.append("&password=").append(URLEncoder.encode(password, StandardCharsets.UTF_8));
        body.append("&execution=").append(URLEncoder.encode(execution, StandardCharsets.UTF_8));
        body.append("&_eventId=submit");
        body.append("&loginType=1");
        body.append("&rememberMe=true");
        body.append("&service=").append(URLEncoder.encode(SERVICE, StandardCharsets.UTF_8));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        captureCookies(conn, cookies);
        System.out.println("  HTTP响应码: " + code);

        // 输出所有响应头
        System.out.println("  响应头:");
        conn.getHeaderFields().forEach((k, v) -> {
            if (k != null && !"Set-Cookie".equalsIgnoreCase(k)) {
                System.out.println("    " + k + ": " + v);
            }
        });

        if (code == 302) {
            String location = conn.getHeaderField("Location");
            System.out.println("  302 Location: " + location);

            if (location != null) {
                String ticket = extractPattern(location, "ticket=([^\"&\\s]+)");
                System.out.println("  提取ticket: " + (ticket != null ? ticket : "无"));
            }

            // 尝试跟踪重定向获取JWSESSION
            if (location != null) {
                System.out.println("\n[步骤4] 跟踪重定向获取JWSESSION...");
                String jws = followJWSession(location, cookies, 0);
                if (jws != null) {
                    System.out.println("  [成功] JWSESSION: " + jws);
                } else {
                    System.out.println("  [失败] 未能获取JWSESSION");
                    System.out.println("  最终Cookies: " + cookies.keySet());
                }
            }
        } else if (code == 200) {
            String html = readBody(conn);
            System.out.println("  200响应长度: " + html.length());
            System.out.println("  响应体前500字符:");
            System.out.println(html.substring(0, Math.min(500, html.length())));

            // 诊断
            diagnoseHtml(html);
        } else {
            System.out.println("  意外HTTP状态码: " + code);
            String errBody = readBodySafe(conn);
            if (errBody != null) {
                System.out.println("  响应体前300字符: " + errBody.substring(0, Math.min(300, errBody.length())));
            }
        }
    }

    private static void diagnoseHtml(String html) {
        System.out.println("\n  === 诊断信息 ===");
        // 提取错误消息
        String msg = extractPattern(html, "<span[^>]*id=\"msg\"[^>]*>([^<]+)</span>");
        if (msg != null) System.out.println("  msg span: " + msg);

        // 检查各种可能状态
        if (html.contains("验证码") || html.contains("captcha")) {
            System.out.println("  [检测] 需要验证码！");
            String imgSrc = extractPattern(html, "<img[^>]*src=[\"']([^\"']*(?:captcha|captchaImage|validateCode)[^\"']*)[\"']");
            System.out.println("  验证码图片URL: " + (imgSrc != null ? imgSrc : "未匹配到"));
            // 尝试更宽松的匹配
            Pattern p = Pattern.compile("<img[^>]*src=[\"']([^\"']+)[\"'][^>]*>");
            Matcher m = p.matcher(html);
            int count = 0;
            while (m.find() && count < 10) {
                String src = m.group(1);
                System.out.println("  页面图片 " + (count + 1) + ": " + src);
                count++;
            }
        }
        if (html.contains("锁定") || html.contains("禁用") || html.contains("locked")) {
            System.out.println("  [检测] 账号已被锁定/禁用！");
        }
        if (html.contains("不正确") || html.contains("错误") || html.contains("invalid")) {
            System.out.println("  [检测] 用户名或密码错误！");
        }
        if (html.contains("过期") || html.contains("expired")) {
            System.out.println("  [检测] 页面已过期！");
        }
        if (html.contains("execution")) {
            String exec = extractExecution(html);
            System.out.println("  execution: " + (exec != null ? exec : "提取失败"));
        }
    }

    private static void dumpCasPage() {
        try {
            Map<String, String> temp = new LinkedHashMap<>();
            HttpURLConnection conn = (HttpURLConnection) new URL(SERVICE).openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", UA);
            int code = conn.getResponseCode();
            System.out.println("  跟随重定向最终响应码: " + code);
            System.out.println("  URL: " + conn.getURL());
            if (code == 200) {
                String html = readBody(conn);
                System.out.println("  页面标题: " + extractPattern(html, "<title>([^<]+)</title>"));
                System.out.println("  前300字符: " + html.substring(0, Math.min(300, html.length())));
            }
        } catch (Exception e) {
            System.out.println("  dumpCasPage异常: " + e.getMessage());
        }
    }

    // ============ JWSESSION跟踪 ============

    private static String followJWSession(String url, Map<String, String> cookies, int depth) throws IOException {
        if (depth > 5) {
            System.out.println("  重定向深度超限(" + depth + ")");
            return null;
        }
        System.out.println("  [" + depth + "] 请求: " + url);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        applyCookies(conn, cookies);
        int code = conn.getResponseCode();
        captureCookies(conn, cookies);
        System.out.println("  [" + depth + "] 响应码: " + code);

        String jws = cookies.get("JWSESSION");
        if (jws != null && !jws.isEmpty()) {
            System.out.println("  [" + depth + "] 获取到JWSESSION");
            return jws;
        }

        if (code == 302) {
            String location = conn.getHeaderField("Location");
            if (location != null) {
                return followJWSession(location, cookies, depth + 1);
            }
        }
        return null;
    }

    // ============ 工具方法 ============

    private static void captureCookies(HttpURLConnection conn, Map<String, String> cookies) {
        List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");
        if (setCookies == null) return;
        for (String header : setCookies) {
            int semi = header.indexOf(';');
            String nv = semi > 0 ? header.substring(0, semi) : header;
            int eq = nv.indexOf('=');
            if (eq > 0) {
                cookies.put(nv.substring(0, eq).trim(), nv.substring(eq + 1).trim());
            }
        }
    }

    private static void applyCookies(HttpURLConnection conn, Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : cookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        conn.setRequestProperty("Cookie", sb.toString());
    }

    private static String readBody(HttpURLConnection conn) throws IOException {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static String readBodySafe(HttpURLConnection conn) {
        try { return readBody(conn); } catch (Exception e) { return null; }
    }

    private static String extractExecution(String html) {
        String exec = extractPattern(html, "name=\"execution\"\\s+value=\"([^\"]+)\"");
        if (exec != null) return exec;
        return extractPattern(html, "execution\"\\s+value=\"([^\"]+)\"");
    }

    private static String extractPattern(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }
}
