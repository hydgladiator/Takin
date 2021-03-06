/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.tro.entity.dao.physicalisolation;

import java.util.List;

import com.pamirs.tro.entity.domain.entity.NetworkIsolateConfig;
import org.apache.ibatis.annotations.Param;

public interface TNetworkIsolateConfigMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int deleteByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int insert(NetworkIsolateConfig record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int insertSelective(NetworkIsolateConfig record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    NetworkIsolateConfig selectByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    List<NetworkIsolateConfig> selectByAppName(String applicationName);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    List<NetworkIsolateConfig> selectByAppNameList(@Param("names") List<String> names);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    List<NetworkIsolateConfig> selectIsoldateSuceessByAppNameList(@Param("names") List<String> names);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int updateByPrimaryKeySelective(NetworkIsolateConfig record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int updateByPrimaryKeyWithBLOBs(NetworkIsolateConfig record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_network_isolate_config
     *
     * @mbggenerated
     */
    int updateByPrimaryKey(NetworkIsolateConfig record);

    int queryConfigExsit(NetworkIsolateConfig record);

    List<NetworkIsolateConfig> queryNetworkIsolateConfigs(NetworkIsolateConfig record);

    int deleteNetworkIsolateConfig(@Param("ids") List<Long> ids);

    List<NetworkIsolateConfig> queryNetworkIsolateConfig(@Param("ids") List<Long> ids);
}
