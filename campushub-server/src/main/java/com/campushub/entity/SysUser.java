package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {

    /** 主键ID */
    private Long id;
    /** 用户名 */
    private String username;
    /** 登录密码 */
    private String password;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 头像地址 */
    private String avatar;
    /** 学号 */
    private String studentNo;
    /** 信用分 */
    private Integer creditScore;
    /** 用户状态，UserStatusConstant */
    private Integer status;
    /** 逻辑删除标记，DeleteStatusConstant */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
