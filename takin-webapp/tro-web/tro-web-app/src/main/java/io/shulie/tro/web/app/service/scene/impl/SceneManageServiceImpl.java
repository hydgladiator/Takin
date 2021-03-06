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

package io.shulie.tro.web.app.service.scene.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.shulie.tro.web.common.enums.script.FileTypeEnum;
import com.pamirs.tro.common.constant.SceneManageConstant;
import com.pamirs.tro.common.constant.TimeUnitEnum;
import com.pamirs.tro.common.exception.ApiException;
import com.pamirs.tro.common.util.DateUtils;
import com.pamirs.tro.common.util.ListHelper;
import com.pamirs.tro.common.util.parse.UrlUtil;
import com.pamirs.tro.entity.dao.confcenter.TApplicationMntDao;
import com.pamirs.tro.entity.dao.linkmanage.TBusinessLinkManageTableMapper;
import com.pamirs.tro.entity.dao.linkmanage.TLinkManageTableMapper;
import com.pamirs.tro.entity.domain.dto.scenemanage.ScriptCheckDTO;
import com.pamirs.tro.entity.domain.entity.linkmanage.BusinessLinkManageTable;
import com.pamirs.tro.entity.domain.entity.scenemanage.SceneBusinessActivityRef;
import com.pamirs.tro.entity.domain.entity.user.User;
import com.pamirs.tro.entity.domain.vo.scenemanage.FlowVO;
import com.pamirs.tro.entity.domain.vo.scenemanage.SceneBusinessActivityRefVO;
import com.pamirs.tro.entity.domain.vo.scenemanage.SceneManageIdVO;
import com.pamirs.tro.entity.domain.vo.scenemanage.SceneManageQueryVO;
import com.pamirs.tro.entity.domain.vo.scenemanage.SceneManageWrapperVO;
import com.pamirs.tro.entity.domain.vo.scenemanage.SceneParseVO;
import com.pamirs.tro.entity.domain.vo.scenemanage.SceneScriptRefVO;
import com.pamirs.tro.entity.domain.vo.scenemanage.ScriptUrlVO;
import com.pamirs.tro.entity.domain.vo.scenemanage.TimeVO;
import io.shulie.tro.cloud.common.bean.TimeBean;
import io.shulie.tro.cloud.open.req.scenemanage.SceneIpNumReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageIdReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageQueryByIdsReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageQueryReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageWrapperReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneScriptRefOpen;
import io.shulie.tro.cloud.open.resp.scenemanage.SceneManageListResp;
import io.shulie.tro.cloud.open.resp.scenemanage.SceneManageWrapperResp;
import io.shulie.tro.cloud.open.resp.scenemanage.SceneManageWrapperResp.SceneBusinessActivityRefResp;
import io.shulie.tro.cloud.open.resp.scenemanage.SceneManageWrapperResp.SceneScriptRefResp;
import io.shulie.tro.cloud.open.resp.strategy.StrategyResp;
import io.shulie.tro.common.beans.response.ResponseResult;
import io.shulie.tro.web.app.common.RestContext;
import io.shulie.tro.web.app.request.scenemanage.SceneSchedulerTaskCreateRequest;
import io.shulie.tro.web.app.request.scenemanage.SceneSchedulerTaskUpdateRequest;
import io.shulie.tro.web.app.response.filemanage.FileManageResponse;
import io.shulie.tro.web.app.response.scenemanage.SceneListResponse;
import io.shulie.tro.web.app.response.scenemanage.SceneSchedulerTaskResponse;
import io.shulie.tro.web.app.response.scenemanage.SceneTagRefResponse;
import io.shulie.tro.web.app.response.scriptmanage.ScriptManageDeployDetailResponse;
import io.shulie.tro.web.app.response.tagmanage.TagManageResponse;
import io.shulie.tro.web.app.service.scene.ApplicationBusinessActivityService;
import io.shulie.tro.web.app.service.scenemanage.SceneManageService;
import io.shulie.tro.web.app.service.scenemanage.SceneSchedulerTaskService;
import io.shulie.tro.web.app.service.scenemanage.SceneTagService;
import io.shulie.tro.web.app.service.scriptmanage.ScriptManageService;
import io.shulie.tro.web.auth.api.UserService;
import io.shulie.tro.web.common.constant.RemoteConstant;
import io.shulie.tro.web.common.domain.ErrorInfo;
import io.shulie.tro.web.common.domain.WebResponse;
import io.shulie.tro.web.common.enums.script.ScriptTypeEnum;
import io.shulie.tro.web.common.http.HttpWebClient;
import io.shulie.tro.web.common.util.ActivityUtil;
import io.shulie.tro.web.common.util.ActivityUtil.EntranceJoinEntity;
import io.shulie.tro.web.common.util.JsonUtil;
import io.shulie.tro.web.diff.api.scenemanage.SceneManageApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

/**
 * @ClassName SceneManageImpl
 * @Description
 * @Author qianshui
 * @Date 2020/4/17 ??????3:32
 */
@Service
@Slf4j
public class SceneManageServiceImpl implements SceneManageService {

    @Autowired
    private HttpWebClient httpWebClient;

    @Resource
    private TBusinessLinkManageTableMapper TBusinessLinkManageTableMapper;

    @Value("${script.check:true}")
    private Boolean scriptCheck;

    @Value("${script.check.perfomancetype:false}")
    private Boolean scriptPTCheck;

    @Resource
    private TLinkManageTableMapper TLinkManageTableMapper;

    @Resource
    private TApplicationMntDao tApplicationMntDao;

    @Autowired
    private ApplicationBusinessActivityService applicationBusinessActivityService;

    @Autowired
    private ScriptManageService scriptManageService;

    @Autowired
    private SceneManageApi sceneManageApi;

    @Autowired
    private UserService userService;

    @Autowired
    private SceneSchedulerTaskService sceneSchedulerTaskService;

    @Autowired
    private SceneTagService sceneTagService;

    @Override
    public ResponseResult<List<SceneManageWrapperResp>> getByIds(SceneManageQueryByIdsReq req) {
        return sceneManageApi.getByIds(req);
    }

    @Override
    public WebResponse addScene(SceneManageWrapperVO vo, Long userId) {
        /**
         * 0????????????????????????????????????????????????
         *    1??????????????????????????????
         *    2???Jmeter????????????????????????
         *    3??????????????????????????????????????????
         * 1?????????????????????+????????????
         * 2?????????????????????
         * 3???????????????
         * 4?????????SLA
         */
        if (!Objects.isNull(vo.getScheduleInterval())) {
            Long scheduleInterval = convertTime(Long.parseLong(String.valueOf(vo.getScheduleInterval())),
                TimeUnitEnum.MINUTE.getValue());
            TimeVO timeVo = vo.getPressureTestTime();
            Long pressureTestSecond = convertTime(timeVo.getTime(), timeVo.getUnit());
            if (scheduleInterval > pressureTestSecond) {
                return WebResponse.fail("500", "????????????????????????????????????????????????");
            }
        }


        SceneManageWrapperReq req = new SceneManageWrapperReq();
        WebResponse webResponse = null;
        webResponse = sceneManageVo2Req(vo, req, userId);
        if (webResponse == null) {
            webResponse = new WebResponse();
        }
        try {
            ResponseResult<Long> result = sceneManageApi.saveScene(req);
            if (vo.getIsScheduler() != null && vo.getIsScheduler() == true && result != null
                && result.getSuccess() == true && vo.getExecuteTime()!=null) {
                //????????????????????????
                Long sceneId = JSON.parseObject(JSON.toJSONString(result.getData()), Long.class);
                SceneSchedulerTaskCreateRequest createRequest = new SceneSchedulerTaskCreateRequest();
                createRequest.setSceneId(sceneId);
                createRequest.setExecuteTime(vo.getExecuteTime());
                createRequest.setUserId(RestContext.getUser().getId());
                sceneSchedulerTaskService.insert(createRequest);
            }

            webResponse.setSuccess(true);
        } catch (Exception e) {
            webResponse.setSuccess(false);
            log.error(e.getMessage(), e);
        }
        return webResponse;
    }

    public WebResponse sceneManageVo2Req(SceneManageWrapperVO vo, SceneManageWrapperReq req, Long userId) {
        WebResponse webResponse = this.buildSceneManageRef(vo, userId);
        Object data = webResponse.getData();
        if (!webResponse.getSuccess()) {
            return webResponse;
        }
        BeanUtils.copyProperties(vo, req);
        if (data != null) {
            req.setUploadFile((List<SceneScriptRefOpen>) data);
        }
        //??????????????????
        if (vo.getPressureTestTime() != null) {
            TimeBean presTime = new TimeBean();
            presTime.setUnit(vo.getPressureTestTime().getUnit());
            presTime.setTime(vo.getPressureTestTime().getTime());
            req.setPressureTestTime(presTime);
        }
        //??????????????????
        if (vo.getIncreasingTime() != null) {
            TimeBean incresTime = new TimeBean();
            incresTime.setTime(vo.getIncreasingTime().getTime());
            incresTime.setUnit(vo.getIncreasingTime().getUnit());
            req.setIncreasingTime(incresTime);
            req.setIncreasingTime(incresTime);
        }
        assembleFeatures(vo, req);
        return webResponse;
    }

    //?????????????????????features
    public void assembleFeatures(SceneManageWrapperVO vo, SceneManageWrapperReq req) {
        Map<String, Object> map = new HashMap<>();
        map.put(SceneManageConstant.FEATURES_BUSINESS_FLOW_ID, vo.getBusinessFlowId());
        Integer configType = vo.getConfigType();
        map.put(SceneManageConstant.FEATURES_CONFIG_TYPE, configType);
        if (configType != null && configType == 1) {
            if (CollectionUtils.isNotEmpty(vo.getBusinessActivityConfig())) {
                map.put(SceneManageConstant.FEATURES_SCRIPT_ID, vo.getBusinessActivityConfig().get(0).getScriptId());
            }
        } else {
            map.put(SceneManageConstant.FEATURES_SCRIPT_ID, vo.getScriptId());
        }
        map.put(SceneManageConstant.FEATURES_SCHEDULE_INTERVAL, vo.getScheduleInterval());
        req.setFeatures(JSON.toJSONString(map));
    }

    @Override
    public WebResponse getPageList(SceneManageQueryVO vo) {

        WebResponse webResponse = new WebResponse();
        webResponse.setData(Lists.newArrayList());
        webResponse.setSuccess(true);
        webResponse.setTotal(0L);
        SceneManageQueryReq req = new SceneManageQueryReq();
        BeanUtils.copyProperties(vo, req);
        if (vo.getTagId() != null) {
            List<Long> tagIds = Arrays.asList(vo.getTagId());
            List<SceneTagRefResponse> sceneTagRefBySceneIds = sceneTagService.getTagRefByTagIds(tagIds);
            if (CollectionUtils.isNotEmpty(sceneTagRefBySceneIds)){
                List<Long> sceneIds = sceneTagRefBySceneIds.stream().map(SceneTagRefResponse::getSceneId).distinct().collect(Collectors.toList());
                String sceneIdStr = StringUtils.join(sceneIds, ",");
                req.setSceneIds(sceneIdStr);
            }else {
                return webResponse;
            }
        }
        req.setLastPtStartTime(vo.getLastPtStartTime());
        req.setLastPtEndTime(vo.getLastPtEndTime());
        ResponseResult<List<SceneManageListResp>> sceneList = sceneManageApi.getSceneList(req);
        List<SceneManageListResp> listData = sceneList.getData();
       // List<Long> sceneIds = listData.stream().map(tmp -> tmp.getId()).collect(Collectors.toList());
        //??????ids
        List<Long> userIds = listData.stream().map(SceneManageListResp::getUserId).filter(Objects::nonNull)
            .collect(Collectors.toList());
        //????????????Map key:userId  value:user??????
        Map<Long, User> userMap = userService.getUserMapByIds(userIds);
        listData.stream().filter(data -> null != data.getUserId()).forEach(data -> {
            //?????????id
            data.setManagerId(data.getUserId());
            //???????????????
            String userName = Optional.ofNullable(userMap.get(data.getUserId()))
                .map(User::getName)
                .orElse("");
            data.setManagerName(userName);
            List<Long> allowUpdateUserIdList = RestContext.getUpdateAllowUserIdList();
            if (CollectionUtils.isNotEmpty(allowUpdateUserIdList)) {
                data.setCanEdit(allowUpdateUserIdList.contains(data.getUserId()));
            }
            List<Long> allowDeleteUserIdList = RestContext.getDeleteAllowUserIdList();
            if (CollectionUtils.isNotEmpty(allowDeleteUserIdList)) {
                data.setCanRemove(allowDeleteUserIdList.contains(data.getUserId()));
            }
            List<Long> allowStartStopUserIdList = RestContext.getStartStopAllowUserIdList();
            if (CollectionUtils.isNotEmpty(allowStartStopUserIdList)) {
                data.setCanStartStop(allowStartStopUserIdList.contains(data.getUserId()));
            }
        });

        //?????????????????????????????????
        webResponse.setTotal(sceneList.getTotalNum());
        if (null != sceneList) {
            webResponse.setTotal(sceneList.getTotalNum());
        } else {
            webResponse.setTotal(0L);
        }
        webResponse.setSuccess(true);
        List<SceneListResponse> sceneListResponses = adjustSchedulerScene(listData);
        webResponse.setData(sceneListResponses);
        return webResponse;
    }

    List<SceneListResponse> adjustSchedulerScene(List<SceneManageListResp> sceneRespList) {

        if (CollectionUtils.isEmpty(sceneRespList)) {
            return Lists.newArrayList();
        }
        List<Long> sceneIds = sceneRespList.stream().map(SceneManageListResp::getId).collect(
            Collectors.toList());
        List<SceneSchedulerTaskResponse> responseList = sceneSchedulerTaskService.selectBySceneIds(sceneIds);
        Map<Long, Date> schedulerMap = new HashMap<>();
        responseList.stream().forEach(response -> {
            schedulerMap.put(response.getSceneId(), response.getExecuteTime());
        });

        //????????????????????????
        List<TagManageResponse> allSceneTags = sceneTagService.getAllSceneTags();
        Map<Long, TagManageResponse> tagMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(allSceneTags)) {
            allSceneTags.stream().forEach(tagManageResponse -> {
                tagMap.put(tagManageResponse.getId(), tagManageResponse);
            });
        }

        List<SceneTagRefResponse> sceneTagRefList = sceneTagService.getSceneTagRefBySceneIds(sceneIds);
        Map<Long, List<SceneTagRefResponse>> sceneMap = sceneTagRefList.stream().collect(
            Collectors.groupingBy(SceneTagRefResponse::getSceneId));

        List<SceneListResponse> voList = new ArrayList<>();
        for (SceneManageListResp resp : sceneRespList) {
            SceneListResponse sceneVo = new SceneListResponse();
            BeanUtils.copyProperties(resp, sceneVo);
            Date executeTime = schedulerMap.get(sceneVo.getId());
            if (executeTime!=null) {
                sceneVo.setIsScheduler(true);
                sceneVo.setScheduleExecuteTime(DateUtils.dateToString(executeTime,DateUtils.FORMATE_YMDHMS));
            } else {
                sceneVo.setIsScheduler(false);
            }
            //????????????????????????
            List<TagManageResponse> tags = new ArrayList<>();
            if (sceneMap.containsKey(sceneVo.getId())) {
                List<SceneTagRefResponse> refTagList = sceneMap.get(sceneVo.getId());
                for (SceneTagRefResponse response : refTagList) {
                    Long tagId = response.getTagId();
                    if (tagMap.containsKey(tagId)) {
                        tags.add(tagMap.get(tagId));
                    }
                }
            }
            sceneVo.setTag(tags);
            voList.add(sceneVo);
        }

        return voList;
    }

    @Override
    public WebResponse calcFlow(FlowVO vo) {
        vo.setRequestUrl(RemoteConstant.SCENE_MANAGE_FLOWCALC_URL);
        vo.setHttpMethod(HttpMethod.POST);
        return httpWebClient.request(vo);
    }

    @Override
    public ResponseResult<StrategyResp> getIpNum(Integer concurrenceNum, Integer tpsNum) {
        SceneIpNumReq req = new SceneIpNumReq();
        req.setConcurrenceNum(concurrenceNum);
        req.setTpsNum(tpsNum);
        return sceneManageApi.getIpNum(req);
    }

    private WebResponse buildSceneManageRef(SceneManageWrapperVO wrapperVO, Long userId) {
        // ????????????id???
        Long scriptId = 0L;
        boolean hasScriptId = false;
        if (wrapperVO.getConfigType() != null && wrapperVO.getConfigType() == 2) {
            scriptId = wrapperVO.getScriptId();
            hasScriptId = true;
        } else {
            // ??????????????????????????????????????????????????????
            List<SceneBusinessActivityRefVO> list = wrapperVO.getBusinessActivityConfig();
            if (CollectionUtils.isNotEmpty(list) && list.size() == 1) {
                scriptId = list.get(0).getScriptId();
                hasScriptId = true;
            }
        }
        if (!hasScriptId) {
            WebResponse.fail("scriptId can not be null !");
        }

        // ??????scriptId??????????????????
        ScriptManageDeployDetailResponse scriptManageDeployDetail = scriptManageService.getScriptManageDeployDetail(
            scriptId);
        if (scriptManageDeployDetail == null) {
            return WebResponse.fail("500", "?????????????????????!");
        }

        // ??????????????????
        wrapperVO.setScriptType(scriptManageDeployDetail.getType());
        // ??????????????????
        List<SceneScriptRefOpen> scriptList = this.buildScriptRef(scriptManageDeployDetail);
        WebResponse<List<SceneScriptRefOpen>> response = this.checkScript(scriptManageDeployDetail.getType(), scriptList);
        if (!response.getSuccess()) {
            return response;
        }
        List<SceneScriptRefOpen> execList = response.getData();

        List<SceneBusinessActivityRef> businessActivityList =
            this.buildSceneBusinessActivityRef(wrapperVO.getBusinessActivityConfig());

        // ???????????????????????????????????????????????????????????????????????????????????????
        if (ScriptTypeEnum.JMETER.getCode().equals(wrapperVO.getScriptType())) {
            ScriptCheckDTO checkDTO = this.checkScriptAndActivity(wrapperVO.getScriptType(), true, businessActivityList,
                execList);
            if (StringUtils.isNoneBlank(checkDTO.getErrmsg())) {
                return WebResponse.fail("500", checkDTO.getErrmsg());
            }
        }

        //??????????????????
        Map<Long, SceneBusinessActivityRef> dataMap = ListHelper.transferToMap(businessActivityList,
            SceneBusinessActivityRef::getBusinessActivityId, data -> data);
        wrapperVO.getBusinessActivityConfig().forEach(vo -> {
            SceneBusinessActivityRef ref = dataMap.get(vo.getBusinessActivityId());
            if (ref != null) {
                vo.setBindRef(ref.getBindRef());
                vo.setApplicationIds(ref.getApplicationIds());
            }
        });
        WebResponse webResponse = new WebResponse();
        webResponse.setSuccess(true);
        webResponse.setData(scriptList);
        return webResponse;
    }

    @Override
    public WebResponse updateScene(SceneManageWrapperVO vo, Long uid) {

        //??????????????????
        SceneSchedulerTaskResponse dbData = sceneSchedulerTaskService.selectBySceneId(vo.getId());
        if (vo.getIsScheduler() != null && vo.getIsScheduler() == true &&
            vo.getExecuteTime()!=null) {
            if (dbData != null) {
                Date executeTime = dbData.getExecuteTime();
                if (executeTime.compareTo(vo.getExecuteTime())!=0) {
                    SceneSchedulerTaskUpdateRequest updateParam = new SceneSchedulerTaskUpdateRequest();
                    updateParam.setId(dbData.getId());
                    updateParam.setExecuteTime(vo.getExecuteTime());
                    updateParam.setUserId(RestContext.getUser().getId());
                    sceneSchedulerTaskService.update(updateParam,true);
                }
            } else {
                SceneSchedulerTaskCreateRequest insertParam = new SceneSchedulerTaskCreateRequest();
                insertParam.setSceneId(vo.getId());
                insertParam.setExecuteTime(vo.getExecuteTime());
                insertParam.setUserId(RestContext.getUser().getId());
                sceneSchedulerTaskService.insert(insertParam);
            }

        } else if (dbData != null) {
            sceneSchedulerTaskService.deleteBySceneId(dbData.getSceneId());
        }

        if (!Objects.isNull(vo.getScheduleInterval())) {
            Long scheduleInterval = convertTime(Long.parseLong(String.valueOf(vo.getScheduleInterval())),
                TimeUnitEnum.MINUTE.getValue());
            TimeVO timeVo = vo.getPressureTestTime();
            Long pressureTestSecond = this.convertTime(timeVo.getTime(), timeVo.getUnit());
            if (scheduleInterval > pressureTestSecond) {
                return WebResponse.fail("500", "????????????????????????????????????????????????");
            }
        }

        SceneManageWrapperReq req = new SceneManageWrapperReq();
        WebResponse webResponse = this.sceneManageVo2Req(vo, req, uid);
        vo.setUploadFile(this.sceneScriptRefOpenToVoList(req.getUploadFile()));
        ResponseResult cloudResult = sceneManageApi.updateScene(req);
        webResponse.setData(cloudResult.getData());
        webResponse.setSuccess(cloudResult.getSuccess());

        ErrorInfo errorInfo = new ErrorInfo();
        if (cloudResult.getError() != null) {
            errorInfo.setMsg(cloudResult.getError().getMsg());
            webResponse.setError(errorInfo);
        }
        webResponse.setTotal(cloudResult.getTotalNum());
        return webResponse;
    }

    public SceneScriptRefVO sceneScriptRefOpenToVo(SceneScriptRefOpen open) {
        SceneScriptRefVO vo = new SceneScriptRefVO();
        BeanUtils.copyProperties(open, vo);
        return vo;
    }

    public List<SceneScriptRefVO> sceneScriptRefOpenToVoList(List<SceneScriptRefOpen> source) {
        List<SceneScriptRefVO> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(source)) {
            source.forEach(open -> list.add(sceneScriptRefOpenToVo(open)));
        }
        return list;
    }

    @Override
    public WebResponse deleteScene(SceneManageIdVO vo) {
        vo.setRequestUrl(RemoteConstant.SCENE_MANAGE_URL);
        vo.setHttpMethod(HttpMethod.DELETE);
        return httpWebClient.request(vo);
    }

    @Override
    public ResponseResult<SceneManageWrapperResp> detailScene(Long id) {
        SceneManageIdReq req = new SceneManageIdReq();
        req.setId(id);
        // cloud ??????????????????
        ResponseResult<SceneManageWrapperResp> sceneDetail = sceneManageApi.getSceneDetail(req);
        if (sceneDetail != null && sceneDetail.getSuccess()) {
            SceneManageWrapperResp data = sceneDetail.getData();
            //????????????????????????
            SceneSchedulerTaskResponse sceneScheduler = sceneSchedulerTaskService.selectBySceneId(id);
            if (sceneScheduler != null && sceneScheduler.getIsDeleted() == false) {
                Date executeTime = sceneScheduler.getExecuteTime();
                if (executeTime!=null) {
                    data.setIsScheduler(true);
                    data.setExecuteTime(DateUtils.dateToString(executeTime,DateUtils.FORMATE_YMDHMS));
                }
            } else {
                data.setIsScheduler(false);
            }
            sceneDetail.setData(data);
        }

        return sceneDetail;
    }

    @Override
    public ScriptCheckDTO checkBusinessActivityAndScript(SceneManageWrapperResp sceneData) {
        int scriptType = sceneData.getScriptType();

        List<SceneScriptRefOpen> scriptList = Lists.newArrayList();
        sceneData.getUploadFile().forEach(data -> scriptList.add(buildSceneScriptRef(data)));
        ScriptCheckDTO dto = new ScriptCheckDTO();
        try {
            WebResponse<List<SceneScriptRefOpen>> response = checkScript(scriptType, scriptList);
            if (!response.getSuccess()) {
                dto.setErrmsg(response.getError().getMsg());
                return dto;
            }
            List<SceneScriptRefOpen> execList = response.getData();
            List<SceneBusinessActivityRef> businessActivityList = Lists.newArrayList();
            sceneData.getBusinessActivityConfig().forEach(
                data -> businessActivityList.add(buildSceneBusinessActivityRef(data)));
            if (0 == scriptType) {
                dto = checkScriptAndActivity(scriptType, false, businessActivityList, execList);
            }
        } catch (ApiException apiException) {
            dto.setMatchActivity(false);
            dto.setPtTag(false);
        }
        return dto;
    }

    private SceneBusinessActivityRef buildSceneBusinessActivityRef(SceneBusinessActivityRefResp refResp) {
        SceneBusinessActivityRef activityRef = new SceneBusinessActivityRef();
        activityRef.setBusinessActivityId(refResp.getBusinessActivityId());
        activityRef.setBusinessActivityName(refResp.getBusinessActivityName());
        return activityRef;
    }

    private SceneScriptRefOpen buildSceneScriptRef(SceneScriptRefResp scriptRefResp) {
        SceneScriptRefOpen scriptRef = new SceneScriptRefOpen();
        scriptRef.setFileType(scriptRefResp.getFileType());
        scriptRef.setIsDeleted(scriptRefResp.getIsDeleted());
        scriptRef.setId(scriptRefResp.getId());
        scriptRef.setUploadPath(scriptRefResp.getUploadPath());
        return scriptRef;
    }

    /**
     * ?????????????????? ??????
     *
     * @param voList ????????????
     * @return ??????????????????
     */
    private List<SceneBusinessActivityRef> buildSceneBusinessActivityRef(List<SceneBusinessActivityRefVO> voList) {
        List<SceneBusinessActivityRef> businessActivityList = Lists.newArrayList();
        voList.forEach(data -> {
            SceneBusinessActivityRef ref = new SceneBusinessActivityRef();
            ref.setBusinessActivityId(data.getBusinessActivityId());
            ref.setBusinessActivityName(data.getBusinessActivityName());
            List<String> ids = getAppIdsByNameAndUser(getAppsbyId(data.getBusinessActivityId()), null);
            ref.setApplicationIds(convertListToString(ids));
            ref.setGoalValue(buildGoalValue(data));
            businessActivityList.add(ref);
        });
        return businessActivityList;
    }

    private String buildGoalValue(SceneBusinessActivityRefVO vo) {
        Map<String, Object> map = Maps.newHashMap();
        map.put(SceneManageConstant.TPS, vo.getTargetTPS());
        map.put(SceneManageConstant.RT, vo.getTargetRT());
        map.put(SceneManageConstant.SUCCESS_RATE, vo.getTargetSuccessRate());
        map.put(SceneManageConstant.SA, vo.getTargetSA());
        return JSON.toJSONString(map);
    }

    /**
     * ??????????????????
     *
     * @param scriptManageDeployDetail ??????????????????
     * @return ????????????
     */
    private List<SceneScriptRefOpen> buildScriptRef(ScriptManageDeployDetailResponse scriptManageDeployDetail) {
        List<FileManageResponse> fileManageResponseList = scriptManageDeployDetail.getFileManageResponseList();
        if (CollectionUtils.isNotEmpty(scriptManageDeployDetail.getAttachmentManageResponseList())) {
            fileManageResponseList.addAll(scriptManageDeployDetail.getAttachmentManageResponseList());
        }

        if (CollectionUtils.isEmpty(fileManageResponseList)) {
            return Collections.emptyList();
        }

        return fileManageResponseList.stream().map(file -> {
            SceneScriptRefOpen ref = new SceneScriptRefOpen();
            ref.setId(file.getId());
            ref.setFileType(scriptManageDeployDetail.getType());
            ref.setFileName(file.getFileName());
            ref.setFileType(file.getFileType());
            ref.setFileSize(file.getFileSize());
            ref.setUploadPath(file.getUploadPath());
            ref.setUploadedData(file.getDataCount());
            ref.setIsSplit(file.getIsSplit());

            Map<String, Object> extend = new HashMap<>(4);
            extend.put(SceneManageConstant.DATA_COUNT, file.getDataCount());
            extend.put(SceneManageConstant.IS_SPLIT, file.getIsSplit());
            ref.setFileExtend(JsonUtil.bean2Json(extend));

            ref.setIsDeleted(file.getIsDeleted());
            ref.setUploadTime(DateUtils.transferDateToString(file.getUploadTime()));
            return ref;
        })
            .collect(Collectors.toList());
    }

    /**
     * ?????????????????????
     *
     * @param scriptType ????????????
     * @param absolutePath ????????????
     * @param businessActivityList ??????????????????
     * @param scriptList ????????????
     * @return dto
     */
    private ScriptCheckDTO checkScriptAndActivity(Integer scriptType, boolean absolutePath,
        List<SceneBusinessActivityRef> businessActivityList, List<SceneScriptRefOpen> scriptList) {
        ScriptCheckDTO dto = new ScriptCheckDTO();
        if (scriptType == null) {
            return new ScriptCheckDTO(false, false, "???????????????");
        }
        if (scriptType == 1) {
            return new ScriptCheckDTO();
        }
        if (scriptCheck == null || !scriptCheck) {
            return new ScriptCheckDTO();
        }
        SceneScriptRefOpen sceneScriptRef = scriptList.get(0);

        SceneParseVO vo = new SceneParseVO();
        vo.setScriptId(sceneScriptRef.getId());
        vo.setUploadPath(sceneScriptRef.getUploadPath());
        vo.setRequestUrl(RemoteConstant.SCENE_MANAGE_PARSE_URL);
        vo.setHttpMethod(HttpMethod.GET);
        vo.setAbsolutePath(absolutePath);
        WebResponse response = httpWebClient.request(vo);

        Map<String, Object> dataMap = (Map<String, Object>)response.getData();
        List<Map<String, Object>> voList = (List<Map<String, Object>>)dataMap.getOrDefault("requestUrl",
            new ArrayList<>());
        log.info("????????????????????????ScriptRefID???{}", sceneScriptRef.getId());

        int ptSize = 0;
        if (dataMap.containsKey("ptSize")) {
            ptSize = (int)dataMap.get("ptSize");
        }
        List<Long> businessActivityIds = businessActivityList.stream().map(
            SceneBusinessActivityRef::getBusinessActivityId)
            .distinct().collect(Collectors.toList());
        Map<Long, String> urlMap = ListHelper.transferToMap(
            TBusinessLinkManageTableMapper.selectByPrimaryKeys(businessActivityIds), BusinessLinkManageTable::getLinkId,
            BusinessLinkManageTable::getEntrace);

        StringBuffer sb = new StringBuffer();
        businessActivityList.forEach(data -> {
            String url = urlMap.get(data.getBusinessActivityId());
            if (url == null) {
                return;
            }
            EntranceJoinEntity entranceJoinEntity = ActivityUtil.covertEntrance(url);
            Map<String, String> map = UrlUtil.convertUrl(entranceJoinEntity);
            if (CollectionUtils.isEmpty(voList)) {
                log.error("tro-cloud????????????????????????url???????????????0???,????????????????????????????????????scriptRefId:{},uploadPath:{}",
                    sceneScriptRef.getId(), sceneScriptRef.getUploadPath());
                throw new RuntimeException("??????????????????????????????????????????????????????????????????");
            }
            for (Map<String, Object> temp : voList) {
                ScriptUrlVO urlVO = JSON.parseObject(JSON.toJSONString(temp), ScriptUrlVO.class);
                if (UrlUtil.checkEqual(map.get("url"), urlVO.getPath()) && urlVO.getEnable()) {
                    data.setBindRef(urlVO.getName());
                    break;
                }
            }
            if (StringUtils.isBlank(data.getBindRef())) {
                sb.append(data.getBusinessActivityName());
                sb.append(",");
            }
        });

        long unbindCount = businessActivityList.stream().filter(data -> data.getBindRef() == null).count();
        if (scriptPTCheck && voList.size() != ptSize) {
            dto.setPtTag(false);
            dto.setErrmsg("??????????????????");
        }
        if (unbindCount > 0) {
            dto.setMatchActivity(false);
            if (sb.length() != 0) {
                sb.deleteCharAt(sb.lastIndexOf(","));
                dto.setErrmsg("????????????????????????????????????:" + sb.toString());
            } else {
                dto.setErrmsg("?????????????????????????????????????????????????????????~");
            }
        }
        return dto;
    }

    /**
     * ????????????
     *
     * @param scriptType
     * @param scriptList
     * @return
     */
    private WebResponse<List<SceneScriptRefOpen>> checkScript(Integer scriptType, List<SceneScriptRefOpen> scriptList) {
        if (CollectionUtils.isEmpty(scriptList)) {
            return WebResponse.fail("500", "?????????????????????");
        }

        // ??????????????????
        List<SceneScriptRefOpen> execList = scriptList.stream()
            .filter(data -> data.getFileType() != null && FileTypeEnum.SCRIPT.getCode().equals(data.getFileType())
                && data.getIsDeleted() != null && data.getIsDeleted() == 0)
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(execList)) {
            return WebResponse.fail("500", "?????????????????????");
        }

        if (0 == scriptType && execList.size() > 1) {
            return WebResponse.fail("500", "JMeter???????????????????????????????????????");
        }

        return WebResponse.success(execList);
    }

    private List<String> getAppIdsByNameAndUser(List<String> nameList, Long userId) {
        if (CollectionUtils.isEmpty(nameList)) {
            return Collections.EMPTY_LIST;
        }
        return tApplicationMntDao.queryIdsByNameAndTenant(nameList, userId);
    }

    private List<String> getAppsbyId(Long id) {
        return applicationBusinessActivityService.processAppNameByBusinessActiveId(id);
    }

    private String convertListToString(List<String> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (String data : dataList) {
            sb.append(data);
            sb.append(",");
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }

    /**
     * ???????????????????????????
     *
     * @param time ??????
     * @param unit ??????
     * @return ????????? ?????????
     */
    private Long convertTime(Long time, String unit) {
        if (time == null) {
            return 0L;
        }

        long t;
        if (TimeUnitEnum.HOUR.getValue().equals(unit)) {
            t = time * 60 * 60;
        } else if (TimeUnitEnum.MINUTE.getValue().equals(unit)) {
            t = time * 60;
        } else {
            t = time;
        }
        return t;
    }

    @Override
    public WebResponse checkParam(SceneManageWrapperVO sceneVO) {
        List<SceneBusinessActivityRefVO> activityRefVOList = sceneVO.getBusinessActivityConfig();
        if (CollectionUtils.isEmpty(activityRefVOList)) {
            return WebResponse.fail("500", "??????????????????????????????");
        }
        for (SceneBusinessActivityRefVO sceneBusinessActivityRefVO : activityRefVOList) {
            if (Objects.isNull(sceneBusinessActivityRefVO.getTargetTPS())) {
                return WebResponse.fail("500", "??????TPS????????????");
            }
            if (Objects.isNull(sceneBusinessActivityRefVO.getTargetRT())) {
                return WebResponse.fail("500", "??????RT????????????");
            }
            if (Objects.isNull(sceneBusinessActivityRefVO.getTargetSuccessRate())) {
                return WebResponse.fail("500", "???????????????????????????");
            }
            if (Objects.isNull(sceneBusinessActivityRefVO.getTargetSA())) {
                return WebResponse.fail("500", "??????SA????????????");
            }
            if (sceneVO.getPressureType().equals(0) && sceneVO.getConcurrenceNum() < sceneVO.getIpNum()) {
                return WebResponse.fail("500", "???????????????????????????IP???");
            }
        }
        return WebResponse.success();
    }

    @Override
    public WebResponse buildSceneForFlowVerify(SceneManageWrapperVO vo, SceneManageWrapperReq req, Long userId) {
        return sceneManageVo2Req(vo, req, userId);
    }
}
