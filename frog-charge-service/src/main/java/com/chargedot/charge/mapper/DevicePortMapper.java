package com.chargedot.charge.mapper;

import com.chargedot.charge.model.DevicePort;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
@Mapper
public interface DevicePortMapper {

    DevicePort findLikeDeviceNumberAvailable(@Param("deviceNumber") String deviceNumber);

    List<DevicePort> findLikeDeviceNumber(@Param("deviceNumber") String deviceNumber);

    DevicePort findByPortNumber(@Param("portNumber") String portNumber);

    DevicePort findByPortNumberAvailable(@Param("portNumber") String portNumber);

    DevicePort findByOccupyUserId(@Param("certId") int certId);

    void update(DevicePort devicePort);
}
