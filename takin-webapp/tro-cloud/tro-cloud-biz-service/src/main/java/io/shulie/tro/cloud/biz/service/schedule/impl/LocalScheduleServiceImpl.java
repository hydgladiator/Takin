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

package io.shulie.tro.cloud.biz.service.schedule.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pamirs.tro.entity.domain.entity.schedule.ScheduleRecord;
import com.pamirs.tro.entity.domain.vo.report.SceneTaskNotifyParam;
import com.pamirs.tro.entity.domain.vo.scenemanage.SceneManageStartRecordVO;
import com.pamirs.tro.entity.domain.vo.schedule.ScheduleInitParam;
import com.pamirs.tro.entity.domain.vo.schedule.ScheduleRunRequest;
import com.pamirs.tro.entity.domain.vo.schedule.ScheduleStartRequest;
import com.pamirs.tro.entity.domain.vo.schedule.ScheduleStopRequest;
import io.shulie.tro.cloud.biz.service.record.ScheduleRecordEnginePluginService;
import io.shulie.tro.cloud.biz.service.scene.SceneManageService;
import io.shulie.tro.cloud.biz.service.scene.SceneTaskService;
import io.shulie.tro.cloud.biz.service.schedule.ScheduleEventService;
import io.shulie.tro.cloud.biz.service.schedule.ScheduleService;
import io.shulie.tro.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.tro.cloud.common.bean.task.TaskResult;
import io.shulie.tro.cloud.common.constants.PressureInstanceRedisKey;
import io.shulie.tro.cloud.common.constants.ScheduleConstants;
import io.shulie.tro.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.tro.cloud.common.redis.RedisClientUtils;
import io.shulie.tro.cloud.common.utils.DesUtil;
import io.shulie.tro.cloud.common.utils.GsonUtil;
import io.shulie.tro.constants.TroRequestConstant;
import io.shulie.tro.eventcenter.Event;
import io.shulie.tro.eventcenter.annotation.IntrestFor;
import io.shulie.tro.k8s.service.MicroService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ??????
 * @Package io.shulie.tro.cloud.biz.service.schedule.impl
 * @date 2021/5/6 4:00 ??????
 */
@Slf4j
@Service
public class LocalScheduleServiceImpl implements ScheduleService {

    @Resource
    private com.pamirs.tro.entity.dao.schedule.TScheduleRecordMapper TScheduleRecordMapper;

    @Autowired
    private ScheduleEventService scheduleEvent;

    @Autowired
    private MicroService microService;

    @Autowired
    private SceneManageService sceneManageService;


    @Resource
    private ScheduleRecordEnginePluginService scheduleRecordEnginePluginService;


    @Value("${console.url}")
    private String console;

    @Value("${spring.redis.host}")
    private String engineRedisAddress;

    @Value("${spring.redis.port}")
    private String engineRedisPort;

    @Value("${spring.redis.password}")
    private String engineRedisPassword;

    @Autowired
    private RedisClientUtils redisClientUtils;


    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private SceneTaskService sceneTaskService;

    /**
     * ??????????????????
     */
    @Value("${pressure.engine.task.dir}")
    private String taskDir;

    /**
     * ??????????????????
     */
    @Value("${pressure.engine.memSetting:-Xmx512m -Xms512m -Xss256K -XX:MaxMetaspaceSize=256m}")
    private String pressureEngineMemSetting;


    @Override
    @Transactional
    public void startSchedule(ScheduleStartRequest request) {
        log.info("????????????, ???????????????{}", request);
        //?????????????????????
        ScheduleRecord schedule = TScheduleRecordMapper.getScheduleByTaskId(request.getTaskId());
        if (schedule != null) {
            log.error("????????????[{}]????????????", request.getTaskId());
            return;
        }
        //??????????????????
        ScheduleRecord scheduleRecord = new ScheduleRecord();
        scheduleRecord.setPodNum(request.getTotalIp());
        scheduleRecord.setSceneId(request.getSceneId());
        scheduleRecord.setTaskId(request.getTaskId());
        scheduleRecord.setStatus(ScheduleConstants.SCHEDULE_STATUS_1);
        // ?????? ??????id
        scheduleRecord.setCustomerId(request.getCustomerId());
        scheduleRecord.setPodClass(
            ScheduleConstants.getScheduleName(request.getSceneId(), request.getTaskId(), request.getCustomerId()));
        TScheduleRecordMapper.insertSelective(scheduleRecord);

        //add by lipeng ????????????????????????????????????????????????
        scheduleRecordEnginePluginService.saveScheduleRecordEnginePlugins(
            scheduleRecord.getId(), request.getEnginePluginsFilePath());
        //add end

        //????????????
        ScheduleRunRequest eventRequest = new ScheduleRunRequest();
        eventRequest.setScheduleId(scheduleRecord.getId());
        eventRequest.setRequest(request);
        //???????????????????????????????????????????????????
        redisClientUtils.setString(ScheduleConstants.getScheduleName(request.getSceneId(), request.getTaskId(), request.getCustomerId()),
            JSON.toJSONString(eventRequest));

        // ?????? ?????????????????? ?????????????????????????????????????????????24??????
        redisClientUtils.set(ScheduleConstants.getPodTotal(request.getSceneId(), request.getTaskId(), request.getCustomerId()),
            request.getTotalIp(), 24 * 60 * 60 * 1000);
        //???????????????
        scheduleEvent.initSchedule(eventRequest);
    }

    @Override
    public void stopSchedule(ScheduleStopRequest request) {
        log.info("????????????, ???????????????{}", request);
        ScheduleRecord scheduleRecord = TScheduleRecordMapper.getScheduleByTaskId(request.getTaskId());
        if (scheduleRecord != null) {
            // ????????????
            String scheduleName = ScheduleConstants.getScheduleName(request.getSceneId(), request.getTaskId(),
                request.getCustomerId());
            redisClientUtils.set(ScheduleConstants.INTERRUPT_POD + scheduleName, true, 24 * 60 * 60 * 1000);
        }
    }

    @Override
    public void runSchedule(ScheduleRunRequest request) {
        // ???????????????????????? ?????????(??????????????????) ---> ??????Job???
        sceneManageService.updateSceneLifeCycle(
            UpdateStatusBean.build(request.getRequest().getSceneId(),
                request.getRequest().getTaskId(),
                request.getRequest().getCustomerId()).checkEnum(
                SceneManageStatusEnum.STARTING, SceneManageStatusEnum.FILESPLIT_END)
                .updateEnum(SceneManageStatusEnum.JOB_CREATEING)
                .build());

        //?????????????????????????????????
        createEngineConfigMap(request);
        //??????????????????????????????
        notifyTaskResult(request);
        // ????????????
        String msg = microService.createJob(ScheduleConstants.getConfigMapName(request.getRequest().getSceneId(), request.getRequest().getTaskId(),
            request.getRequest().getCustomerId()), PressureInstanceRedisKey.getEngineInstanceRedisKey(request.getRequest().getSceneId(),
                request.getRequest().getTaskId(), request.getRequest().getCustomerId()));
        if (StringUtils.isEmpty(msg)) {
            // ?????????
            log.info("??????{},??????{},??????{}????????????????????????Job???????????????job????????????", request.getRequest().getSceneId(),
                request.getRequest().getTaskId(),
                request.getRequest().getCustomerId());
        } else {
            // ????????????
            log.info("??????{},??????{},??????{}????????????????????????Job???????????????job????????????", request.getRequest().getSceneId(),
                request.getRequest().getTaskId(),
                request.getRequest().getCustomerId());
            sceneManageService.reportRecord(SceneManageStartRecordVO.build(request.getRequest().getSceneId(),
                request.getRequest().getTaskId(),
                request.getRequest().getCustomerId()).success(false)
                .errorMsg(String.format("????????????job??????????????????????????????" + msg)).build());
        }
    }

    private void notifyTaskResult(ScheduleRunRequest request) {
        SceneTaskNotifyParam notify = new SceneTaskNotifyParam();
        notify.setSceneId(request.getRequest().getSceneId());
        notify.setTaskId(request.getRequest().getTaskId());
        notify.setCustomerId(request.getRequest().getCustomerId());
        notify.setCustomerId(request.getRequest().getCustomerId());
        notify.setStatus("started");
        sceneTaskService.taskResultNotify(notify);
    }

    @Override
    public void initScheduleCallback(ScheduleInitParam param) {
        // ?????????????????????????????????????????? ---->??????????????????
        sceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(param.getSceneId(), param.getTaskId(),
            param.getCustomerId())
            .checkEnum(SceneManageStatusEnum.FILESPLIT_RUNNING).updateEnum(SceneManageStatusEnum.FILESPLIT_END)
            .build());
        log.info("??????{},??????{},??????????????????, ????????????????????????", param.getSceneId(), param.getTaskId());

        //????????????
        push(param);

        //??????????????????pod
        microService.deleteJob(
            ScheduleConstants.getFileSplitScheduleName(param.getSceneId(), param.getTaskId(), param.getCustomerId()),
            PressureInstanceRedisKey.getEngineInstanceRedisKey(param.getSceneId(), param.getTaskId(), param.getCustomerId()));

        //????????????run
        String dataKey = ScheduleConstants.getScheduleName(param.getSceneId(), param.getTaskId(),
            param.getCustomerId());
        String scheduleRunRequestData = redisClientUtils.getString(dataKey);
        if (StringUtils.isNotEmpty(scheduleRunRequestData)) {
            ScheduleRunRequest scheduleRunRequest = JSON.parseObject(scheduleRunRequestData, ScheduleRunRequest.class);
            //????????????????????????
            runSchedule(scheduleRunRequest);
            //??????????????????
            redisClientUtils.delete(dataKey);
        }
    }

    /**
     * ????????????????????????
     */
    public void createEngineConfigMap(ScheduleRunRequest request) {
        Map<String, Object> configMap = Maps.newHashMap();
        ScheduleStartRequest scheduleStartRequest = request.getRequest();
        configMap.put("name",ScheduleConstants.getConfigMapName(scheduleStartRequest.getSceneId(), scheduleStartRequest.getTaskId(),
            scheduleStartRequest.getCustomerId()));
        JSONObject param = new JSONObject();
        param.put("scriptPath", scheduleStartRequest.getScriptPath());
        param.put("extJarPath", "");
        param.put("isLocal", true);
        param.put("taskDir", taskDir);
        param.put("pressureMode", scheduleStartRequest.getPressureMode());
        param.put("continuedTime", scheduleStartRequest.getContinuedTime());
        if (scheduleStartRequest.getExpectThroughput() != null){
            param.put("expectThroughput", scheduleStartRequest.getExpectThroughput() / scheduleStartRequest.getTotalIp());
        }
        //???jar?????????????????????????????????????????????ext??????
        if (CollectionUtils.isNotEmpty(scheduleStartRequest.getDataFile())){
            List<String> jarFilePaths = scheduleStartRequest.getDataFile().stream().filter(o -> o.getName().endsWith(".jar"))
                .map(ScheduleStartRequest.DataFile::getPath).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(jarFilePaths)){
                jarFilePaths.forEach(scheduleStartRequest::addEnginePluginsFilePath);
            }
        }
        param.put("rampUp", scheduleStartRequest.getRampUp());
        param.put("steps", scheduleStartRequest.getSteps());
        // add start by lipeng ??????????????????????????????????????? enginePluginFolderPath
        param.put("enginePluginsFilePath", scheduleStartRequest.getEnginePluginsFilePath());
        // add end
        JSONObject enginePressureParams = new JSONObject();
        enginePressureParams.put("engineRedisAddress",engineRedisAddress);
        enginePressureParams.put("engineRedisPort",engineRedisPort);
        String engineRedisPasswordDecrypt = null;
        try {
            engineRedisPasswordDecrypt = DesUtil.encrypt(engineRedisPassword, "DBMEETYOURMAKERSMANDKEY");
        } catch (Exception e) {
            log.error("redis??????????????????",e);
        }
        BigDecimal podTpsNum = new BigDecimal(scheduleStartRequest.getTps()).divide(new BigDecimal(scheduleStartRequest.getTotalIp()), 0, BigDecimal.ROUND_UP);
        enginePressureParams.put("tpsTargetLevel",podTpsNum.longValue());
        enginePressureParams.put("engineRedisPassword",engineRedisPasswordDecrypt);
        enginePressureParams.put("enginePressureMode",scheduleStartRequest.getPressureType() == null ? "" : scheduleStartRequest.getPressureType().toString());
        if (scheduleStartRequest.getBusinessTpsData() != null){
            List<Map<String,String>> businessActivities = new ArrayList<>();
            scheduleStartRequest.getBusinessTpsData().forEach((k,v) ->{
                Map<String,String> businessActivity = new HashMap<>();
                businessActivity.put("elementTestName",k);
                businessActivity.put("throughputPercent",new BigDecimal(v).multiply(new BigDecimal(100))
                    .divide(new BigDecimal(scheduleStartRequest.getTps()),0, BigDecimal.ROUND_UP).toString());
                businessActivities.add(businessActivity);
            });
            enginePressureParams.put("businessActivities",businessActivities);
        }
        param.put("enginePressureParams",enginePressureParams);

        String engineInstanceRedisKey = PressureInstanceRedisKey.getEngineInstanceRedisKey(scheduleStartRequest.getSceneId(), scheduleStartRequest.getTaskId(),
            scheduleStartRequest.getCustomerId());
        redisTemplate.opsForHash().put(engineInstanceRedisKey,PressureInstanceRedisKey.SecondRedisKey.REDIS_TPS_ALL_LIMIT,scheduleStartRequest.getTps()+"");
        redisTemplate.opsForHash().put(engineInstanceRedisKey,PressureInstanceRedisKey.SecondRedisKey.REDIS_TPS_LIMIT,podTpsNum+"");
        redisTemplate.opsForHash().put(engineInstanceRedisKey,PressureInstanceRedisKey.SecondRedisKey.REDIS_TPS_POD_NUM,scheduleStartRequest.getTotalIp()+"");
        redisTemplate.expire(engineInstanceRedisKey,10, TimeUnit.DAYS);
        param.put(TroRequestConstant.CLUSTER_TEST_SCENE_HEADER_VALUE, scheduleStartRequest.getSceneId());
        param.put(TroRequestConstant.CLUSTER_TEST_TASK_HEADER_VALUE, scheduleStartRequest.getTaskId());
        //  ??????id
        param.put(TroRequestConstant.CLUSTER_TEST_CUSTOMER_HEADER_VALUE, scheduleStartRequest.getCustomerId());

        param.put("consoleUrl",
            console + ScheduleConstants.getConsoleUrl(request.getRequest().getSceneId(),
                request.getRequest().getTaskId(),
                request.getRequest().getCustomerId()));
        param.put("troCloudCallbackUrl", console + "/api/engine/callback");
        // ?????? ??????pod ,??????????????????????????????????????????bug
        param.put("podCount", scheduleStartRequest.getTotalIp());
        param.put("fileSets", scheduleStartRequest.getDataFile());
        param.put("businessMap", GsonUtil.gsonToString(scheduleStartRequest.getBusinessData()));
        param.put("memSetting", pressureEngineMemSetting);
        configMap.put("engine.conf",param.toJSONString());
        microService.createConfigMap(configMap, PressureInstanceRedisKey.getEngineInstanceRedisKey(request.getRequest().getSceneId(),
            request.getRequest().getTaskId(), request.getRequest().getCustomerId()));
    }


    /**
     * ???????????????
     * ?????????????????????????????????redis??????, ???????????????????????????????????????????????????
     */
    private void push(ScheduleInitParam param) {
        //?????????????????????
        String key = ScheduleConstants.getFileSplitQueue(param.getSceneId(), param.getTaskId(), param.getCustomerId());

        List<String> numList = Lists.newArrayList();
        for (int i = 1; i <= param.getTotal(); i++) {
            numList.add(i + "");
        }

        redisClientUtils.leftPushAll(key, numList);
    }

    /**
     * ????????????????????? pod job configMap
     */
    @IntrestFor(event = "finished")
    public void doDeleteJob(Event event) {
        log.info("??????deleteJob????????? ?????????????????????.....");
        try {
            Object object = event.getExt();
            TaskResult taskResult = (TaskResult)object;
            // ?????? job pod
            String jobName = ScheduleConstants.getScheduleName(taskResult.getSceneId(), taskResult.getTaskId(),
                taskResult.getCustomerId());
            microService.deleteJob(jobName,PressureInstanceRedisKey.getEngineInstanceRedisKey(taskResult.getSceneId(), taskResult.getTaskId(),
                taskResult.getCustomerId()));
            // ?????? configMap
            String engineInstanceRedisKey = PressureInstanceRedisKey.getEngineInstanceRedisKey(taskResult.getSceneId(), taskResult.getTaskId(),
                taskResult.getCustomerId());
            String configMapName = ScheduleConstants.getConfigMapName(taskResult.getSceneId(), taskResult.getTaskId(), taskResult.getCustomerId());
            microService.deleteConfigMap(configMapName);
            redisTemplate.expire(engineInstanceRedisKey,10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("???deleteJob?????????finished????????????={}", e.getMessage(), e);
        }

    }

}
