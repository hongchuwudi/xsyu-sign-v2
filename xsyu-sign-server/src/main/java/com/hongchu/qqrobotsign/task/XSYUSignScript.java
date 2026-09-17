package com.hongchu.qqrobotsign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * 西安石油大学一键签到脚本（纯Java，无Spring）
 * 使用：直接运行 main，输入学号密码，自动签到所有待签项
 */
public class XSYUSignScript {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    // 移动端UA
    private static final String UA = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36";

    // 签到位置（鄠邑校区）
    private static final String IN_AREA = "1";
    private static final String AREA_JSON = "{\"type\":\"Point\",\"coordinates\":[108.654321,34.123456]}";
    private static final String LAT = "34.123456";
    private static final String LNG = "108.654321";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("学号: ");
        String user = sc.nextLine().trim();
        System.out.print("密码: ");
        String pass = sc.nextLine().trim();

        try {
            // 1. 登录
            System.out.println("\n正在登录...");
            String jws = LoginUtil.login(user, pass);
            if (jws == null) {
                System.err.println("登录失败");
                return;
            }
            System.out.println("登录成功，JWSESSION: " + jws);

            // 2. 获取签到列表
            System.out.println("\n获取签到列表...");
            List<SignItem> items = getSignList(jws);
            if (items.isEmpty()) {
                System.out.println("没有待签项目");
                return;
            }
            System.out.println("待签项: " + items.size());

            // 3. 逐个签到
            int ok = 0;
            for (SignItem it : items) {
                System.out.printf("签到: %s ... ", it.name);
                if (doSign(jws, it)) {
                    System.out.println("成功");
                    ok++;
                } else {
                    System.out.println("失败");
                }
            }
            System.out.printf("\n完成: %d/%d 成功\n", ok, items.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取待签到列表
    private static List<SignItem> getSignList(String jws) throws Exception {
        String url = "https://gwxg.xsyu.edu.cn/h5/mobile/basicinfo/sign/getAllSign?page=1&size=20";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", "application/json, text/plain, */*")
                .header("JWSESSION", jws)
                .GET().build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return Collections.emptyList();

        JsonNode root = MAPPER.readTree(resp.body());
        if (root.path("code").asInt() != 0) {
            System.err.println("接口错误: " + root.path("message").asText());
            return Collections.emptyList();
        }

        List<SignItem> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (JsonNode n : root.path("data")) {
            int status = n.path("signStatus").asInt();
            long start = n.path("start").asLong();
            long end = n.path("end").asLong();
            if (status == 1 && now >= start && now <= end) {
                SignItem it = new SignItem();
                it.id = n.path("id").asText();
                it.signId = n.path("signId").asText();
                it.schoolId = n.path("schoolId").asText();
                it.name = n.path("signTitle").asText();
                list.add(it);
            }
        }
        return list;
    }

    // 执行单个签到
    private static boolean doSign(String jws, SignItem it) throws Exception {
        String url = String.format(
                "https://gwxg.xsyu.edu.cn/h5/mobile/basicinfo/sign/sign?id=%s&signId=%s&schoolId=%s",
                it.id, it.signId, it.schoolId);
        String body = String.format(
                "{\"inArea\":\"%s\",\"areaJSON\":\"%s\",\"latitude\":\"%s\",\"longitude\":\"%s\"}",
                IN_AREA, AREA_JSON, LAT, LNG);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/plain, */*")
                .header("JWSESSION", jws)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return false;
        JsonNode root = MAPPER.readTree(resp.body());
        return root.path("code").asInt() == 0;
    }

    // 简单数据类
    static class SignItem {
        String id, signId, schoolId, name;
    }

    // ---------- 登录工具（完全复用你提供的代码，无改动）----------
    public static class LoginUtil {
        public static String login(String username, String password) {
            Map<String, String> cookies = new HashMap<>();
            try {
                String casLoginUrl = getCasLoginUrl(cookies);
                if (casLoginUrl == null) return null;
                String execution = getExecutionFromCas(casLoginUrl, cookies);
                if (execution == null) return null;
                String ticket = submitCasLogin(username, password, execution, cookies);
                if (ticket == null) return null;
                return getJWSessionWithTicket(ticket, cookies);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private static String getCasLoginUrl(Map<String, String> cookies) throws IOException {
            String gwxgUrl = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin";
            HttpURLConnection conn = (HttpURLConnection) new URL(gwxgUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            processCookies(conn, cookies);
            return conn.getResponseCode() == 302 ? conn.getHeaderField("Location") : null;
        }

        private static String getExecutionFromCas(String casUrl, Map<String, String> cookies) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) new URL(casUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (!cookies.isEmpty()) conn.setRequestProperty("Cookie", buildCookieHeader(cookies));
            processCookies(conn, cookies);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return extractExecution(sb.toString());
        }

        private static String submitCasLogin(String username, String password, String execution, Map<String, String> cookies) throws IOException {
            String casLoginUrl = "https://ids.xsyu.edu.cn/authserver/login";
            HttpURLConnection conn = (HttpURLConnection) new URL(casLoginUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (!cookies.isEmpty()) conn.setRequestProperty("Cookie", buildCookieHeader(cookies));

            String serviceUrl = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin";
            String body = "username=" + URLEncoder.encode(username, "UTF-8") +
                    "&password=" + URLEncoder.encode(password, "UTF-8") +
                    "&execution=" + URLEncoder.encode(execution, "UTF-8") +
                    "&_eventId=submit&loginType=1&rememberMe=true&service=" + URLEncoder.encode(serviceUrl, "UTF-8");
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes("UTF-8")); }
            processCookies(conn, cookies);
            if (conn.getResponseCode() == 302) {
                String loc = conn.getHeaderField("Location");
                if (loc != null && loc.contains("ticket=")) {
                    String[] parts = loc.split("ticket=");
                    if (parts.length > 1) {
                        String t = parts[1];
                        if (t.contains("&")) t = t.split("&")[0];
                        return t;
                    }
                }
            }
            // 处理200等情况（保留原逻辑）
            if (conn.getResponseCode() == 200) {
                // 尝试从页面提取ticket
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                String html = sb.toString();
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("ticket=([^\"&\\s]+)");
                java.util.regex.Matcher m = p.matcher(html);
                if (m.find()) return m.group(1);
            }
            return null;
        }

        private static String getJWSessionWithTicket(String ticket, Map<String, String> cookies) throws IOException {
            String url = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin?ticket=" + URLEncoder.encode(ticket, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (!cookies.isEmpty()) conn.setRequestProperty("Cookie", buildCookieHeader(cookies));
            processCookies(conn, cookies);
            if (cookies.containsKey("JWSESSION")) return cookies.get("JWSESSION");
            if (conn.getResponseCode() == 302) {
                return followRedirectChain(conn.getHeaderField("Location"), cookies, 0);
            }
            return null;
        }

        private static String followRedirectChain(String redirectUrl, Map<String, String> cookies, int depth) throws IOException {
            if (depth > 5) return null;
            HttpURLConnection conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (!cookies.isEmpty()) conn.setRequestProperty("Cookie", buildCookieHeader(cookies));
            processCookies(conn, cookies);
            if (cookies.containsKey("JWSESSION")) return cookies.get("JWSESSION");
            if (conn.getResponseCode() == 302) {
                return followRedirectChain(conn.getHeaderField("Location"), cookies, depth + 1);
            }
            return null;
        }

        private static String extractExecution(String html) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("name=\"execution\" value=\"([^\"]+)\"");
            java.util.regex.Matcher m = p.matcher(html);
            return m.find() ? m.group(1) : null;
        }

        private static void processCookies(HttpURLConnection conn, Map<String, String> cookies) {
            Map<String, List<String>> headers = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if ("Set-Cookie".equalsIgnoreCase(e.getKey())) {
                    for (String c : e.getValue()) {
                        String[] parts = c.split(";")[0].split("=", 2);
                        if (parts.length == 2) cookies.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }

        private static String buildCookieHeader(Map<String, String> cookies) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : cookies.entrySet()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
            return sb.toString();
        }
    }
}