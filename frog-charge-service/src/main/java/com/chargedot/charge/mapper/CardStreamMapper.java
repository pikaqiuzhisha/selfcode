package com.chargedot.charge.mapper;

import com.chargedot.charge.model.CardStream;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Eric Gui
 * @date 2019/4/26
 */
@Mapper
public interface CardStreamMapper {

    void insert(CardStream cardStream);
}
