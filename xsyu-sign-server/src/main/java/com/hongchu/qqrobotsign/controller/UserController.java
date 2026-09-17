package com.hongchu.qqrobotsign.controller;

import com.hongchu.qqrobotsign.annotation.LogRecord;
import com.hongchu.qqrobotsign.config.props.RSAConfig;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.pojo.DTO.BindPasswordDTO;
import com.hongchu.qqrobotsign.pojo.DTO.QrCreateDTO;
import com.hongchu.qqrobotsign.pojo.DTO.QrPollDTO;
import com.hongchu.qqrobotsign.pojo.DTO.SmsSendDTO;
import com.hongchu.qqrobotsign.pojo.DTO.SmsVerifyDTO;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.QrPollVO;
import com.hongchu.qqrobotsign.pojo.VO.QrSessionVO;
import com.hongchu.qqrobotsign.pojo.VO.UserLoginVO;
import com.hongchu.qqrobotsign.pojo.VO.UserVO;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IUserService;
import com.hongchu.qqrobotsign.service.SignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户认证与账户管理
 *
 * @author hongchu
 * @since 2025-11-17
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    @Autowired private IUserService userService;
    @Autowired private RSAConfig rsaConfig;

    /**
     * 获取用户信息
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getInfo(){
        log.info("controller层-获取信息-userId: {}", BaseContext.getCurrentId());
        UserVO myInfo = userService.getMyInfo();
        if(myInfo == null) throw new BusinessException("获取信息失败");
        return Result.success(myInfo);
    }

    /**
     * 获取RSA公钥（用于前端加密密码）
     * @return RSA公钥
     */
    @GetMapping("/public-key")
    public Result<Map<String, String>> getPublicKey() {
        log.info("controller层-获取RSA公钥");
        Map<String, String> result = new HashMap<>();
        result.put("publicKey", rsaConfig.getPublicKey());
        return Result.success(result);
    }

    /**
     * 学号+密码登录（登录页入口；管理员走本地密码校验；触发验证码时返回 1004；无账号自动创建）
     */
    @LogRecord("用户登录")
    @PostMapping("/xsy-login")
    public Result<UserLoginVO> xsyLogin(@RequestBody BindPasswordDTO dto) {
        log.info("controller层-学号密码登录-username: {}", dto.getUsername());
        UserLoginVO vo = userService.xsyPasswordLogin(dto.getUsername(), dto.getCasPsd(),
                dto.getCaptchaSessionId(), dto.getCaptchaCode());
        return Result.success(vo);
    }

    /**
     * 更新学校密码（用 CAS 密码验证当前账号；触发验证码时返回 1004）
     */
    @LogRecord("更新学校密码")
    @PostMapping("/bind/password")
    public Result<UserLoginVO> bindByPassword(@RequestBody BindPasswordDTO dto) {
        log.info("controller层-更新学校密码-userId: {}", BaseContext.getCurrentId());
        UserLoginVO vo = userService.bindByPassword(dto.getCasPsd(),
                dto.getCaptchaSessionId(), dto.getCaptchaCode());
        return Result.success(vo);
    }

    /**
     * 短信登录：发送验证码
     */
    @PostMapping("/sms/send")
    public Result<Map<String, String>> smsSend(@RequestBody SmsSendDTO dto) {
        log.info("controller层-短信登录发送验证码-phone: {}", dto.getPhone());
        String smsSessionId = userService.smsSendCode(dto.getPhone());
        Map<String, String> result = new HashMap<>();
        result.put("smsSessionId", smsSessionId);
        return Result.success(result);
    }

    /**
     * 短信登录：校验验证码并按学号匹配（无账号自动创建）
     */
    @LogRecord("用户登录")
    @PostMapping("/sms/login")
    public Result<UserLoginVO> smsLogin(@RequestBody SmsVerifyDTO dto) {
        log.info("controller层-短信登录-username: {}, phone: {}", dto.getUsername(), dto.getPhone());
        UserLoginVO vo = userService.smsLogin(dto.getSmsSessionId(), dto.getUsername(), dto.getPhone(), dto.getSmsCode());
        return Result.success(vo);
    }

    /**
     * 扫码登录：创建二维码会话（需先输入学号）
     */
    @PostMapping("/qr/create")
    public Result<QrSessionVO> qrCreate(@RequestBody QrCreateDTO dto) {
        log.info("controller层-扫码登录创建会话-username: {}", dto.getUsername());
        QrSessionVO vo = userService.qrCreate(dto.getUsername());
        return Result.success(vo);
    }

    /**
     * 扫码登录：轮询扫码状态（SUCCESS 时携带登录信息；校验学号归属；无账号自动创建）
     */
    @LogRecord("用户登录")
    @PostMapping("/qr/poll")
    public Result<QrPollVO> qrPoll(@RequestBody QrPollDTO dto) {
        QrPollVO vo = userService.qrPoll(dto.getQrSessionId());
        return Result.success(vo);
    }

    /**
     * 是否自动签到
     * @param isAuto true/false
     * @return 成功
     */
    @PutMapping("/auto-sign/{isAuto}")
    public Result<Void> setAutoSign(@PathVariable Boolean isAuto) {
        log.info("接收参数 - userId: {}, isAuto: {}, 类型: {}",
                BaseContext.getCurrentId(), isAuto, isAuto.getClass());
        userService.setAutoSign(isAuto);
        return Result.success();
    }

    /**
     * 设置签到日期
     * @param userDTO 签到日期配置（0=周日，1=周一，...，6=周六）
     * @return 成功
     */
    @PutMapping("/sign-days")
    public Result<Void> setSignDays(@RequestBody UserDTO userDTO) {
        // TODO 需要改进的接口
        log.info("设置签到日期 - userId: {}, signDays: {}", BaseContext.getCurrentId(), userDTO.getSignDays());
        userService.setInfo(userDTO);
        return Result.success();
    }

    /**
     * 注销信息（删除数据库）
     * @return 注销结果
     */
    @PostMapping("/unregister")
    public Result<Void> unregister() {
        log.info("controller层-注销信息-userId: {}", BaseContext.getCurrentId());
        String result = userService.removeLoginInfo();
        return Result.success();
    }

    /**
     * 修改用户信息
     * @param userDTO 修改信息
     * @return 操作成功
     */
    @PutMapping("/info/")
    public Result<Void> setInfo(UserDTO userDTO){
        log.info("controller层-修改信息-userId: {},UserDTO: {}", BaseContext.getCurrentId(),userDTO);
        userService.setInfo(userDTO);
        return Result.success();
    }


}
