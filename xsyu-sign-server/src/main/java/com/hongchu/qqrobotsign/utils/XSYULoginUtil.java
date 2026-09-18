package com.hongchu.qqrobotsign.utils;

import com.hongchu.qqrobotsign.enums.CasErrorType;
import com.hongchu.qqrobotsign.pojo.entity.CasCaptchaState;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XSYULoginUtil {

    private static final Logger log = LoggerFactory.getLogger(XSYULoginUtil.class);

    private static final String SERVICE_CAS_LOGIN = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin";
    private static final String CAS_LOGIN_URL = "https://ids.xsyu.edu.cn/authserver/login";
    private static final String CAS_HOST = "ids.xsyu.edu.cn";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private static final Pattern EXECUTION_PATTERN = Pattern.compile("name=\"execution\"\\s+value=\"([^\"]+)\"");
    private static final Pattern EXECUTION_PATTERN2 = Pattern.compile("execution\"\\s+value=\"([^\"]+)\"");
    private static final Pattern TICKET_PATTERN = Pattern.compile("ticket=([^\"&\\s]+)");
    private static final Pattern CAPTCHA_IMG_PATTERN =
            Pattern.compile("<img[^>]*src=[\"']([^\"']*(?:captcha|captchaImage|validateCode)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_MSG_PATTERN = Pattern.compile(
            "<span\\b(?=[^>]*\\bid\\s*=\\s*[\"']msg1?[\"'])[^>]*>(.*?)</span>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FALLBACK_ERROR_MSG_PATTERN = Pattern.compile(
            "<(?:div|span)\\b(?=[^>]*\\bclass\\s*=\\s*[\"'][^\"']*(?:error|errors|alert|msg)[^\"']*[\"'])[^>]*>(.*?)</(?:div|span)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private static void applyBrowserHeaders(HttpURLConnection conn, String referer) {
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        conn.setRequestProperty("Accept-Encoding", "identity");
        conn.setRequestProperty("Connection", "keep-alive");
        if (referer != null && !referer.isEmpty()) {
            conn.setRequestProperty("Referer", referer);
        }
    }

    // ==================== Cookie helpers ====================

    private static void captureCookies(HttpURLConnection conn, Map<String, String> cookies) {
        // 注意：学校服务器(Envoy代理)返回的响应头可能全小写(set-cookie)，
        // getHeaderFields().get("Set-Cookie") 大小写敏感会取不到，必须遍历忽略大小写
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            if (entry.getKey() == null || !"Set-Cookie".equalsIgnoreCase(entry.getKey())) continue;
            for (String header : entry.getValue()) {
                int semi = header.indexOf(';');
                String nv = semi > 0 ? header.substring(0, semi) : header;
                int eq = nv.indexOf('=');
                if (eq > 0) {
                    cookies.put(nv.substring(0, eq).trim(), nv.substring(eq + 1).trim());
                }
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

    public static List<CasCaptchaState.CookieEntry> toCookieEntries(Map<String, String> cookies) {
        if (cookies == null) return Collections.emptyList();
        return cookies.entrySet().stream()
                .map(e -> new CasCaptchaState.CookieEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public static Map<String, String> toCookieMap(List<CasCaptchaState.CookieEntry> entries) {
        Map<String, String> map = new LinkedHashMap<>();
        if (entries != null) {
            for (CasCaptchaState.CookieEntry e : entries) {
                map.put(e.getName(), e.getValue());
            }
        }
        return map;
    }

    // ==================== Public API ====================

    /** 首次登录，可能返回CAPTCHA_REQUIRED */
    public static CasLoginResult login(String username, String password) {
        Map<String, String> cookies = new LinkedHashMap<>();
        try {
            log.info("CAS登录开始 - 用户: {}", username);
            CasLoginResult result = doLogin(username, password, cookies);
            if (result.getErrorType() == CasErrorType.SUCCESS) {
                log.info("CAS登录成功 - 用户: {}", username);
            } else {
                log.warn("CAS登录失败 - 用户: {}, 原因: {}", username, result.getErrorType());
            }
            return result;
        } catch (Exception e) {
            log.error("CAS登录异常 - 用户: {}", username, e.getMessage(), e);
            return CasLoginResult.error(CasErrorType.OTHER_ERROR, "CAS登录连接异常: " + e.getMessage());
        }
    }

    /** 带验证码的登录，使用之前保存的cookies和execution */
    public static CasLoginResult loginWithCaptcha(
            String username, String password,
            String captchaCode, String execution,
            Map<String, String> cookies) {
        try {
            return submitCasLogin(username, password, execution, cookies, captchaCode);
        } catch (Exception e) {
            log.error("CAS验证码登录异常 - 用户: {} - {}", username, e.getMessage(), e);
            return CasLoginResult.error(CasErrorType.OTHER_ERROR, "验证码登录失败: " + e.getMessage());
        }
    }

    // ==================== Internal login flow ====================

    private static CasLoginResult doLogin(String username, String password, Map<String, String> cookies) throws IOException, InterruptedException {
        String casLoginUrl = getCasLoginUrl(cookies);
        if (casLoginUrl == null) {
            return CasLoginResult.error(CasErrorType.OTHER_ERROR, "无法获取CAS登录URL");
        }
        log.debug("CAS登录URL: {}", casLoginUrl);

        String execution = getExecutionFromCas(casLoginUrl, cookies);
        if (execution == null) {
            return CasLoginResult.error(CasErrorType.OTHER_ERROR, "无法获取execution参数");
        }
        log.debug("获取到execution: {}", execution);

        Thread.sleep(800 + (long)(Math.random() * 400));

        return submitCasLogin(username, password, execution, cookies, null);
    }

    public static String getCasLoginUrl(Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = open(SERVICE_CAS_LOGIN, false);
        applyCookies(conn, cookies);
        int code = conn.getResponseCode();
        captureCookies(conn, cookies);
        log.debug("初始访问响应码: {}", code);
        if (code == 302) {
            String location = conn.getHeaderField("Location");
            log.debug("重定向到: {}", location);
            return location;
        }
        log.warn("初始访问未返回302重定向，响应码: {}", code);
        return null;
    }

    private static String getExecutionFromCas(String casUrl, Map<String, String> cookies) throws IOException {
        return extractExecution(fetchPage(casUrl, cookies));
    }

    private static String fetchPage(String url, Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = open(url, false);
        applyCookies(conn, cookies);
        String html = readBody(conn);
        captureCookies(conn, cookies);
        return html;
    }

    private static CasLoginResult submitCasLogin(
            String username, String password, String execution,
            Map<String, String> cookies, String captchaCode) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) new URL(CAS_LOGIN_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Origin", "https://" + CAS_HOST);
        conn.setRequestProperty("Referer", "https://" + CAS_HOST + "/authserver/login?service=" + URLEncoder.encode(SERVICE_CAS_LOGIN, StandardCharsets.UTF_8));
        conn.setRequestProperty("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"");
        conn.setRequestProperty("Sec-Ch-Ua-Mobile", "?0");
        conn.setRequestProperty("Sec-Ch-Ua-Platform", "\"Windows\"");
        conn.setRequestProperty("Sec-Fetch-Dest", "document");
        conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
        conn.setRequestProperty("Sec-Fetch-Site", "same-origin");
        conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
        applyCookies(conn, cookies);

        StringBuilder body = new StringBuilder();
        body.append("username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));
        body.append("&password=").append(URLEncoder.encode(password, StandardCharsets.UTF_8));
        body.append("&execution=").append(URLEncoder.encode(execution, StandardCharsets.UTF_8));
        body.append("&_eventId=submit");
        body.append("&loginType=1");
        body.append("&rememberMe=true");
        if (captchaCode != null && !captchaCode.isEmpty()) {
            body.append("&authcode=").append(URLEncoder.encode(captchaCode, StandardCharsets.UTF_8));
        }
        body.append("&service=").append(URLEncoder.encode(SERVICE_CAS_LOGIN, StandardCharsets.UTF_8));

        log.debug("提交CAS登录表单 - 用户: {}, hasCaptcha: {}", username, captchaCode != null);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        captureCookies(conn, cookies);
        log.debug("CAS登录响应码: {}", code);

        if (code == 302) {
            String location = conn.getHeaderField("Location");
            log.debug("CAS登录后重定向到: {}", location);
            String ticket = extractTicket(location);
            if (ticket != null) {
                String jws = getJWSessionWithTicket(ticket, cookies);
                if (jws != null) {
                    return CasLoginResult.success(jws);
                }
                return CasLoginResult.error(CasErrorType.OTHER_ERROR, "获取JWSESSION失败");
            }
            // 302 but no ticket - follow location as redirect chain
            if (location != null) {
                String jws = followRedirect(location, 0, cookies);
                if (jws != null) {
                    return CasLoginResult.success(jws);
                }
            }
            return CasLoginResult.error(CasErrorType.OTHER_ERROR, "重定向后未获取到JWSESSION");
        }

        if (code == 200) {
            String html = readBody(conn);
            log.debug("CAS返回200，响应长度: {}", html.length());

            CasErrorType errorType = diagnoseError(html);
            String errorMsg = extractErrorMessage(html);

            log.info("CAS登录诊断 - 用户: {}, errorType: {}, html前300字符: {}",
                    username, errorType,
                    html.substring(0, Math.min(300, html.length())));

            if (errorType == CasErrorType.CAPTCHA_REQUIRED) {
                // Extract captcha image
                String captchaImageBase64 = extractCaptchaImageBase64(html, cookies);
                // Re-extract execution from captcha page (may have been refreshed)
                String newExecution = extractExecution(html);
                if (newExecution == null) newExecution = execution;

                CasCaptchaState state = new CasCaptchaState();
                state.setUsername(username);
                state.setExecution(newExecution);
                state.setCookies(toCookieEntries(cookies));
                state.setCreatedAt(System.currentTimeMillis());

                return CasLoginResult.captchaRequired(state, captchaImageBase64);
            }

            return CasLoginResult.error(errorType, errorMsg != null ? errorMsg : "CAS登录失败");
        }

        return CasLoginResult.error(CasErrorType.OTHER_ERROR, "CAS登录返回异常状态码: " + code);
    }

    // ==================== Error diagnosis ====================

    private static CasErrorType diagnoseError(String html) {
        String msg = extractErrorMessage(html);

        // 账号状态只能根据CAS明确返回的错误文本判断。登录页脚本和控件本身也可能包含disabled等字样，
        // 扫描整页会把普通密码错误误判为账号锁定。
        if (msg != null && containsAny(msg, "账号被锁定", "锁定", "冻结", "locked", "disabled")) {
            return CasErrorType.ACCOUNT_LOCKED;
        }
        // 所有页面（含注释）都有“验证码”字样，必须使用真实的authcode字段判断。
        if (html.contains("authcode")) {
            return CasErrorType.CAPTCHA_REQUIRED;
        }
        if (msg != null && containsAny(msg,
                "账号或密码错误", "用户名或密码错误", "密码错误",
                "不正确", "不存在", "无效", "invalid")) {
            return CasErrorType.WRONG_PASSWORD;
        }
        return CasErrorType.OTHER_ERROR;
    }

    private static boolean containsAny(String html, String... keywords) {
        for (String kw : keywords) {
            if (html.contains(kw)) return true;
        }
        return false;
    }

    private static String extractErrorMessage(String html) {
        Matcher m = ERROR_MSG_PATTERN.matcher(html);
        if (m.find()) return normalizeErrorMessage(m.group(1));

        // 兼容其他CAS主题使用的通用错误容器。
        m = FALLBACK_ERROR_MSG_PATTERN.matcher(html);
        if (m.find()) return normalizeErrorMessage(m.group(1));

        return null;
    }

    private static String normalizeErrorMessage(String messageHtml) {
        String message = HTML_TAG_PATTERN.matcher(messageHtml).replaceAll("");
        message = message
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .trim();
        return message.isEmpty() ? null : message;
    }

    // ==================== Captcha extraction ====================

    private static String extractCaptchaImageBase64(String html, Map<String, String> cookies) {
        Matcher m = CAPTCHA_IMG_PATTERN.matcher(html);
        if (!m.find()) {
            log.warn("检测到验证码但未找到验证码图片URL");
            return null;
        }
        String imgSrc = m.group(1);
        log.debug("验证码图片URL: {}", imgSrc);

        String fullUrl;
        if (imgSrc.startsWith("http")) {
            fullUrl = imgSrc;
        } else if (imgSrc.startsWith("/")) {
            fullUrl = "https://" + CAS_HOST + imgSrc;
        } else {
            fullUrl = "https://" + CAS_HOST + "/authserver/" + imgSrc;
        }
        log.debug("验证码完整URL: {}", fullUrl);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", UA);
            applyCookies(conn, cookies);

            int code = conn.getResponseCode();
            captureCookies(conn, cookies);

            if (code != 200) {
                log.warn("验证码图片下载失败 - HTTP {}", code);
                return null;
            }

            String contentType = conn.getContentType();
            String mimeType = (contentType != null && contentType.contains("jpeg")) ? "image/jpeg" : "image/png";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = conn.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            }
            byte[] imageBytes = baos.toByteArray();
            log.debug("验证码图片下载成功 - 大小: {} bytes", imageBytes.length);

            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            log.error("验证码图片下载异常: {}", e.getMessage());
            return null;
        }
    }

    // ==================== JWSESSION retrieval ====================

    public static String getJWSessionWithTicket(String ticket, Map<String, String> cookies) throws IOException {
        String url = SERVICE_CAS_LOGIN + "?ticket=" +
                URLEncoder.encode(ticket, StandardCharsets.UTF_8);
        log.debug("使用ticket访问: {}", url);
        return followRedirect(url, 0, cookies);
    }

    private static String followRedirect(String url, int depth, Map<String, String> cookies) throws IOException {
        // gwxg 的 ticket 校验后可能弹回 CAS 再走一轮（首次访问 casLogin 常被弹回），
        // 实测正常链长约 5-8 跳，上限放宽到 12
        if (depth > 12) {
            log.warn("重定向链过长({})，停止跟随，最后地址: {}", depth, url);
            return null;
        }

        log.debug("{}: {}", depth == 0 ? "访问" : "跟随重定向[" + depth + "]", url);

        HttpURLConnection conn = open(url, false);
        applyCookies(conn, cookies);
        int code = conn.getResponseCode();
        captureCookies(conn, cookies);

        String jws = cookies.get("JWSESSION");
        if (jws != null && !jws.isEmpty()) {
            log.debug("成功获取JWSESSION，重定向深度: {}", depth);
            return jws;
        }

        log.debug("响应码[{}]: {}", depth, code);

        if (code == 302) {
            String location = conn.getHeaderField("Location");
            if (location != null) return followRedirect(location, depth + 1, cookies);
        }

        return null;
    }

    // ==================== 会话与页面 ====================

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String CAS_LOGIN_PAGE_URL =
            CAS_LOGIN_URL + "?service=" + URLEncoder.encode(SERVICE_CAS_LOGIN, StandardCharsets.UTF_8);

    private static final Pattern LOGIN_LT_PATTERN = Pattern.compile("generateQRCode\\?loginLT=([^\"&]+)");

    /** 建立 CAS 会话：302 拿登录页地址 → 拉取页面拿 SESSION cookie/execution/loginLT */
    public static CasPage openCasPage(Map<String, String> cookies) throws IOException {
        String casLoginUrl = getCasLoginUrl(cookies);
        if (casLoginUrl == null) {
            throw new IOException("无法获取CAS登录URL(gwxg未返回302)");
        }
        String html = fetchPage(casLoginUrl, cookies);
        CasPage page = new CasPage();
        page.setLoginUrl(casLoginUrl);
        page.setHtml(html);
        page.setExecution(extractExecution(html));
        page.setLoginLt(extractLoginLt(html));
        return page;
    }

    private static String extractLoginLt(String html) {
        Matcher m = LOGIN_LT_PATTERN.matcher(html);
        return m.find() ? m.group(1) : null;
    }

    // ==================== 短信登录 ====================

    /** 发送短信验证码。成功返回 null，失败返回错误信息 */
    public static String sendSmsCode(String phone, Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(
                "https://" + CAS_HOST + "/authserver/smsLogin/sendSms").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setRequestProperty("Referer", CAS_LOGIN_PAGE_URL);
        applyCookies(conn, cookies);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(("request_username=" + URLEncoder.encode(phone, StandardCharsets.UTF_8))
                    .getBytes(StandardCharsets.UTF_8));
        }
        captureCookies(conn, cookies);
        String body = readBody(conn);
        log.info("sendSms 原始响应: {}", body);
        try {
            JsonNode root = JSON.readTree(body);
            if (root.has("success") && root.get("success").asBoolean()) {
                return null;
            }
            return root.has("errormsg") ? root.get("errormsg").asText() : ("发送失败: " + body);
        } catch (Exception e) {
            log.warn("sendSms 响应解析失败: {}", body);
            return "发送失败: " + body;
        }
    }

    /** 校验短信验证码并识别账号。multiple=true 表示手机号绑定多个账号 */
    public static SmsValidResult smsValid(String phone, String smsCode, Map<String, String> cookies) throws IOException {
        String url = "https://" + CAS_HOST + "/authserver/maccountNoLoginValid/smsValid?phone=" +
                URLEncoder.encode(phone, StandardCharsets.UTF_8) + "&smsCode=" +
                URLEncoder.encode(smsCode, StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setRequestProperty("Referer", CAS_LOGIN_PAGE_URL);
        applyCookies(conn, cookies);
        String body = readBody(conn);
        captureCookies(conn, cookies);
        log.info("smsValid 原始响应: {}", body);

        SmsValidResult result = new SmsValidResult();
        try {
            JsonNode root = JSON.readTree(body);
            result.setCode(root.has("code") ? root.get("code").asInt() : -1);
            result.setMsg(root.has("msg") ? root.get("msg").asText() : null);
            result.setMultiple(root.has("isExitMultipleAccount") && root.get("isExitMultipleAccount").asBoolean());
            JsonNode data = root.get("data");
            if (data != null && data.isArray()) {
                for (JsonNode item : data) {
                    SmsAccount account = new SmsAccount();
                    account.setCode(item.has("code") ? item.get("code").asText() : null);
                    account.setName(item.has("name") ? item.get("name").asText() : null);
                    account.setCategory(item.has("category") ? item.get("category").asText() : null);
                    account.setDefault(item.has("isDefault") && item.get("isDefault").asBoolean());
                    result.getAccounts().add(account);
                }
            }
        } catch (Exception e) {
            log.warn("smsValid 响应解析失败: {}", body);
            result.setMsg("响应解析失败: " + body);
        }
        return result;
    }

    /** 多账号时选择登录账号。成功返回 null，失败返回错误信息 */
    public static String accountValid(String loginName, boolean isDefault, Map<String, String> cookies) throws IOException {
        String url = "https://" + CAS_HOST + "/authserver/maccountNoLoginValid/accountValid?loginName=" +
                URLEncoder.encode(loginName, StandardCharsets.UTF_8) + "&isDefault=" + isDefault;
        HttpURLConnection conn = open(url, false);
        applyCookies(conn, cookies);
        String body = readBody(conn);
        captureCookies(conn, cookies);
        try {
            JsonNode root = JSON.readTree(body);
            // 前端逻辑: code==0 表示失败(展示msg), 非0 表示成功(重新加载页面)
            if (root.has("code") && root.get("code").asInt() != 0) {
                return null;
            }
            return root.has("msg") ? root.get("msg").asText() : ("选择账号失败: " + body);
        } catch (Exception e) {
            log.warn("accountValid 响应解析失败: {}", body);
            return "选择账号失败: " + body;
        }
    }

    // ==================== 扫码登录 ====================

    /** 下载登录二维码图片 */
    public static byte[] generateQrImage(String loginLT, Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(
                "https://" + CAS_HOST + "/authserver/generateQRCode?loginLT=" +
                        URLEncoder.encode(loginLT, StandardCharsets.UTF_8)).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        applyCookies(conn, cookies);
        int code = conn.getResponseCode();
        captureCookies(conn, cookies);
        if (code != 200) {
            throw new IOException("二维码下载失败 HTTP " + code);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = conn.getInputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    /** 轮询扫码状态。返回 "success" 表示已确认，"" 表示等待中 */
    public static String pollAnalogLogin(Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(
                "https://" + CAS_HOST + "/authserver/analogLogin").openConnection();
        conn.setRequestMethod("POST");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        conn.setRequestProperty("Referer", CAS_LOGIN_PAGE_URL);
        applyCookies(conn, cookies);
        captureCookies(conn, cookies);
        String body = readBody(conn);
        return body == null ? "" : body.trim();
    }

    // ==================== 认证后取 ticket ====================

    /** 短信/扫码认证建立会话后，重载登录页拿 ticket（未就绪返回 null） */
    public static String getTicketAfterAuth(Map<String, String> cookies) throws IOException {
        HttpURLConnection conn = open(CAS_LOGIN_PAGE_URL, false);
        applyCookies(conn, cookies);
        int code = conn.getResponseCode();
        captureCookies(conn, cookies);
        if (code == 302) {
            return extractTicket(conn.getHeaderField("Location"));
        }
        if (code == 200) {
            return extractTicket(readBody(conn));
        }
        return null;
    }

    /** 完整走完 ticket → JWSESSION 链 */
    public static String completeTicketFlow(String ticket, Map<String, String> cookies) throws IOException {
        return getJWSessionWithTicket(ticket, cookies);
    }

    // ==================== 结果模型 ====================

    public static class CasPage {
        private String loginUrl;
        private String html;
        private String execution;
        private String loginLt;

        public String getLoginUrl() { return loginUrl; }
        public void setLoginUrl(String loginUrl) { this.loginUrl = loginUrl; }
        public String getHtml() { return html; }
        public void setHtml(String html) { this.html = html; }
        public String getExecution() { return execution; }
        public void setExecution(String execution) { this.execution = execution; }
        public String getLoginLt() { return loginLt; }
        public void setLoginLt(String loginLt) { this.loginLt = loginLt; }
    }

    public static class SmsValidResult {
        private int code = -1;
        private String msg;
        private boolean multiple;
        private final List<SmsAccount> accounts = new ArrayList<>();

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public boolean isMultiple() { return multiple; }
        public void setMultiple(boolean multiple) { this.multiple = multiple; }
        public List<SmsAccount> getAccounts() { return accounts; }
    }

    public static class SmsAccount {
        private String code;
        private String name;
        private String category;
        private boolean isDefault;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    }

    // ==================== Utility methods ====================

    private static HttpURLConnection open(String url, boolean followRedirects) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(followRedirects);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        applyBrowserHeaders(conn, url.contains("authserver") ? null : "https://" + CAS_HOST + "/");
        return conn;
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

    private static String extractExecution(String html) {
        Matcher m = EXECUTION_PATTERN.matcher(html);
        if (m.find()) return m.group(1);
        m = EXECUTION_PATTERN2.matcher(html);
        if (m.find()) return m.group(1);
        return null;
    }

    private static String extractTicket(String text) {
        if (text == null) return null;
        Matcher m = TICKET_PATTERN.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入学号：");
        String username = scanner.nextLine();
        System.out.print("请输入密码：");
        String password = scanner.nextLine();
        CasLoginResult result = login(username, password);
        if (result.getErrorType() == CasErrorType.SUCCESS) {
            System.out.println("登录成功！");
            System.out.println("JWSESSION: " + result.getJwsession());
        } else {
            System.out.println("登录失败！原因: " + result.getErrorType() + " - " + result.getErrorMessage());
        }
    }
}
