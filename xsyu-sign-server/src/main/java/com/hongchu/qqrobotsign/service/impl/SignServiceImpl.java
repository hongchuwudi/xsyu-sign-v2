package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hongchu.qqrobotsign.config.props.AdminConfig;
import com.hongchu.qqrobotsign.config.props.SignInfoConfig;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.pojo.DTO.SignDTO;
import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.EmailService;
import com.hongchu.qqrobotsign.service.SignService;
import com.hongchu.qqrobotsign.webClient.BaseSignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class SignServiceImpl implements SignService {
    @Autowired private BaseSignService baseSignService;
    @Autowired private UserServiceImpl userService;
    @Autowired private SignInfoConfig signInfoConfig;
    @Autowired private EmailService emailService;
    @Autowired private AdminConfig adminConfig;
    /**
     * 一键签到所有未签到的签到
     *
     * @param username 用户名
     * @return 签到结果
     */
    public String signAll(String username) {
        log.info("service层-一键签到-username: {}", username);

        // 1. 检查用户是否存在
        User user = userService.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) throw new BusinessException("用户不存在,请先登录");

        // 2. 获取最近10条签到
        Result<List<SignItem>> signResult = baseSignService.getAllSign(username, 1, 10);

        // 3. 检查JWS是否失效
        if (Objects.equals(signResult.getMessage(), "未登录,请重新登录")) {
            userService.refreshJws(username);
            signResult = baseSignService.getAllSign(username, 1, 10);
        }

        // 4.如果刷新JWS后再次获取失败,直接返回错误
        if (!Objects.equals(signResult.getCode(), 200))
            return "获取签到列表失败" + signResult.getMessage();

        List<SignItem> signList = signResult.getData();

        // 5. 检查是否有未签到且未过期的签到项
        List<SignItem> unsignedItems = signList.stream()
                .filter(item ->
                        // 检查基础状态：未签到
                        item.getSignStatus() != null && item.getSignStatus() == 1 &&
                                item.getType() != null && item.getType() == 0 ||
                                // 检查结束时间：未过期（当前时间小于结束时间）
                                item.getEnd() != null && item.getEnd() > System.currentTimeMillis() &&
                                        // 可选：检查开始时间：已经开始（当前时间大于等于开始时间）
                                        (item.getStart() == null || item.getStart() <= System.currentTimeMillis())
                )
                .toList();

        log.info("service层-有{}个签到未完成", unsignedItems.size());
        if (unsignedItems.isEmpty()) return "最近10条签到已全部完成，无需签到";

        // 6. 执行签到
        List<String> signResults = new ArrayList<>();
        for (SignItem item : unsignedItems) {
//            SignDTO signBuild = SignDTO.builder().inArea(1)
//                    .latitude(item.getLatitude())
//                    .longitude(item.getLongitude())
//                    .areaJSON(signInfoConfig.getAreaJson())
//                    .build();
            // 执行签到-默认本校区
            String result = processSingleSign(username, item, null);
            signResults.add(result);

            // 短暂延迟
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 5. 返回结果
        return String.format("一键签到完成！共处理%d个签到：\n%s",
                unsignedItems.size(), String.join("\n", signResults));
    }

    /**
     * 处理单个签到
     *
     * @param username 用户名
     * @param signItem 签到项
     * @param signDTO  签到数据(不传采用默认配置)
     * @return 处理结果
     */
    @Override
    public String processSingleSign(String username, SignItem signItem, SignDTO signDTO) {
        User user = userService.getOne(new QueryWrapper<User>().eq("username", username));
        log.info("处理签到-username: {}, signItem: {}", user.getEmail(), signItem);
        try {
            // 执行签到
            Result<String> signResult = baseSignService.sign(
                    username,
                    signItem.getId(),
                    signItem.getSignId(),
                    signItem.getSchoolId(),
                    signDTO
            );

            // 检查签到结果
            // 未登录-刷新JWS-再次签到
            if (Objects.equals(signResult.getMessage(), "未登录,请重新登录")) {
                userService.refreshJws(username);
                signResult = baseSignService.sign(
                        username,
                        signItem.getId(),
                        signItem.getSignId(),
                        signItem.getSchoolId(),
                        signDTO
                );
            }

            // 返回结果
            if (signResult.getCode() == 200) {
                emailService.sendSignResultNotice(user.getEmail(), user.getUsername(), true, signResult.getData());
                return String.format("✅ %s - %s", signItem.getSignTitle(), signResult.getData());
            } else {
                emailService.sendSignResultNotice(user.getEmail(), user.getUsername(), false, signResult.getMessage());
                return String.format("❌ %s - %s", signItem.getSignTitle(), signResult.getMessage());
            }
        } catch (Exception e) {
            log.error("处理签到 {} 失败: {}", signItem.getSignTitle(), e.getMessage());
            return String.format("⚠️ %s - 异常: %s", signItem.getSignTitle(), e.getMessage());
        }
    }

    // 为所有用户处理一键签到
    public void AllUserSignAll() {
        log.info("service层-开始进行所有用户一键签到");
        // 1. 查询所有用户信息
        List<User> users = userService.list();
        // 移出管理员（role 为准，兼容旧数据用 adminConfig 用户名兜底）
        users.removeIf(user -> "ADMIN".equalsIgnoreCase(user.getRole())
                || adminConfig.getUsername().equalsIgnoreCase(user.getUsername()));
        // 2. 对每个用户进行续签工作
        users.forEach(user -> {
            try {
                String username = user.getUsername();
                log.info("service层-所有用户一键签到开始-username:{}", user.getUsername());
                this.signAll(username);
                log.info("service层-所有用户一键签到完成-username:{}", user.getUsername());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("service层-所有用户一键签到失败-username:{}", user.getUsername(),e);
                throw new RuntimeException(e);
            }
        });
    }
}
