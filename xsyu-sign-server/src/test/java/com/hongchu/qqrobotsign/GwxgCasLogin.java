package com.hongchu.qqrobotsign;

import java.io.*;
import java.net.*;
import java.util.*;

public class GwxgCasLogin {

    public static void main(String[] args) {
        String jwsession = login("20230707XXXX", "XXXX");
        if (jwsession != null) {
            System.out.println("登录成功！");
            System.out.println("JWSESSION: " + jwsession);
        } else {
            System.out.println("登录失败！");
        }
    }

    public static String login(String username, String password) {
        Map<String, String> cookies = new HashMap<>();

        try {
            // 第一步：访问gwxg系统，获取重定向到统一认证的URL
            String casLoginUrl = getCasLoginUrl(cookies);
            if (casLoginUrl == null) {
                System.out.println("无法获取CAS登录URL");
                return null;
            }
            System.out.println("CAS登录URL: " + casLoginUrl);

            // 第二步：从统一认证系统获取execution参数
            String execution = getExecutionFromCas(casLoginUrl, cookies);
            if (execution == null) {
                System.out.println("无法从CAS获取execution参数");
                return null;
            }
            System.out.println("获取到execution: " + execution);

            // 第三步：向统一认证系统提交登录
            String ticket = submitCasLogin(username, password, execution, cookies);
            if (ticket == null) {
                System.out.println("CAS登录失败");
                return null;
            }
            System.out.println("获取到ticket: " + ticket);

            // 第四步：使用ticket回到gwxg系统获取JWSESSION
            return getJwsessionWithTicket(ticket, cookies);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getCasLoginUrl(Map<String, String> cookies) throws IOException {
        String gwxgUrl = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin";

        HttpURLConnection connection = (HttpURLConnection) new URL(gwxgUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        processCookies(connection, cookies);

        int responseCode = connection.getResponseCode();
        System.out.println("初始访问响应码: " + responseCode);

        if (responseCode == 302) {
            String location = connection.getHeaderField("Location");
            System.out.println("重定向到: " + location);
            return location;
        }

        return null;
    }

    private static String getExecutionFromCas(String casUrl, Map<String, String> cookies) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(casUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        if (!cookies.isEmpty()) {
            connection.setRequestProperty("Cookie", buildCookieHeader(cookies));
        }

        processCookies(connection, cookies);

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return extractExecution(response.toString());
    }

    private static String submitCasLogin(String username, String password,
                                         String execution, Map<String, String> cookies) throws IOException {
        String casLoginUrl = "https://ids.xsyu.edu.cn/authserver/login";

        HttpURLConnection connection = (HttpURLConnection) new URL(casLoginUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);

        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        if (!cookies.isEmpty()) {
            connection.setRequestProperty("Cookie", buildCookieHeader(cookies));
        }

        String serviceUrl = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin";
        String requestBody = "username=" + URLEncoder.encode(username, "UTF-8") +
                "&password=" + URLEncoder.encode(password, "UTF-8") +
                "&execution=" + URLEncoder.encode(execution, "UTF-8") +
                "&_eventId=submit" +
                "&loginType=1" +
                "&rememberMe=true" +
                "&service=" + URLEncoder.encode(serviceUrl, "UTF-8");

        System.out.println("提交CAS登录请求体");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes("UTF-8"));
        }

        processCookies(connection, cookies);

        int responseCode = connection.getResponseCode();
        System.out.println("CAS登录响应码: " + responseCode);

        if (responseCode == 302) {
            String location = connection.getHeaderField("Location");
            System.out.println("CAS登录后重定向到: " + location);

            if (location != null && location.contains("ticket=")) {
                String[] parts = location.split("ticket=");
                if (parts.length > 1) {
                    String ticket = parts[1];
                    if (ticket.contains("&")) {
                        ticket = ticket.split("&")[0];
                    }
                    return ticket;
                }
            }
        }

        return null;
    }

    private static String getJwsessionWithTicket(String ticket, Map<String, String> cookies) throws IOException {
        // 使用ticket访问gwxg系统
        String ticketUrl = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin" +
                "?ticket=" + URLEncoder.encode(ticket, "UTF-8");

        System.out.println("使用ticket访问URL: " + ticketUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(ticketUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        if (!cookies.isEmpty()) {
            connection.setRequestProperty("Cookie", buildCookieHeader(cookies));
        }

        int responseCode = connection.getResponseCode();
        System.out.println("ticket验证响应码: " + responseCode);

        // 处理cookies
        processCookies(connection, cookies);

        // 检查是否已经获取到JWSESSION
        if (cookies.containsKey("JWSESSION")) {
            return cookies.get("JWSESSION");
        }

        // 如果是重定向，处理重定向链
        if (responseCode == 302) {
            String location = connection.getHeaderField("Location");
            System.out.println("第一次重定向到: " + location);

            // 跟随重定向链，直到获取JWSESSION或重定向结束
            return followRedirectChain(location, cookies, 0);
        }

        return null;
    }

    private static String followRedirectChain(String redirectUrl, Map<String, String> cookies, int depth) throws IOException {
        if (depth > 5) {
            System.out.println("重定向链过长，停止跟随");
            return null;
        }

        System.out.println("跟随重定向 [" + depth + "]: " + redirectUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        if (!cookies.isEmpty()) {
            connection.setRequestProperty("Cookie", buildCookieHeader(cookies));
        }

        processCookies(connection, cookies);

        // 检查是否获取到JWSESSION
        if (cookies.containsKey("JWSESSION")) {
            System.out.println("成功获取JWSESSION!");
            return cookies.get("JWSESSION");
        }

        int responseCode = connection.getResponseCode();
        System.out.println("重定向响应码 [" + depth + "]: " + responseCode);

        // 打印所有响应头用于调试
        Map<String, List<String>> headers = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (header.getKey() != null) {
                System.out.println(header.getKey() + ": " + header.getValue());
            }
        }

        if (responseCode == 302) {
            String nextLocation = connection.getHeaderField("Location");
            if (nextLocation != null) {
                return followRedirectChain(nextLocation, cookies, depth + 1);
            }
        } else if (responseCode == 200) {
            // 如果是200响应，可能已经到达目标页面，检查cookies
            System.out.println("到达目标页面，检查cookies...");
            if (cookies.containsKey("JWSESSION")) {
                return cookies.get("JWSESSION");
            }

            // 如果没有JWSESSION，尝试访问首页获取
            return tryAccessHomePage(cookies);
        }

        return null;
    }

    private static String tryAccessHomePage(Map<String, String> cookies) throws IOException {
        // 尝试访问gwxg系统的首页来获取JWSESSION
        String homeUrl = "https://gwxg.xsyu.edu.cn/h5/mobile/basicinfo/index";

        System.out.println("尝试访问首页: " + homeUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(homeUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        if (!cookies.isEmpty()) {
            connection.setRequestProperty("Cookie", buildCookieHeader(cookies));
        }

        processCookies(connection, cookies);

        if (cookies.containsKey("JWSESSION")) {
            System.out.println("通过首页获取到JWSESSION!");
            return cookies.get("JWSESSION");
        }

        return null;
    }

    private static String extractExecution(String html) {
        String[] patterns = {
                "name=\"execution\" value=\"([^\"]+)\"",
                "execution\" value=\"([^\"]+)\""
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(html);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private static void processCookies(HttpURLConnection connection, Map<String, String> cookies) {
        Map<String, List<String>> headers = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if ("Set-Cookie".equalsIgnoreCase(header.getKey())) {
                for (String cookie : header.getValue()) {
                    System.out.println("收到Cookie: " + cookie);
                    parseCookie(cookie, cookies);
                }
            }
        }

        // 打印当前所有cookies
        System.out.println("当前cookies: " + cookies.keySet());
    }

    private static void parseCookie(String cookieHeader, Map<String, String> cookies) {
        String[] cookiePairs = cookieHeader.split(";");
        for (String cookiePair : cookiePairs) {
            String[] parts = cookiePair.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                cookies.put(key, value);
                System.out.println("保存Cookie: " + key + "=" + value);
            }
        }
    }

    private static String buildCookieHeader(Map<String, String> cookies) {
        StringBuilder header = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (header.length() > 0) {
                header.append("; ");
            }
            header.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return header.toString();
    }
}