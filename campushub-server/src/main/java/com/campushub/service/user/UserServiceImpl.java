package com.campushub.service.user;

import com.campushub.constant.DeleteStatusConstant;
import com.campushub.constant.JwtClaimsConstant;
import com.campushub.constant.UserRoleConstant;
import com.campushub.constant.UserStatusConstant;
import com.campushub.dto.UserLoginDTO;
import com.campushub.dto.UserRegisterDTO;
import com.campushub.entity.SysUser;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.UserMapper;
import com.campushub.utils.JwtTokenUtil;
import com.campushub.utils.UserContext;
import com.campushub.vo.UserInfoVO;
import com.campushub.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 用户注册。
     * 当前阶段通过用户名密码创建普通用户账号，不开放管理员注册入口。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterDTO registerDTO) {
        if (registerDTO.getUsername() == null || registerDTO.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (registerDTO.getPassword() == null || registerDTO.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        if (registerDTO.getRealName() == null || registerDTO.getRealName().isBlank()) {
            throw new BusinessException("真实姓名不能为空");
        }

        SysUser existedUser = userMapper.getByUsername(registerDTO.getUsername());
        if (existedUser != null) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(registerDTO.getPassword());
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setStudentNo(registerDTO.getStudentNo());
        user.setCreditScore(100);
        user.setRole(UserRoleConstant.USER);
        user.setStatus(UserStatusConstant.ENABLED);
        user.setIsDeleted(DeleteStatusConstant.NOT_DELETED);

        userMapper.saveUser(user);
        return user.getId();
    }

    /**
     * 普通用户登录。
     * 登录成功后返回普通用户 token，后续用户端接口都依赖这个 token 做身份识别。
     */
    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
        return doLogin(loginDTO, UserRoleConstant.USER);
    }

    /**
     * 管理员登录。
     * 只有角色为管理员的账号，才能从后台登录入口拿到管理员 token。
     */
    @Override
    public UserLoginVO adminLogin(UserLoginDTO loginDTO) {
        return doLogin(loginDTO, UserRoleConstant.ADMIN);
    }

    /**
     * 获取当前登录普通用户信息。
     * 这里会再次校验当前登录身份是不是普通用户，避免管理员 token 混用到用户接口。
     */
    @Override
    public UserInfoVO getCurrentUser() {
        return getCurrentUserByRole(UserRoleConstant.USER);
    }

    /**
     * 获取当前登录管理员信息。
     * 这里会再次校验当前登录身份是不是管理员，保证后台身份闭环完整。
     */
    @Override
    public UserInfoVO getCurrentAdmin() {
        return getCurrentUserByRole(UserRoleConstant.ADMIN);
    }

    /**
     * 统一处理登录校验。
     * 通过 expectedRole 区分本次是普通用户登录还是管理员登录，减少重复代码。
     */
    private UserLoginVO doLogin(UserLoginDTO loginDTO, Integer expectedRole) {
        if (loginDTO.getUsername() == null || loginDTO.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }

        SysUser user = userMapper.getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!DeleteStatusConstant.NOT_DELETED.equals(user.getIsDeleted())) {
            throw new BusinessException("用户不存在");
        }
        if (!UserStatusConstant.ENABLED.equals(user.getStatus())) {
            throw new BusinessException("当前用户已被禁用");
        }
        if (!user.getPassword().equals(loginDTO.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!expectedRole.equals(user.getRole())) {
            if (UserRoleConstant.ADMIN.equals(expectedRole)) {
                throw new BusinessException("当前账号不是管理员");
            }
            throw new BusinessException("请使用管理员入口登录");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.USER_ROLE, user.getRole());
        String token = jwtTokenUtil.generateToken(claims);

        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setRole(user.getRole());
        loginVO.setToken(token);
        return loginVO;
    }

    /**
     * 统一处理“获取当前登录人信息”。
     * 通过 expectedRole 限制当前 token 必须匹配指定角色，避免普通用户和管理员身份串用。
     */
    private UserInfoVO getCurrentUserByRole(Integer expectedRole) {
        Long currentUserId = UserContext.getCurrentUserId();
        Integer currentUserRole = UserContext.getCurrentUserRole();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        if (!expectedRole.equals(currentUserRole)) {
            if (UserRoleConstant.ADMIN.equals(expectedRole)) {
                throw new BusinessException("当前账号无管理员权限");
            }
            throw new BusinessException("当前账号不是普通用户");
        }

        SysUser user = userMapper.getById(currentUserId);
        if (user == null || !DeleteStatusConstant.NOT_DELETED.equals(user.getIsDeleted())) {
            throw new BusinessException("当前用户不存在");
        }
        if (!expectedRole.equals(user.getRole())) {
            throw new BusinessException("登录用户角色异常");
        }

        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setRealName(user.getRealName());
        userInfoVO.setPhone(user.getPhone());
        userInfoVO.setEmail(user.getEmail());
        userInfoVO.setAvatar(user.getAvatar());
        userInfoVO.setStudentNo(user.getStudentNo());
        userInfoVO.setCreditScore(user.getCreditScore());
        userInfoVO.setRole(user.getRole());
        userInfoVO.setStatus(user.getStatus());
        return userInfoVO;
    }
}
