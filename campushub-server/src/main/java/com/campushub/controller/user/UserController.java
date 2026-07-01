package com.campushub.controller.user;

import com.campushub.common.Result;
import com.campushub.dto.UserLoginDTO;
import com.campushub.dto.UserRegisterDTO;
import com.campushub.service.user.UserService;
import com.campushub.vo.UserInfoVO;
import com.campushub.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 用户注册接口。
     * 当前阶段通过用户名密码完成账号注册。
     */
    @PostMapping("/register")
    public Result<Long> register(@RequestBody UserRegisterDTO registerDTO) {
        log.info("[User] register username={}", registerDTO.getUsername());
        return Result.success(userService.register(registerDTO));
    }

    /**
     * 用户登录接口。
     * 登录成功后返回 token 和基础用户信息。
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO loginDTO) {
        log.info("[User] login username={}", loginDTO.getUsername());
        return Result.success(userService.login(loginDTO));
    }

    /**
     * 当前登录用户信息接口。
     * 通过请求头中的 JWT 识别当前用户并返回其基础信息。
     */
    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentUser() {
        log.info("[User] me");
        return Result.success(userService.getCurrentUser());
    }
}
