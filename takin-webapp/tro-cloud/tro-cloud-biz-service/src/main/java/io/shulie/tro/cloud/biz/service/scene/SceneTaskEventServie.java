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

package io.shulie.tro.cloud.biz.service.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pamirs.tro.entity.domain.vo.report.SceneTaskNotifyParam;
import com.pamirs.tro.entity.domain.vo.schedule.ScheduleStartRequest;
import com.pamirs.tro.entity.domain.vo.schedule.ScheduleStopRequest;
import io.shulie.tro.cloud.biz.output.scenemanage.SceneManageWrapperOutput;
import io.shulie.tro.cloud.biz.output.scenemanage.SceneManageWrapperOutput.SceneBusinessActivityRefOutput;
import io.shulie.tro.cloud.biz.service.engine.EnginePluginFilesService;
import io.shulie.tro.cloud.common.bean.scenemanage.SceneManageQueryOpitons;
import io.shulie.tro.cloud.common.bean.task.TaskResult;
import io.shulie.tro.cloud.common.constants.ScheduleConstants;
import io.shulie.tro.cloud.common.constants.ScheduleEventConstant;
import io.shulie.tro.cloud.common.enums.engine.EngineStartTypeEnum;
import io.shulie.tro.cloud.common.enums.scenemanage.TaskStatusEnum;
import io.shulie.tro.cloud.data.result.report.ReportResult;
import io.shulie.tro.eventcenter.Event;
import io.shulie.tro.eventcenter.EventCenterTemplate;
import io.shulie.tro.eventcenter.annotation.IntrestFor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author ??????
 * @Date 2020-04-22
 */
@Component
@Slf4j
public class SceneTaskEventServie {

    @Autowired
    private SceneTaskService sceneTaskService;

    @Autowired
    private SceneManageService sceneManageService;

    @Autowired
    private EventCenterTemplate eventCenterTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private EnginePluginFilesService enginePluginFilesService;

    @Value("${script.path}")
    private String scriptPath;

    @Value("${micro.type:localThread}")
    private String microType;

    @IntrestFor(event = "failed")
    public void failed(Event event) {
        log.info("???????????????????????????.....");
        Object object = event.getExt();
        TaskResult taskBean = (TaskResult)object;
        if (taskBean != null) {
            sceneTaskService.handleSceneTaskEvent(taskBean);
        }
    }

    @IntrestFor(event = "started")
    public void started(Event event) {
        log.info("???????????????????????????.....");
        Object object = event.getExt();
        TaskResult taskBean = (TaskResult)object;
        if (taskBean != null) {
            sceneTaskService.handleSceneTaskEvent(taskBean);
        }
    }

    /**
     * ????????????????????????
     *
     * @param scene
     * @param reportId
     */
    public void callStartEvent(SceneManageWrapperOutput scene, Long reportId) {
        ScheduleStartRequest scheduleStartRequest  = new ScheduleStartRequest();
        scheduleStartRequest.setContinuedTime(scene.getTotalTestTime());
        scheduleStartRequest.setSceneId(scene.getId());
        scheduleStartRequest.setTaskId(reportId);
        // ??????id
        scheduleStartRequest.setCustomerId(scene.getCustomId());
        String pressureMode = scene.getPressureMode() == 1 ? "fixed"
            : scene.getPressureMode() == 2 ? "linear" : "stair";
        scheduleStartRequest.setPressureMode(pressureMode);
        scheduleStartRequest.setPressureType(scene.getPressureType());
        scheduleStartRequest.setRampUp(scene.getIncreasingSecond());
        scheduleStartRequest.setSteps(scene.getStep());
        scheduleStartRequest.setTotalIp(scene.getIpNum());
        scheduleStartRequest.setExpectThroughput(scene.getConcurrenceNum());
        Map<String, String> businessData = Maps.newHashMap();
        Map<String, Integer> businessTpsData = Maps.newHashMap();
        Integer tps = 0;
        for (SceneBusinessActivityRefOutput config : scene.getBusinessActivityConfig()){
            businessData.put(config.getBindRef(), config.getTargetRT().toString());
            businessTpsData.put(config.getBindRef(),config.getTargetTPS());
            tps += config.getTargetTPS();
        }
        scheduleStartRequest.setTps(tps);
        scheduleStartRequest.setBusinessData(businessData);
        scheduleStartRequest.setBusinessTpsData(businessTpsData);
        //add by lipeng ??????????????????????????????
        List<Long> enginePluginIds = scene.getEnginePluginIds();
        //????????????????????????
        if(CollectionUtils.isNotEmpty(enginePluginIds)) {
            scheduleStartRequest.setEnginePluginsFilePath(
                    enginePluginFilesService.findPluginFilesPathByPluginIds(enginePluginIds));
        } else { //?????????????????????????????????
            scheduleStartRequest.setEnginePluginsFilePath(
                    Lists.newArrayList());
        }
        //add end

        List dataFileList = new ArrayList();
        String tempScriptPath = EngineStartTypeEnum.LOCAL_THREAD.getType().equals(microType)?(scriptPath + "/") : ScheduleConstants.ENGINE_SCRIPT_FILE_PATH;
        scene.getUploadFile().stream().forEach(file -> {
            if (file.getFileType() == 0) {
                scheduleStartRequest.setScriptPath( tempScriptPath + file.getUploadPath());
            } else {
                ScheduleStartRequest.DataFile dataFile = new ScheduleStartRequest.DataFile();
                dataFile.setName(file.getFileName());
                dataFile.setPath(tempScriptPath+ file.getUploadPath());
                dataFile.setSplit(file.getIsSplit() != null && file.getIsSplit() == 1 ? true : false);
                dataFileList.add(dataFile);
            }
        });
        scheduleStartRequest.setDataFile(dataFileList);

        Event event = new Event();
        event.setEventName(ScheduleEventConstant.START_SCHEDULE_EVENT);
        event.setExt(scheduleStartRequest);
        eventCenterTemplate.doEvents(event);
        log.info("??????[{}]??????????????????.....{}", scene, reportId);
    }

    /**
     * ????????????????????????
     *
     * @param reportResult
     */
    public void callStopEvent(ReportResult reportResult) {
        ScheduleStopRequest  scheduleStopRequest  = new ScheduleStopRequest();
        scheduleStopRequest.setSceneId(reportResult.getSceneId());
        scheduleStopRequest.setTaskId(reportResult.getId());
        scheduleStopRequest.setCustomerId(reportResult.getCustomId());
        Event event = new Event();
        event.setEventName(ScheduleEventConstant.STOP_SCHEDULE_EVENT);
        event.setExt(scheduleStopRequest);
        eventCenterTemplate.doEvents(event);
        log.info("??????????????????[{}]????????????.....{}", reportResult.getSceneId(), reportResult.getId());
    }

    /**
     * ????????????
     *
     * @param param
     */
    public String callStartResultEvent(SceneTaskNotifyParam param) {
        String index = "";
        if (param != null) {
            log.info("??????web????????????:{}", param);
            Event event = new Event();
            TaskResult result = new TaskResult();
            result.setSceneId(param.getSceneId());
            result.setTaskId(param.getTaskId());
            result.setCustomerId(param.getCustomerId());
            result.setMsg(param.getMsg());

            boolean isNotify = true;
            if (param.getStatus().equals("started")) {
                // pod ????????????
                result.setStatus(TaskStatusEnum.STARTED);
                event.setEventName("started");
                /**
                 * ????????????
                 */
                Map<String, Object> extendMap = Maps.newHashMap();
                SceneManageQueryOpitons options = new SceneManageQueryOpitons();
                options.setIncludeBusinessActivity(true);
                SceneManageWrapperOutput dto = sceneManageService.getSceneManage(param.getSceneId(), options);
                if (dto != null && CollectionUtils.isNotEmpty(dto.getBusinessActivityConfig())) {
                    extendMap.put("businessActivityCount", dto.getBusinessActivityConfig().size());
                    extendMap.put("businessActivityBindRef", dto.getBusinessActivityConfig().stream()
                        .filter(data -> StringUtils.isNoneBlank(data.getBindRef()))
                        .map(SceneBusinessActivityRefOutput::getBindRef).distinct().collect(Collectors.toList()));
                }
                result.setExtendMap(extendMap);
                String key = ScheduleConstants.getFileSplitQueue(param.getSceneId(), param.getTaskId(),
                    param.getCustomerId());
                index = stringRedisTemplate.opsForList().leftPop(key);

            } else if (param.getStatus().equals("failed")) {
                result.setStatus(TaskStatusEnum.FAILED);
                event.setEventName("failed");
            } else {
                isNotify = false;
            }

            if (isNotify) {
                event.setExt(result);
                eventCenterTemplate.doEvents(event);
                log.info("????????????web????????????: {}", param);
            }
        }
        return index;
    }

}
