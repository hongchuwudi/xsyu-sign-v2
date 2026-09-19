package com.hongchu.qqrobotsign.controller;

import com.hongchu.qqrobotsign.annotation.LogRecord;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.pojo.DTO.SignDTO;
import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IUserService;
import com.hongchu.qqrobotsign.service.SignService;
import com.hongchu.qqrobotsign.webClient.BaseSignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 签到管理
 *
 * @author hongchu
 * @since 2025-11-18
 */
@RestController
@RequestMapping("/sign")
@RequiredArgsConstructor
@Slf4j
public class SignController {
    @Autowired private SignService signService;
    @Autowired private BaseSignService baseSignService;
    @Autowired private IUserService userService;

    /**
     * 获取所有签到
     *
     * @param page 页码
     * @param size 每页数量
     * @return 签到结果
     */
    @RequestMapping("/allSign")
    public Result<List<SignItem>> getAllSign(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                             @RequestParam(value = "size", defaultValue = "10") Integer size) {
        log.info("controller层-获取所有签到-userId: {}", BaseContext.getCurrentId());
        User user = userService.getById(BaseContext.getCurrentId());
        if (user == null) throw new BusinessException("该用户不存在");
        Result<List<SignItem>> allSign = baseSignService.getAllSign(user.getUsername(), page, size);
        if (allSign == null) throw new BusinessException("空数据");
        return allSign;
    }

    /**
     * 获取单个签到
     *
     * @param signId   签到id
     * @param schoolId 学校id
     * @return 签到结果
     */
    @RequestMapping("/oneSign/{signId}/{schoolId}")
    public Result<SignItem> getOneSign(@PathVariable String signId, @PathVariable String schoolId) {
        log.info("controller层-获取单个签到-userId: {}, signId: {}, schoolId: {}", BaseContext.getCurrentId(), signId, schoolId);
        User user = userService.getById(BaseContext.getCurrentId());
        if (user == null) throw new BusinessException("该用户不存在");
        return baseSignService.getOneSign(user.getUsername(), signId, schoolId);
    }

    /**
     * 一键签到-username
     * @return 签到结果
     */
    @LogRecord("管理员为用户签到")
    @PostMapping("/all-admin/{username}")
    public String signByAdmin(@PathVariable String username) {
        log.info("controller层-一键签到-username: {}", username);
        return signService.signAll(username);
    }

    /**
     * 一键签到
     * @return 签到结果
     */
    @LogRecord("用户一键签到")
    @PostMapping("/all")
    public String sign() {
        User user = userService.getById(BaseContext.getCurrentId());
        if (user == null) throw new BusinessException("该用户不存在");
        String username = user.getUsername();
        log.info("controller层-签到-username: {}", username);
        return signService.signAll(username);
    }

    /**
     * 一键为所有用户进行签到
     */
    @PostMapping("/all-all")
    public void allSign() {
        log.info("controller层-一键为所有用户进行签到");
        signService.AllUserSignAll();
    }

    /**
     * 处理单个签到
     * @return 签到结果
     */
    @LogRecord("用户单项签到")
    @PostMapping("/one")
    public String signOne(@RequestParam(name = "id") String id,
                          @RequestParam(name = "signId") String signId,
                          @RequestParam(name = "schoolId") String schoolId,
                          @RequestParam(name = "signDTO", required = false) SignDTO signDTO) {
        log.info("controller层-处理单个签到-userId: {}, id: {}, signId: {}, schoolId: {}",
                BaseContext.getCurrentId(), id, signId, schoolId);
        User user = userService.getById(BaseContext.getCurrentId());
        if (user == null) throw new BusinessException("该用户不存在");
        String username = user.getUsername();
        SignItem signItem = SignItem.builder()
                .id(id)
                .signId(signId)
                .schoolId(schoolId)
                .build();
        return signService.processSingleSign(username, signItem, signDTO);
    }
}
