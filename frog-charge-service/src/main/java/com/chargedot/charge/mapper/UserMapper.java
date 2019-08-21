package com.chargedot.charge.mapper;

import com.chargedot.charge.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户ID查找用户
     * @param userId
     * @return
     */
    User findById(@Param("userId") Integer userId);
}
