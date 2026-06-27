package com.campushub.controller.admin;

import com.campushub.common.Result;
import com.campushub.dto.UserLoginDTO;
import com.campushub.service.user.UserService;
import com.campushub.vo.UserInfoVO;
import com.campushub.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserService userService;

    /**
     * 管理员登录接口。
     * 登录成功后返回管理员 token，供后台接口统一放在 Authorization 中使用。
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO loginDTO) {
        return Result.success(userService.adminLogin(loginDTO));
    }

    /**
     * 当前登录管理员信息接口。
     * 便于后台页面在进入系统后读取当前管理员的基础信息。
     */
    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentAdmin() {
        return Result.success(userService.getCurrentAdmin());
    }
}
