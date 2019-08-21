package com.chargedot.charge.mapper;

import com.chargedot.charge.model.Fault;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/1/18
 */
@Mapper
public interface FaultMapper {

    void insert(Fault Fault);

    void update(@Param("id") String id, @Param("finishedAt") String finishedAt);

}
