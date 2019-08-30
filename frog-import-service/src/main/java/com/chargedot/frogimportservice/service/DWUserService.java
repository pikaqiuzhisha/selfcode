package com.chargedot.frogimportservice.service;

import com.chargedot.frogimportservice.model.DWUser;

import java.util.List;

public interface DWUserService {

    /**
     * 批量新增用户信息
     * @param dwUserList 用户集合
     * @return 返回受影响行数
     */
    int importDWUserData(List<DWUser> dwUserList);

    /**
     * 新增用户信息
     * @param dwUser 用户
     * @return 返回受影响行数
     */
    int addDWUserInfo(DWUser dwUser);
}
