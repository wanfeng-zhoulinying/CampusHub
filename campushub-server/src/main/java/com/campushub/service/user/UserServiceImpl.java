package com.campushub.service.user;

import com.campushub.constant.DeleteStatusConstant;
import com.campushub.constant.JwtClaimsConstant;
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
     * 当前阶段使用用户名密码注册，后续再补短信验证码、邮箱验证等增强能力。
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
        user.setStatus(UserStatusConstant.ENABLED);
        user.setIsDeleted(DeleteStatusConstant.NOT_DELETED);

        userMapper.saveUser(user);
        return user.getId();
    }

    /**
     * 用户登录。
     * 登录成功后生成 JWT，供后续接口通过拦截器识别当前登录用户。
     */
    @Override
    public UserLoginVO login(UserLoginDTO loginDTO) {
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

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        String token = jwtTokenUtil.generateToken(claims);

        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setToken(token);
        return loginVO;
    }

    /**
     * 获取当前登录用户信息。
     * 当前用户身份由 JWT 拦截器解析后写入 ThreadLocal。
     */
    @Override
    public UserInfoVO getCurrentUser() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }

        SysUser user = userMapper.getById(currentUserId);
        if (user == null || !DeleteStatusConstant.NOT_DELETED.equals(user.getIsDeleted())) {
            throw new BusinessException("当前用户不存在");
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
        userInfoVO.setStatus(user.getStatus());
        return userInfoVO;
    }
}
