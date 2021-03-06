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

package io.shulie.tro.cloud.biz.service.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pamirs.tro.entity.dao.machine.TMachineSpecMapper;
import com.pamirs.tro.entity.dao.machine.TMachineTaskLogMapper;
import com.pamirs.tro.entity.dao.machine.TMachineTaskMapper;
import com.pamirs.tro.entity.dao.machine.TPressureMachineMapper;
import com.pamirs.tro.entity.domain.entity.machine.MachineSpec;
import com.pamirs.tro.entity.domain.entity.machine.MachineTask;
import com.pamirs.tro.entity.domain.entity.machine.MachineTaskExample;
import com.pamirs.tro.entity.domain.entity.machine.MachineTaskLog;
import com.pamirs.tro.entity.domain.entity.machine.MachineTaskLogExample;
import com.pamirs.tro.entity.domain.entity.machine.PressureMachine;
import com.pamirs.tro.entity.domain.entity.machine.PressureMachineExample;
import com.pamirs.tro.entity.domain.query.MachineTaskLogQueryParm;
import com.pamirs.tro.entity.domain.query.MachineTaskQueryParam;
import com.pamirs.tro.entity.domain.query.PressureMachineQueryParam;
import com.pamirs.tro.entity.domain.vo.cloudserver.CloudAccountVO;
import com.pamirs.tro.entity.domain.vo.cloudserver.CloudPlatformVO;
import com.pamirs.tro.entity.domain.vo.machine.MachineDeleteTaskVO;
import com.pamirs.tro.entity.domain.vo.machine.MachineTaskLogVO;
import com.pamirs.tro.entity.domain.vo.machine.MachineTaskVO;
import com.pamirs.tro.entity.domain.vo.machine.PressureMachineVO;
import io.shulie.tro.cloud.biz.cache.DictionaryCache;
import io.shulie.tro.cloud.biz.cloudserver.MachineTaskConvert;
import io.shulie.tro.cloud.biz.cloudserver.MachineTaskLogConvert;
import io.shulie.tro.cloud.biz.cloudserver.PressureMachineConvert;
import io.shulie.tro.cloud.biz.service.cloudServer.CloudAccountService;
import io.shulie.tro.cloud.biz.service.cloudServer.CloudPlatformService;
//import io.shulie.tro.cloud.biz.service.cloudServer.CloudPluginServerService;
import io.shulie.tro.cloud.common.constants.DicKeyConstant;
import io.shulie.tro.cloud.common.enums.machine.MachineStatusEnum;
import io.shulie.tro.cloud.common.enums.machine.MachineTaskStatusEnum;
import io.shulie.tro.cloud.common.utils.DateUtil;
import io.shulie.tro.common.beans.response.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @Author: fanxx
 * @Date: 2020/5/13 ??????11:33
 * @Description:
 */
@Service
@Slf4j
public class MachineManageServiceImpl implements MachineManageService {

    public static String FALSE_CORE = "0";
    @Autowired
    TMachineTaskMapper TMachineTaskMapper;
    @Autowired
    TMachineTaskLogMapper TMachineTaskLogMapper;
    @Autowired
    TMachineSpecMapper TMachineSpecMapper;
    @Autowired
    TPressureMachineMapper TPressureMachineMapper;
    @Autowired
    CloudAccountService cloudAccountService;
    //@Autowired
    //CloudPluginServerService cloudPluginServerService;
    @Autowired
    CloudPlatformService cloudPlatformService;
    @Autowired
    MachineEventPublishService machineEventPublishService;
    @Autowired
    private DictionaryCache dictionaryCache;

    @Scheduled(cron = "0 */5 * * * ?")
    public void processExpireMachine() {
        PressureMachineExample pressureMachineExample = new PressureMachineExample();
        PressureMachineExample.Criteria criteria = pressureMachineExample.createCriteria();
        criteria.andIsDeleteEqualTo(false);
        criteria.andStatusEqualTo(MachineStatusEnum.do_run.getCode());
        List<PressureMachine> pressureMachines = TPressureMachineMapper.selectByExample(pressureMachineExample);
        if (CollectionUtils.isNotEmpty(pressureMachines)) {
            List<Long> expireMachineIds = pressureMachines.stream()
                .filter(pressureMachine -> pressureMachine.getExpireDate().getTime() < System.currentTimeMillis())
                .map(PressureMachine::getId).collect(Collectors.toList());
            MachineDeleteTaskVO machineDeleteTaskVO = new MachineDeleteTaskVO();
            machineDeleteTaskVO.setMachineIdList(expireMachineIds);
            addMachineDestoryTask(machineDeleteTaskVO);
            log.info("??????????????????:{}", JSON.toJSON(expireMachineIds));
        } else {
            log.info("?????????????????????");
        }
    }

    @Override
    public ResponseResult addMachineOpenTask(MachineTaskVO machineTaskVO) {
        //????????????
        ResponseResult response = validate(machineTaskVO);
        if (!response.getSuccess()) {
            return response;
        }
        CloudAccountVO cloudAccountVO = (CloudAccountVO)cloudAccountService.queryById(machineTaskVO.getAccountId())
            .getData();
        CloudPlatformVO cloudPlatformVO = (CloudPlatformVO)cloudPlatformService.queryById(machineTaskVO.getPlatformId())
            .getData();
        String authorizeParam = cloudAccountVO.getAuthorizeParam();
        HashMap<String, Object> extendsMap = (HashMap<String, Object>)JSON.parseObject(authorizeParam, HashMap.class);
        if (!StringUtils.isBlank(machineTaskVO.getExtendConfig())) {
            ResponseResult parseResponse = MachineUtil.parseXMLToMap(machineTaskVO.getExtendConfig(), extendsMap);
            if (!parseResponse.getSuccess()) {
                return parseResponse;
            }
        }
        MachineSpec machineSpec = TMachineSpecMapper.selectByPrimaryKey(machineTaskVO.getSpecId());
        machineTaskVO.setPlatformName(cloudPlatformVO.getName());
        machineTaskVO.setAccountName(cloudAccountVO.getAccount());
        machineTaskVO.setSpec(machineSpec.getSpec());
        machineTaskVO.setStatus(MachineTaskStatusEnum.do_open.getCode());
        machineTaskVO.setTaskType(1);
        MachineTask machineTask = new MachineTask();
        String serialNo = UUID.randomUUID().toString();
        BeanUtils.copyProperties(machineTaskVO, machineTask);
        //????????????????????????
        machineTask.setSerialNo(serialNo);
        TMachineTaskMapper.insertSelective(machineTask);
        //????????????
        machineEventPublishService.open(machineTask);
        return ResponseResult.success("??????????????????");
    }

    @Override
    public ResponseResult addMachineDestoryTask(MachineDeleteTaskVO machineDeleteTaskVO) {
        PressureMachineExample machineExample = new PressureMachineExample();
        PressureMachineExample.Criteria machineCriteria = machineExample.createCriteria();
        machineCriteria.andIdIn(machineDeleteTaskVO.getMachineIdList());
        List<PressureMachine> pressureMachines = TPressureMachineMapper.selectByExample(machineExample);
        List<PressureMachine> toDestoryPressureMachines = pressureMachines.stream()
            .filter(pressureMachine -> !MachineStatusEnum.destoryed.getCode().equals(pressureMachine.getStatus()))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(toDestoryPressureMachines)) {
            return ResponseResult.success("????????????");
        }

        List<Long> toDestoryIds = toDestoryPressureMachines.stream().map(PressureMachine::getId).collect(
            Collectors.toList());
        MachineTaskVO machineTaskVO = new MachineTaskVO();
        machineTaskVO.setAccountId(pressureMachines.get(0).getAccountId());
        machineTaskVO.setPlatformId(pressureMachines.get(0).getPlatformId());
        machineTaskVO.setMachineIdList(toDestoryIds);
        //????????????
        ResponseResult response = validate(machineTaskVO);
        if (!response.getSuccess()) {
            return response;
        }
        CloudAccountVO cloudAccountVO = (CloudAccountVO)cloudAccountService.queryById(machineTaskVO.getAccountId())
            .getData();
        CloudPlatformVO cloudPlatformVO = (CloudPlatformVO)cloudPlatformService.queryById(machineTaskVO.getPlatformId())
            .getData();
        MachineTask machineTask = new MachineTask();
        String serialNo = UUID.randomUUID().toString();
        machineTask.setSerialNo(serialNo);
        machineTask.setPlatformId(machineTaskVO.getPlatformId());
        machineTask.setPlatformName(cloudPlatformVO.getName());
        machineTask.setAccountId(machineTaskVO.getAccountId());
        machineTask.setAccountName(cloudAccountVO.getAccount());
        machineTask.setStatus(MachineTaskStatusEnum.do_destory.getCode());
        machineTask.setMachineNum(toDestoryIds.size());
        machineTask.setTaskType(2);
        //????????????????????????
        TMachineTaskMapper.insertSelective(machineTask);
        //????????????
        machineTaskVO.setSerialNo(serialNo);
        machineEventPublishService.destroy(machineTaskVO);
        return ResponseResult.success("??????????????????");
    }

    private ResponseResult validate(MachineTaskVO machineTaskVO) {
        //????????????
        ResponseResult accountResponse = cloudAccountService.queryById(machineTaskVO.getAccountId());
        if (!accountResponse.getSuccess()) {
            //????????????
            return accountResponse;
        } else {
            ResponseResult platformResponse = cloudPlatformService.queryById(machineTaskVO.getPlatformId());
            if (!platformResponse.getSuccess()) {
                //????????????
                return platformResponse;
            } else {
                CloudPlatformVO cloudPlatformVO = (CloudPlatformVO)platformResponse.getData();
                if (!cloudPlatformVO.getStatus()) {
                    return ResponseResult.fail(FALSE_CORE, "??????????????????", "");
                }
                //????????????
                CloudAccountVO cloudAccountVO = (CloudAccountVO)accountResponse.getData();
                String authorizeParam = cloudAccountVO.getAuthorizeParam();
                if (StringUtils.isEmpty(authorizeParam)) {
                    return ResponseResult.fail(FALSE_CORE, "????????????????????????", "");
                }
                if (!cloudAccountVO.getStatus()) {
                    return ResponseResult.fail(FALSE_CORE, "??????????????????", "");
                }
            }
        }
        return ResponseResult.success();
    }

    @Override
    public PageInfo<MachineTaskVO> queryMachineTaskPageInfo(MachineTaskQueryParam param) {
        MachineTaskExample machineTaskExample = new MachineTaskExample();
        MachineTaskExample.Criteria criteria = machineTaskExample.createCriteria();
        criteria.andIsDeleteEqualTo(false);
        machineTaskExample.setOrderByClause("gmt_create desc");
        if (null != param.getTaskType()) {
            criteria.andTaskTypeEqualTo(param.getTaskType());
        }
        if (null != param.getTaskStatus()) {
            criteria.andStatusEqualTo(param.getTaskStatus());
        }
        if (null != param.getStartTime() && null != param.getEndTime()) {
            criteria.andGmtCreateBetween(DateUtil.getDate(param.getStartTime()), DateUtil.getDate(param.getEndTime()));
        }
        Page<Object> page = PageHelper.startPage(param.getCurrentPage() + 1, param.getPageSize());
        List<MachineTask> tmpMachineTasks = TMachineTaskMapper.selectByExample(machineTaskExample);
        if (tmpMachineTasks.isEmpty()) {
            return new PageInfo<>(new ArrayList<>());
        }
        List<MachineTask> machineTasks = new ArrayList<>();
        for (MachineTask machineTask : tmpMachineTasks) {
            machineTask.setStatusObj(
                dictionaryCache.getObjectByParam(DicKeyConstant.MACHINE_TASK_STATUS, machineTask.getStatus()));
            machineTasks.add(machineTask);
        }
        PageInfo<MachineTaskVO> pageInfo = new PageInfo(MachineTaskConvert.INSTAMCE.ofs(machineTasks));
        pageInfo.setTotal(page.getTotal());
        return pageInfo;
    }

    @Override
    public PageInfo<PressureMachineVO> queryMachinePageInfo(PressureMachineQueryParam param) {
        PressureMachineExample pressureMachineExample = new PressureMachineExample();
        PressureMachineExample.Criteria criteria = pressureMachineExample.createCriteria();
        criteria.andIsDeleteEqualTo(false);
        pressureMachineExample.setOrderByClause("gmt_create desc");
        if (null != param.getPlatformId()) {
            criteria.andPlatformIdEqualTo(param.getPlatformId());
        }
        if (null != param.getStatus()) {
            criteria.andStatusEqualTo(param.getStatus());
        }
        if (null != param.getOpenType()) {
            criteria.andOpenTypeEqualTo(param.getOpenType());
        }
        if (null != param.getStartTime() && null != param.getEndTime()) {
            criteria.andExpireDateBetween(DateUtil.getDate(param.getStartTime()), DateUtil.getDate(param.getEndTime()));
        }
        Page<Object> page = PageHelper.startPage(param.getCurrentPage() + 1, param.getPageSize());
        List<PressureMachine> tmpPressureMachines = TPressureMachineMapper.selectByExample(pressureMachineExample);
        if (tmpPressureMachines.isEmpty()) {
            return new PageInfo<>(new ArrayList<>());
        }
        List<PressureMachine> pressureMachines = new ArrayList<>();
        for (PressureMachine pressureMachine : tmpPressureMachines) {
            pressureMachine.setStatusObj(
                dictionaryCache.getObjectByParam(DicKeyConstant.MACHINE_STATUS, pressureMachine.getStatus()));
            pressureMachines.add(pressureMachine);
        }
        PageInfo<PressureMachineVO> pageInfo = new PageInfo(PressureMachineConvert.INSTAMCE.ofs(pressureMachines));
        pageInfo.setTotal(page.getTotal());
        return pageInfo;
    }

    @Override
    public PageInfo<MachineTaskLogVO> queryLogByTaskId(MachineTaskLogQueryParm param) {
        MachineTaskLogExample machineTaskLogExample = new MachineTaskLogExample();
        MachineTaskLogExample.Criteria criteria = machineTaskLogExample.createCriteria();
        criteria.andTaskIdEqualTo(param.getTaskId());
        Page<Object> page = PageHelper.startPage(param.getCurrentPage() + 1, param.getPageSize());
        List<MachineTaskLog> tmpMachineTaskLogs = TMachineTaskLogMapper.selectByExampleWithBLOBs(machineTaskLogExample);
        List<MachineTaskLog> machineTaskLogs = new ArrayList<>();
        if (tmpMachineTaskLogs.isEmpty()) {
            return new PageInfo<>(new ArrayList<>());
        }
        for (MachineTaskLog machineTaskLog : tmpMachineTaskLogs) {
            machineTaskLog.setStatusObj(dictionaryCache.getObjectByParam(DicKeyConstant.MACHINE_STATUS,
                Integer.parseInt(String.valueOf(machineTaskLog.getStatus()))));
            machineTaskLogs.add(machineTaskLog);
        }
        PageInfo<MachineTaskLogVO> pageInfo = new PageInfo(MachineTaskLogConvert.INSTAMCE.ofs(machineTaskLogs));
        pageInfo.setTotal(page.getTotal());
        return pageInfo;
    }
}
