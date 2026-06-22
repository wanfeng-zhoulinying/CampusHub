package com.campushub.mapper;

import com.campushub.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    SysUser getByUsername(@Param("username") String username);

    int saveUser(SysUser user);

    SysUser getById(@Param("id") Long id);
}
