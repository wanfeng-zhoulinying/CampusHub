package com.campushub.service.user;

import com.campushub.dto.UserLoginDTO;
import com.campushub.dto.UserRegisterDTO;
import com.campushub.vo.UserInfoVO;
import com.campushub.vo.UserLoginVO;

public interface UserService {

    Long register(UserRegisterDTO registerDTO);

    UserLoginVO login(UserLoginDTO loginDTO);

    UserInfoVO getCurrentUser();
}
