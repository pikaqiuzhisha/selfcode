package com.chargedot.frogimportservice.mapper;

import com.chargedot.frogimportservice.model.DWUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DWUserMapper {

    /**
     * 批量新增用户信息
     * @param dwUserList 用户集合
     * @return 返回受影响行数
     */
    int importDWUserData(@Param("dwUserList") List<DWUser> dwUserList);

    /**
     * 新增用户信息
     * @param dwUser 用户
     * @return 返回受影响行数
     */
    int addDWUserInfo(DWUser dwUser);
}
