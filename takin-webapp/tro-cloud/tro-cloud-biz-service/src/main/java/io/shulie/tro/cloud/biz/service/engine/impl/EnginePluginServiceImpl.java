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

package io.shulie.tro.cloud.biz.service.engine.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.shulie.tro.cloud.biz.cloudserver.EnginePluginSimpleResultConvert;
import io.shulie.tro.cloud.biz.input.engine.EnginePluginWrapperInput;
import io.shulie.tro.cloud.biz.output.engine.EnginePluginDetailOutput;
import io.shulie.tro.cloud.biz.output.engine.EnginePluginFileOutput;
import io.shulie.tro.cloud.biz.output.engine.EnginePluginSimpleInfoOutput;
import io.shulie.tro.cloud.biz.service.engine.EnginePluginFilesService;
import io.shulie.tro.cloud.biz.service.engine.EnginePluginService;
import io.shulie.tro.cloud.biz.service.engine.EnginePluginSupportedService;
import io.shulie.tro.cloud.common.constants.Constants;
import io.shulie.tro.cloud.common.constants.EnginePluginConstants;
import io.shulie.tro.cloud.common.constants.ResponseResultConstant;
import io.shulie.tro.cloud.data.mapper.mysql.EnginePluginMapper;
import io.shulie.tro.cloud.data.mapper.mysql.EnginePluginSupportedMapper;
import io.shulie.tro.cloud.data.model.mysql.EnginePluginEntity;
import io.shulie.tro.cloud.data.model.mysql.EnginePluginSupportedVersionEntity;
import io.shulie.tro.cloud.data.result.engine.EnginePluginSimpleInfoResult;
import io.shulie.tro.common.beans.response.ResponseResult;
import io.shulie.tro.utils.json.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ??????????????????
 *
 * @author lipeng
 * @date 2021-01-06 3:09 ??????
 */
@Slf4j
@Service
public class EnginePluginServiceImpl extends ServiceImpl<EnginePluginMapper, EnginePluginEntity> implements EnginePluginService {

    @Resource
    private EnginePluginMapper enginePluginMapper;

    @Resource
    private EnginePluginSupportedService enginePluginSupportedService;

    @Resource
    private EnginePluginFilesService enginePluginFilesService;

    /**
     * ?????????????????????????????????
     *
     * @param pluginTypes ????????????
     *
     * @return
     */
    @Override
    public Map<String, List<EnginePluginSimpleInfoOutput>> findEngineAvailablePluginsByType(List<String> pluginTypes) {
        Map<String, List<EnginePluginSimpleInfoOutput>> result = Maps.newHashMap();
        List<EnginePluginSimpleInfoResult> infos = enginePluginMapper.selectAvailablePluginsByType(pluginTypes);
        List<EnginePluginSimpleInfoOutput> outputInfos = EnginePluginSimpleResultConvert.INSTANCE.ofs(infos);
        if(CollectionUtils.isNotEmpty(outputInfos)) {
            result = outputInfos.stream().filter(r -> r.getPluginType() != null)
                    .collect(Collectors.groupingBy(EnginePluginSimpleInfoOutput::getPluginType));
        }
        return result;
    }

    /**
     * ????????????ID??????????????????
     *
     * @param pluginId
     * @return
     */
    @Override
    public ResponseResult<EnginePluginDetailOutput> findEnginePluginDetailss(Long pluginId) {
        //??????????????????
        EnginePluginEntity enginePluginInfo = enginePluginMapper.selectById(pluginId);
        if(enginePluginInfo == null) {
            return ResponseResult.fail(ResponseResultConstant.RESPONSE_RESULT_CODE_ERROR, "???????????????????????????????????????");
        }
        //???????????????????????????
        List<String> supportedVersions = enginePluginSupportedService.findSupportedVersionsByPluginId(pluginId);
        //??????????????????
        List<EnginePluginFileOutput> uploadFiles = enginePluginFilesService.findPluginFilesInfoByPluginId(pluginId);
        //????????????
        return ResponseResult.success(EnginePluginDetailOutput.create(enginePluginInfo, supportedVersions, uploadFiles));
    }

    /**
     * ??????????????????
     *
     * @param input
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEnginePlugin(EnginePluginWrapperInput input) {
        Long pluginId = input.getPluginId();
        boolean isEdit = (pluginId != null && pluginId != 0);
        //????????????????????????
        EnginePluginEntity pressureEnginePluginEntity = isEdit
                ? enginePluginMapper.selectById(pluginId) : new EnginePluginEntity();

        BeanUtils.copyProperties(input, pressureEnginePluginEntity);
        LocalDateTime now = LocalDateTime.now();
        //????????????????????????
        if(isEdit) {
            pressureEnginePluginEntity.setGmtUpdate(now);
            enginePluginMapper.updateById(pressureEnginePluginEntity);
        } else{
            //??????????????????
            pressureEnginePluginEntity.setGmtCreate(now);
            pressureEnginePluginEntity.setGmtUpdate(now);
            pressureEnginePluginEntity.setStatus(EnginePluginConstants.ENGINE_PLUGIN_STATUS_ENABLED);
            enginePluginMapper.insert(pressureEnginePluginEntity);
        }

        //???????????????id
        pluginId = pressureEnginePluginEntity.getId();

        //?????????????????????????????????
        //????????????
        //???????????????id???????????????????????????
        enginePluginSupportedService.removeSupportedVersionsByPluginId(pluginId);
        //????????????????????????????????????
        enginePluginSupportedService.batchSaveSupportedVersions(input.getSupportedVersions(), pluginId);

        //??????????????????NFS
        enginePluginFilesService.batchSaveEnginePluginFiles(input.getUploadFiles(), pluginId);
    }

    /**
     * @param pluginId
     * @param status
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeEnginePluginStatus(Long pluginId, Integer status) {
        EnginePluginEntity enginePlugin = enginePluginMapper.selectById(pluginId);
        if(Objects.isNull(enginePlugin)) {
            log.warn("???????????????????????????");
            return;
        }
        enginePlugin.setStatus(status);
        this.updateById(enginePlugin);
    }

}
