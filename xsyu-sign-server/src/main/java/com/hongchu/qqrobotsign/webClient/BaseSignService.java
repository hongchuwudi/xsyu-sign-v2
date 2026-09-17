package com.hongchu.qqrobotsign.webClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hongchu.qqrobotsign.config.props.SignInfoConfig;
import com.hongchu.qqrobotsign.config.props.UrlConfig;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.pojo.DTO.SignDTO;
import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.result.res.SignDetailResponse;
import com.hongchu.qqrobotsign.result.res.SignListResponse;
import com.hongchu.qqrobotsign.result.res.SignResultResponse;
import com.hongchu.qqrobotsign.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@Slf4j
public class BaseSignService {
    @Autowired @Qualifier("mobileWebClient") private WebClient mobileWebClient;
    @Autowired private IUserService userService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UrlConfig urlConfig;
    @Autowired private SignInfoConfig signInfoConfig;

    /**
     * 获取所有签到
     * @param username 用户名
     * @param page 页码
     * @param size 每页数量
     */
    public Result<List<SignItem>> getAllSign(String username, Integer page, Integer size){
        log.info("webclient层-获取所有签到-username: {}", username);
        // 获取用户JWS
        String userJws = getUserJws(username);

        // 使用UriComponentsBuilder构建完整URL
        String url = UriComponentsBuilder.fromUriString(urlConfig.getFullGetAllSignUrl())
                .queryParam("page", page)
                .queryParam("size", size)
                .build(false) // false表示完全不编码
                .toUriString();

        String response = mobileWebClient.get()
                .uri(url)
                .header("JWSESSION", userJws)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // 解析响应
        SignListResponse signResponse;
        try{
            signResponse = objectMapper.readValue(response, SignListResponse.class);
            if(signResponse.getMessage() != null && !signResponse.getMessage().isEmpty())
//                return Result.fail(signResponse.getMessage());
                throw new BusinessException("webClient层异常:" + signResponse.getMessage());
        }catch (JsonProcessingException e){
            return Result.fail("解析响应失败");
        }
        log.info("webclient层-获取所有签到-signResponse: {}", signResponse);
        return Result.success(signResponse.getData());
    }

    /**
     * 获取单个签到
     * @param username 用户名
     * @param signId 签到ID (字符串类型)
     * @param schoolId 学校ID (字符串类型)
     * @return 签到信息
     */
    public Result<SignItem> getOneSign(String username, String signId, String schoolId){
        log.info("webclient层-获取单个签到-username: {}, signId: {}, schoolId: {}",
                username, signId, schoolId);
        // 获取用户JWS
        String userJws = getUserJws(username);

        // 使用UriComponentsBuilder构建完整URL
        String url = UriComponentsBuilder.fromUriString(urlConfig.getFullGetOneSignUrl())
                .queryParam("signId", signId)
                .queryParam("schoolId", schoolId)
                .build(false) // false表示完全不编码
                .toUriString();

        String response = mobileWebClient.get()
                .uri(url)
                .header("JWSESSION", userJws)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // 解析响应
        SignDetailResponse detailResponse;
        try {
            detailResponse = objectMapper.readValue(response, SignDetailResponse.class);
            if(detailResponse.getMessage() != null && !detailResponse.getMessage().isEmpty())
//                return Result.fail(detailResponse.getMessage());
                throw new BusinessException("webClient层异常:" + detailResponse.getMessage());
        }catch (JsonProcessingException e){
            return Result.fail("解析响应失败");
        }
        log.info("webclient层-获取单个签到-detailResponse: {}", detailResponse);
        return Result.success(detailResponse.getData());
    }

    /**
     * 核心签到
     * @param username 用户名
     * @param id 记录ID (字符串类型)
     * @param signId 签到ID (字符串类型)
     * @param schoolId 学校ID (字符串类型)
     * @param signDTO 签到数据
     * @return 签到结果
     */
    public Result<String> sign(String username, String id, String signId, String schoolId, SignDTO signDTO)  {
        log.info("webclient层-签到-username: {}, id: {}, signId: {}, schoolId: {}",
                username, id, signId, schoolId);
        // 获取用户JWS
        String userJws = getUserJws(username);
        // 默认使用鄠邑签到
        if(signDTO == null) signDTO = SignDTO.builder()
                .inArea(signInfoConfig.getInArea())
                .areaJSON(signInfoConfig.getAreaJson())
                .latitude(signInfoConfig.getLatitude())
                .longitude(signInfoConfig.getLongitude())
                .build();

        // 使用UriComponentsBuilder构建完整URL
        String url = UriComponentsBuilder.fromUriString(urlConfig.getFullSignUrl())
                .queryParam("id", id)
                .queryParam("signId", signId)
                .queryParam("schoolId", schoolId)
                .build(false) // false表示完全不编码
                .toUriString();

        // 发送POST请求
        String response = mobileWebClient.post()
                .uri(url)
                .header("JWSESSION", userJws)
                .bodyValue(signDTO)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info("webclient层-签到-resultResponse: {}", response);

        // 解析响应
        SignResultResponse resultResponse;
        try {
            resultResponse = objectMapper.readValue(response, SignResultResponse.class);
            if(resultResponse.getMessage() != null && !resultResponse.getMessage().isEmpty())
                throw new BusinessException("webClient层异常:" + resultResponse.getMessage());
        } catch (JsonProcessingException e) {
            return Result.fail("解析响应失败");
        }
        return Result.success(resultResponse.getData());
    }

    /**
     * 获取当前 JWSESSION 对应的用户信息（学号/姓名/手机号）
     * 用于绑定后校验会话归属
     */
    public GwxgUserInfo getCurrentUserInfo(String jws) {
        String url = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/my/index";
        String response = mobileWebClient.post()
                .uri(url)
                .header("JWSESSION", jws)
                .header("Content-Type", "application/json")
                .bodyValue("{}")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.has("code") && root.get("code").asInt() == 0 && root.has("data")) {
                JsonNode data = root.get("data");
                GwxgUserInfo info = new GwxgUserInfo();
                // 学号字段：实测为 username（同值 number），不是 code
                if (data.has("username") && !data.get("username").isNull()) {
                    info.setUsername(data.get("username").asText());
                } else if (data.has("number") && !data.get("number").isNull()) {
                    info.setUsername(data.get("number").asText());
                }
                info.setName(data.has("name") ? data.get("name").asText() : null);
                info.setPhone(data.has("phone") ? data.get("phone").asText() : null);
                return info;
            }
            log.warn("获取用户信息失败: {}", response);
            return null;
        } catch (Exception e) {
            log.error("解析用户信息响应失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 查询数据库中用户JWS
     * @param username 用户名
     * @return 用户JWS
     */
    private String getUserJws(String username) {
        User user = userService.query().eq("username", username).one();
        if (user == null) {
            log.error("用户不存在: {}", username);
            throw new BusinessException("用户不存在");
        }

        String jws = user.getJws();
        if (jws == null || jws.trim().isEmpty()) {
            log.error("用户JWSESSION为空: {}", username);
            throw new BusinessException("用户存在，但JWSESSION为空");
        }
        return jws;
    }
}