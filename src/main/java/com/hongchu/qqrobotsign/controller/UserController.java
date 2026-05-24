package com.hongchu.qqrobotsign.controller;

import com.hongchu.qqrobotsign.annotation.LogRecord;
import com.hongchu.qqrobotsign.config.props.RSAConfig;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.pojo.DTO.LoginDTO;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
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
 * <p>
 * 用户表 前端控制器
 * </p>
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
    @Autowired private SignService SignService;
    @Autowired private RSAConfig rsaConfig;

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
     * 注册自签用户信息
     * @param loginDTO 登录参数（包含用户名和RSA加密后的密码）
     * @return 登录结果
     */
    @LogRecord("用户登录")
    @PostMapping("/login")
    public Result<UserLoginVO> register(@RequestBody LoginDTO loginDTO) throws InterruptedException {
        log.info("controller层-登录用户名：{}，密码长度：{}", loginDTO.getUsername(),
                loginDTO.getPsd() != null ? loginDTO.getPsd().length() : 0);
        // 插入用户信息
        UserLoginVO sucVO = userService.register(loginDTO.getUsername(), loginDTO.getPsd());
        if(sucVO == null) throw new BusinessException("登录失败请重试");
        return  Result.success(sucVO);
    }

    /**
     * 测试CAS登录接口
     * @param username 用户名
     * @param password 密码（明文）
     * @param code 验证码（固定值：hongchuiloveu）
     * @return CAS登录返回的JWS
     */
    @GetMapping("/test-login")
    public Result<String> testLogin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String code) {
        String jws = userService.testLogin(username, password, code);
        return Result.success(jws);
    }

    /**
     * 是否自动签到
     * @param isAuto true/false
     * @return 成功
     */
    @PutMapping("/auto-sign/{isAuto}")
    public Result<Void> setAutoSign(@PathVariable("isAuto") Boolean isAuto) {
        log.info("接收参数 - userId: {}, isAuto: {}, 类型: {}",
                BaseContext.getCurrentId(), isAuto, isAuto.getClass());
        userService.setAutoSign(isAuto);
        return Result.success();
    }

    /**
     * 设置签到日期
     * @param signDays 签到日期配置（0=周日，1=周一，...，6=周六）
     * @return 成功
     */
    @PutMapping("/sign-days")
    public Result<Void> setSignDays(@RequestBody UserDTO userDTO) {
        log.info("设置签到日期 - userId: {}, signDays: {}", BaseContext.getCurrentId(), userDTO.getSignDays());
        userService.setInfo(userDTO);
        return Result.success();
    }

    /**
     * 退出登录（不删除数据库）
     * @return 退出登录结果
     */
    @LogRecord("用户退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("controller层-退出登录-userId: {}", BaseContext.getCurrentId());
        userService.logout();
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
}