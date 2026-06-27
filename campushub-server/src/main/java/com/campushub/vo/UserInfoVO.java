package com.campushub.vo;

import lombok.Data;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private String studentNo;
    private Integer creditScore;
    private Integer role;
    private Integer status;
}
