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

package io.shulie.tro.cloud.biz.service.scene.impl;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.alibaba.fastjson.JSONObject;

import cn.hutool.json.JSONUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pamirs.tro.entity.dao.report.TReportMapper;
import com.pamirs.tro.entity.dao.scenemanage.TSceneBusinessActivityRefMapper;
import com.pamirs.tro.entity.dao.scenemanage.TSceneManageMapper;
import com.pamirs.tro.entity.dao.scenemanage.TSceneScriptRefMapper;
import com.pamirs.tro.entity.dao.scenemanage.TSceneSlaRefMapper;
import com.pamirs.tro.entity.dao.user.TUserMapper;
import com.pamirs.tro.entity.domain.entity.scenemanage.SceneBusinessActivityRef;
import com.pamirs.tro.entity.domain.entity.scenemanage.SceneManage;
import com.pamirs.tro.entity.domain.entity.scenemanage.SceneRef;
import com.pamirs.tro.entity.domain.entity.scenemanage.SceneScriptRef;
import com.pamirs.tro.entity.domain.entity.scenemanage.SceneSlaRef;
import com.pamirs.tro.entity.domain.vo.scenemanage.SceneManageStartRecordVO;
import io.shulie.tro.cloud.biz.cloudserver.SceneManageDTOConvert;
import io.shulie.tro.cloud.biz.convertor.ConvertUtil;
import io.shulie.tro.cloud.biz.input.scenemanage.SceneBusinessActivityRefInput;
import io.shulie.tro.cloud.biz.input.scenemanage.SceneManageQueryInput;
import io.shulie.tro.cloud.biz.input.scenemanage.SceneManageWrapperInput;
import io.shulie.tro.cloud.biz.input.scenemanage.SceneScriptRefInput;
import io.shulie.tro.cloud.biz.input.scenemanage.SceneSlaRefInput;
import io.shulie.tro.cloud.biz.output.scenemanage.SceneManageListOutput;
import io.shulie.tro.cloud.biz.output.scenemanage.SceneManageWrapperOutput;
import io.shulie.tro.cloud.biz.output.scenemanage.SceneManageWrapperOutput.SceneBusinessActivityRefOutput;
import io.shulie.tro.cloud.biz.output.scenemanage.SceneManageWrapperOutput.SceneScriptRefOutput;
import io.shulie.tro.cloud.biz.output.scenemanage.SceneManageWrapperOutput.SceneSlaRefOutput;
import io.shulie.tro.cloud.biz.service.report.ReportService;
import io.shulie.tro.cloud.biz.service.scene.SceneManageService;
import io.shulie.tro.cloud.biz.utils.FileTypeBusinessUtil;
import io.shulie.tro.cloud.biz.utils.SaxUtil;
import io.shulie.tro.cloud.common.bean.RuleBean;
import io.shulie.tro.cloud.common.bean.TimeBean;
import io.shulie.tro.cloud.common.bean.scenemanage.SceneManageQueryBean;
import io.shulie.tro.cloud.common.bean.scenemanage.SceneManageQueryOpitons;
import io.shulie.tro.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.tro.cloud.common.constants.ReportConstans;
import io.shulie.tro.cloud.common.constants.SceneManageConstant;
import io.shulie.tro.cloud.common.constants.ScheduleConstants;
import io.shulie.tro.cloud.common.enums.PressureTypeEnums;
import io.shulie.tro.cloud.common.enums.TimeUnitEnum;
import io.shulie.tro.cloud.common.enums.scenemanage.SceneManageErrorEnum;
import io.shulie.tro.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.tro.cloud.common.exception.ApiException;
import io.shulie.tro.cloud.common.exception.TroCloudException;
import io.shulie.tro.cloud.common.exception.TroCloudExceptionEnum;
import io.shulie.tro.cloud.common.pojo.dto.scenemanage.UploadFileDTO;
import io.shulie.tro.cloud.common.pojo.vo.scenemanage.SceneMangeFeaturesVO;
import io.shulie.tro.cloud.common.redis.RedisClientUtils;
import io.shulie.tro.cloud.common.request.scenemanage.UpdateSceneFileRequest;
import io.shulie.tro.cloud.common.utils.DateUtil;
import io.shulie.tro.cloud.common.utils.FlowUtil;
import io.shulie.tro.cloud.common.utils.LinuxUtil;
import io.shulie.tro.cloud.common.utils.ListHelper;
import io.shulie.tro.cloud.data.dao.scenemanage.SceneManageDAO;
import io.shulie.tro.cloud.data.result.scenemanage.SceneManageListFromUpdateScriptResult;
import io.shulie.tro.eventcenter.EventCenterTemplate;
import io.shulie.tro.utils.file.FileManagerHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @ClassName SceneManageImpl
 * @Description
 * @Author qianshui
 * @Date 2020/4/17 ??????3:32
 */
@Service
@Slf4j
public class SceneManageServiceImpl implements SceneManageService {

    public static final String SCENE_MANAGE = "sceneManage";
    public static final String SCENE_BUSINESS_ACTIVITY = "sceneBusinessActivity";
    public static final String SCENE_SCRIPT = "sceneScript";
    public static final String SCENE_SLA = "sceneSla";
    @Resource
    private TSceneManageMapper TSceneManageMapper;
    @Resource
    private TSceneBusinessActivityRefMapper TSceneBusinessActivityRefMapper;
    @Resource
    private TSceneScriptRefMapper TSceneScriptRefMapper;
    @Resource
    private TSceneSlaRefMapper TSceneSlaRefMapper;
    @Resource
    private TReportMapper TReportMapper;
    @Autowired
    private EventCenterTemplate eventCenterTemplate;
    @Resource
    private TUserMapper tUserMapper;
    @Value("${script.temp.path}")
    private String scriptTempPath;
    @Value("${script.path}")
    private String scriptPath;
    @Autowired
    private RedisClientUtils redisClientUtils;
    @Autowired
    private ReportService reportService;

    @Autowired
    private SceneManageDAO sceneManageDAO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSceneManage(SceneManageWrapperInput wrapperRequest) {
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
        boolean isScriptManage = false;
        if (wrapperRequest.getFeatures() != null){
            JSONObject json = JSON.parseObject(wrapperRequest.getFeatures());
            isScriptManage = json.get(SceneManageConstant.FEATURES_SCRIPT_ID) != null;
        }
        Map<String, Object> maps = buildSceneManageRef(wrapperRequest);
        Long sceneId = saveToDB((SceneManage) maps.get(SCENE_MANAGE),
                (List<SceneBusinessActivityRef>) maps.get(SCENE_BUSINESS_ACTIVITY),
                (List<SceneScriptRef>) maps.get(SCENE_SCRIPT),
                (List<SceneSlaRef>) maps.get(SCENE_SLA),isScriptManage);

        //????????????????????????????????????
        if (isScriptManage){
            List<SceneScriptRefInput> uploadFileList = wrapperRequest.getUploadFile();
            if (CollectionUtils.isNotEmpty(uploadFileList)) {
                //???????????????????????????
                List<SceneScriptRefInput> normalFileList
                    = uploadFileList.stream().filter(sceneScriptRefInput -> sceneScriptRefInput.getFileType().equals(0)||sceneScriptRefInput.getFileType().equals(1)).collect(
                    Collectors.toList());
                String destPath = getDestPath(sceneId);
                for (SceneScriptRefInput file : normalFileList) {
                    String sourcePath = file.getUploadPath();
                    copyFile(sourcePath, destPath);
                }
                //??????
                List<SceneScriptRefInput> attachmentFileList
                    = uploadFileList.stream().filter(sceneScriptRefInput -> sceneScriptRefInput.getFileType().equals(2)).collect(
                    Collectors.toList());
                if(CollectionUtils.isNotEmpty(attachmentFileList)){
                    String attachmentPath = destPath +  SceneManageConstant.FILE_SPLIT + "attachments";
                    for (SceneScriptRefInput file : attachmentFileList) {
                        String sourcePath = file.getUploadPath();
                        copyFile(sourcePath, attachmentPath);
                    }
                }
            }
        }
        return sceneId;
    }

    @Override
    public String getDestPath (Long sceneId){
       return scriptPath + SceneManageConstant.FILE_SPLIT + sceneId + SceneManageConstant.FILE_SPLIT;
    }

    private void delDirFile(String dest) {
        File file = new File(dest);
        if (!file.isDirectory()) {
            file.delete();
        } else if (file.isDirectory()) {
            String[] filelist = file.list();
            for (int i = 0; i < filelist.length; i++) {
                File delfile = new File(dest + "/" + filelist[i]);
                if (!delfile.isDirectory()) {
                    delfile.delete();
                    log.info(delfile.getAbsolutePath() + "??????????????????");
                } else if (delfile.isDirectory()) {
                    delDirFile(dest + "/" + filelist[i]);
                }
            }
            log.info(file.getAbsolutePath() + "????????????");
            file.delete();
        }
    }


    private void copyFile(String source, String dest) {
        if (StringUtils.isBlank(dest) || StringUtils.isBlank(source)) {
            return;
        }

        File file = new File(dest.substring(0, dest.lastIndexOf("/")));
        if (!file.exists()) {
            file.mkdirs();
        }
        new Thread(() -> {
            try {
                FileManagerHelper.copyFiles(Collections.singletonList(source),dest);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }).start();
    }

    private Long saveToDB(SceneManage sceneManage, List<SceneBusinessActivityRef> businessActivityList,
                          List<SceneScriptRef> scriptList, List<SceneSlaRef> slaList,boolean isScriptManage) {
        //????????????????????????
        TSceneManageMapper.insertSelective(sceneManage);
        Long sceneId = sceneManage.getId();
        fillSceneId(businessActivityList, sceneId);
        fillSceneId(scriptList, sceneId);
        fillSceneId(slaList, sceneId);
        if (!isScriptManage){
            moveTempFile(scriptList, sceneId);
        }
        if (CollectionUtils.isNotEmpty(businessActivityList)) {
            TSceneBusinessActivityRefMapper.batchInsert(businessActivityList);
        }
        if (CollectionUtils.isNotEmpty(scriptList)) {
            if (isScriptManage){
                scriptList.stream().forEach(sceneScriptRef -> {
                    String uploadPath = sceneScriptRef.getUploadPath();
                    uploadPath = sceneId + "/" + uploadPath.substring(uploadPath.lastIndexOf("/") + 1);
                    sceneScriptRef.setUploadPath(uploadPath);
                });
            }else {
                scriptList = scriptList.stream().filter(data -> data.getUploadId() != null).collect(Collectors.toList());
            }
            if (CollectionUtils.isNotEmpty(scriptList)) {
                TSceneScriptRefMapper.batchInsert(scriptList);
            }
        }
        if (CollectionUtils.isNotEmpty(slaList)) {
            TSceneSlaRefMapper.batchInsert(slaList);
        }
        return sceneId;
    }

    @Override
    public PageInfo<SceneManageListOutput> queryPageList(SceneManageQueryInput queryVO) {
        if (StringUtils.isNoneBlank(queryVO.getCustomName())) {
            List<Long> customerIds = tUserMapper.selectIdsByName(queryVO.getCustomName());
            queryVO.setCustomIds(CollectionUtils.isNotEmpty(customerIds) ? customerIds : Lists.newArrayList(0L));
        }
        Page page = PageHelper.startPage(queryVO.getCurrentPage() + 1, queryVO.getPageSize());
        SceneManageQueryBean sceneManageQueryBean = new SceneManageQueryBean();
        BeanUtils.copyProperties(queryVO, sceneManageQueryBean);
        //???????????????????????????????????????????????????????????????
        if (sceneManageQueryBean.getType() == null){
            sceneManageQueryBean.setType(0);
        }
        List<SceneManage> queryList = TSceneManageMapper.getPageList(sceneManageQueryBean);
        if (CollectionUtils.isEmpty(queryList)) {
            return new PageInfo<>(Lists.newArrayList());
        }
        List<SceneManageListOutput> resultList = SceneManageDTOConvert.INSTANCE.ofs(queryList);
        Map<Long, Integer> threadNum = new HashMap<>();
        for (SceneManage sceneManage : queryList) {
            if (sceneManage.getPtConfig() == null) {
                continue;
            }
            JSONObject object = JSON.parseObject(sceneManage.getPtConfig());
            if (object.containsKey("threadNum")) {
                threadNum.put(sceneManage.getId(), object.getIntValue("threadNum"));
            }
        }
        for (SceneManageListOutput dto : resultList) {
            dto.setThreadNum(threadNum.get(dto.getId()));
        }
        List<Long> sceneIds = TReportMapper.listReportSceneIds(
                resultList.stream().map(data -> data.getId()).collect(Collectors.toList()))
                .stream().map(data -> data.getSceneId()).distinct().collect(Collectors.toList());
        resultList.stream().forEach(data -> data.setHasReport(sceneIds.contains(data.getId())));

        List<Long> customIds = resultList.stream().map(data -> data.getCustomId()).distinct().collect(
                Collectors.toList());
        if (CollectionUtils.isNotEmpty(customIds)) {
            Map<Long, String> userMap = ListHelper.transferToMap(tUserMapper.selectByIds(customIds),
                    data -> data.getId(), data -> data.getName());
            resultList.stream().forEach(data -> {
                data.setCustomName(data.getCustomId() != null ? userMap.get(data.getCustomId()) : null);
            });
        }
        // ????????????
        resultList.stream().forEach(data -> {
            data.setStatus(SceneManageStatusEnum.getAdaptStatus(data.getStatus()));
        });

        PageInfo pageInfo = new PageInfo<>(resultList);
        pageInfo.setTotal(page.getTotal());
        return pageInfo;
    }

    private Map<String, Object> buildSceneManageRef(SceneManageWrapperInput wrapperRequest) {
        List<SceneScriptRef> scriptList = buildScriptRef(wrapperRequest.getUploadFile(), wrapperRequest.getScriptType());
//        List<SceneBusinessActivityRefInput> inputs = SceneManageInputConvertor.INSTANCE.ofListSceneBusinessActivityRefInput(wrapperRequest.getBusinessActivityConfig());
        List<SceneBusinessActivityRef> businessActivityList = buildSceneBusinessActivityRef(wrapperRequest.getBusinessActivityConfig());
        SceneManage sceneManage = buildSceneManage(wrapperRequest);
        List<SceneSlaRef> slaList = Lists.newArrayList();
        slaList.addAll(buildSceneSlaRef(wrapperRequest.getStopCondition(), SceneManageConstant.EVENT_DESTORY));
        slaList.addAll(buildSceneSlaRef(wrapperRequest.getWarningCondition(), SceneManageConstant.EVENT_WARN));
        Map<String, Object> maps = Maps.newHashMap();
        maps.put(SCENE_MANAGE, sceneManage);
        maps.put(SCENE_BUSINESS_ACTIVITY, businessActivityList);
        maps.put(SCENE_SCRIPT, scriptList);
        maps.put(SCENE_SLA, slaList);
        return maps;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSceneManage(SceneManageWrapperInput wrapperRequest) {
        SceneManageWrapperOutput oldScene = getSceneManage(wrapperRequest.getId(), null);
        boolean isScriptManage = false;
        if (wrapperRequest.getFeatures() != null){
            JSONObject json = JSON.parseObject(wrapperRequest.getFeatures());
            isScriptManage = json.get(SceneManageConstant.FEATURES_SCRIPT_ID) != null;
        }
        if (oldScene == null) {
            throw ApiException.create(500, "??????????????????????????????id=" + wrapperRequest.getId());
        }
        boolean fileNeedChange = false;
        if (isScriptManage){
            JSONObject json = JSON.parseObject(oldScene.getFeatures());
            JSONObject newFeatures = JSON.parseObject(wrapperRequest.getFeatures());
            if (newFeatures != null && json != null && json.get(SceneManageConstant.FEATURES_SCRIPT_ID) != null && newFeatures.get(SceneManageConstant.FEATURES_SCRIPT_ID) != null
                    && json.getLongValue(SceneManageConstant.FEATURES_SCRIPT_ID) != newFeatures.getLongValue(SceneManageConstant.FEATURES_SCRIPT_ID)) {
                fileNeedChange = true;
            }
        }

        Map<String, Object> maps = buildSceneManageRef(wrapperRequest);
        updateToDB((SceneManage) maps.get(SCENE_MANAGE),
                (List<SceneBusinessActivityRef>) maps.get(SCENE_BUSINESS_ACTIVITY),
                (List<SceneScriptRef>) maps.get(SCENE_SCRIPT),
                (List<SceneSlaRef>) maps.get(SCENE_SLA),isScriptManage);

        //?????????????????????????????????????????????copy
        Long sceneId = wrapperRequest.getId();
        if (isScriptManage && fileNeedChange && StringUtils.isNotBlank(scriptPath) && sceneId != null) {
            this.operateFileOnSystem(wrapperRequest.getUploadFile(), sceneId);
        }
    }

    private void updateToDB(SceneManage sceneManage, List<SceneBusinessActivityRef> businessActivityList,
                            List<SceneScriptRef> scriptList, List<SceneSlaRef> slaList,boolean isScriptManage) {
        TSceneManageMapper.updateByPrimaryKeySelective(sceneManage);
        Long sceneId = sceneManage.getId();
        fillSceneId(businessActivityList, sceneId);
        fillSceneId(scriptList, sceneId);
        fillSceneId(slaList, sceneId);
        TSceneBusinessActivityRefMapper.deleteBySceneId(sceneId);
        if (CollectionUtils.isNotEmpty(businessActivityList)) {
            TSceneBusinessActivityRefMapper.batchInsert(businessActivityList);
        }
        TSceneSlaRefMapper.deleteBySceneId(sceneId);
        if (CollectionUtils.isNotEmpty(slaList)) {
            TSceneSlaRefMapper.batchInsert(slaList);
        }
        if (isScriptManage){
            for (SceneScriptRef ref : scriptList) {
                ref.setUploadPath(sceneId + "/" + ref.getFileName());
            }
        }
        if (isScriptManage){
            dealScriptRefFileByScriptManage(scriptList,sceneId);
        }else {
            dealScriptRefFile(scriptList,sceneId);
        }


    }

    /**
     * 4.5.0??????????????????????????????????????????
     * @param scriptList
     * @param sceneId
     */
    private void dealScriptRefFile(List<SceneScriptRef> scriptList,Long sceneId) {
        if (CollectionUtils.isNotEmpty(scriptList)) {
            List<Long> ids = scriptList.stream().filter(data -> data.getId() != null && 1 == data.getIsDeleted()).map(
                    data -> data.getId()).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ids)) {
                TSceneScriptRefMapper.deleteByIds(ids);
                //??????????????????????????????
                delCache(ids);
                //????????????/????????????
                List<String> files = scriptList.stream().filter(
                        data -> data.getId() != null && 1 == data.getIsDeleted()).map(data -> data.getUploadPath()).collect(
                        Collectors.toList());
                deleteUploadFile(files);
            }

            scriptList = scriptList.stream().filter(data -> data.getUploadId() != null).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(scriptList)) {
                moveTempFile(scriptList, sceneId);
                TSceneScriptRefMapper.batchInsert(scriptList);
            }
        }
    }

    /**
     * ????????????????????????????????????
     * @param scriptList
     * @param sceneId
     */
    private void dealScriptRefFileByScriptManage(List<SceneScriptRef> scriptList,Long sceneId){
        if (CollectionUtils.isNotEmpty(scriptList)) {
            //?????????????????????????????????????????????
            TSceneScriptRefMapper.deleteBySceneId(sceneId);
            List<Long> ids = scriptList.stream().filter(data -> data.getId() != null && 1 == data.getIsDeleted()).map(
                    data -> data.getId()).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ids)) {
                TSceneScriptRefMapper.deleteByIds(ids);
                //??????????????????????????????
                delCache(ids);
            }
            if (CollectionUtils.isNotEmpty(scriptList)) {
                TSceneScriptRefMapper.batchInsert(scriptList);
            }
        }
    }

    /**
     * ??????????????????????????????
     */
    public void delCache(List<Long> fileIds) {
        if (fileIds != null && fileIds.size() > 0) {
            for (Long fileId : fileIds) {
                SceneScriptRef sceneScriptRef = TSceneScriptRefMapper.selectByPrimaryKey(fileId);
                try {
                    if (sceneScriptRef != null) {
                        //????????????????????????
                        StringBuilder bigFileStartPos = new StringBuilder();
                        bigFileStartPos.append(sceneScriptRef.getSceneId());
                        bigFileStartPos.append("-");
                        bigFileStartPos.append(sceneScriptRef.getFileName());
                        redisClientUtils.delete(bigFileStartPos.toString());

                        //???????????????
                        bigFileStartPos.append("-NUM");
                        redisClientUtils.delete(bigFileStartPos.toString());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void updateSceneManageStatus(UpdateStatusBean statusVO) {
        TSceneManageMapper.updateStatus(statusVO);
    }

    @Override
    public Boolean updateSceneLifeCycle(UpdateStatusBean statusVO) {
        String checkStatus = Arrays.stream(statusVO.getCheckEnum()).map(SceneManageStatusEnum::getDesc).collect(
                Collectors.joining(","));
        String updateStatus = statusVO.getUpdateEnum().getDesc();

        SceneManage scene = TSceneManageMapper.selectByPrimaryKey(statusVO.getSceneId());
        if (scene == null) {
            log.error("??????{}????????????????????????????????????", statusVO.getSceneId());
            toFailureState(statusVO.getSceneId(), statusVO.getResultId(),
                    SceneManageErrorEnum.SCENEMANAGE_UPDATE_LIFECYCLE_NOT_FIND_SCENE.getErrorMessage());
            return false;
        }

        SceneManageStatusEnum statusEnum = SceneManageStatusEnum.getSceneManageStatusEnum(scene.getStatus());
        if (statusEnum == null) {
            log.error("??????{}?????????????????????????????????????????????", statusVO.getSceneId());
            toFailureState(statusVO.getSceneId(), statusVO.getResultId(),
                    SceneManageErrorEnum.SCENEMANAGE_UPDATE_LIFECYCLE_UNKNOWN_STATE.getErrorMessage());
            return false;
        }
        String statusMsg = statusEnum.getDesc();

        if (!Arrays.asList(statusVO.getCheckEnum()).contains(statusEnum)) {
            log.error("???????????? {}-{}-{} ??????????????????????????????????????????{} -> {},check:{}", statusVO.getSceneId(), statusVO.getResultId(),
                    statusVO.getCustomerId(), statusMsg, updateStatus, checkStatus);
            toFailureState(statusVO.getSceneId(), statusVO.getResultId(),
                    SceneManageErrorEnum.SCENEMANAGE_UPDATE_LIFECYCLE_CHECK_FAILED.getErrorMessage());
            return false;
        }

        try {
            SceneManage sceneManage = new SceneManage();
            sceneManage.setLastPtTime(new Date());
            sceneManage.setId(statusVO.getSceneId());
            sceneManage.setUpdateTime(new Date());
            // --->update
            sceneManage.setStatus(statusVO.getUpdateEnum().getValue());
            // ??????????????????????????? ????????? ??????????????????
            if (statusVO.getUpdateEnum().equals(SceneManageStatusEnum.ENGINE_RUNNING)) {
                String engineName = ScheduleConstants.getEngineName(statusVO.getSceneId(), statusVO.getResultId(),
                        statusVO.getCustomerId());
                String startTime = engineName + ScheduleConstants.FIRST_SIGN;
                TReportMapper.updateStartTime(statusVO.getResultId(), new Date(Long.valueOf(
                        Optional.ofNullable(redisClientUtils.getString(startTime))
                                .orElse(String.valueOf(System.currentTimeMillis())))));
            }

            //   ????????????startTime ?????????????????????????????????????????????????????????
            //        report.setStartTime(new Date());
            TSceneManageMapper.updateByPrimaryKeySelective(sceneManage);
            log.info("????????????{}-{}-{} ??????????????????????????????????????????{} -> {},check:{}", statusVO.getSceneId(), statusVO.getResultId(),
                    statusVO.getCustomerId(), statusMsg, updateStatus, checkStatus);
        } catch (Exception e) {
            log.error("????????????{}-{}-{} ??????????????????????????????????????????{} -> {},check:{}", statusVO.getSceneId(), statusVO.getResultId(),
                    statusVO.getCustomerId(), statusMsg, updateStatus, checkStatus, e);
            toFailureState(statusVO.getSceneId(), statusVO.getResultId(), "??????????????????" + e.getLocalizedMessage());
            return false;

        }
        return true;
    }

    /**
     * ???????????????
     */
    private void toFailureState(Long sceneId, Long reportId, String errorMsg) {
        // ?????? ?????? ????????????
        SceneManage sceneManage = new SceneManage();
        sceneManage.setLastPtTime(new Date());
        sceneManage.setId(sceneId);
        sceneManage.setUpdateTime(new Date());
        // --->update ????????????
        sceneManage.setStatus(SceneManageStatusEnum.FAILED.getValue());
        TSceneManageMapper.updateByPrimaryKeySelective(sceneManage);

        // ???????????????????????????????????????????????? ??????????????????
        reportService.updateReportFeatures(reportId, ReportConstans.FINISH_STATUS, ReportConstans.PRESSURE_MSG,
                errorMsg);
    }

    @Override
    public void reportRecord(SceneManageStartRecordVO recordVO) {
        if (!existSceneManage(recordVO.getSceneId())) {
            log.error("??????{}?????????", recordVO.getSceneId());
            toFailureState(recordVO.getSceneId(), recordVO.getResultId(),
                    String.format("??????%s?????????", recordVO.getSceneId()));
        }
        if (!recordVO.getSuccess()) {
            // ?????????????????????
            toFailureState(recordVO.getSceneId(), recordVO.getResultId(), recordVO.getErrorMsg());
        }
    }

    @Override
    public List<SceneManageListOutput> querySceneManageList() {
        List<SceneManage> sceneManages = TSceneManageMapper.selectAllSceneManageList();
        if (CollectionUtils.isNotEmpty(sceneManages)) {
            return SceneManageDTOConvert.INSTANCE.ofs(sceneManages);
        }
        return null;
    }

    /**
     * ????????????-???????????????
     *
     * @param dataId
     * @param userId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int allocationUser(Long dataId, Long userId) {
        if (null == dataId || null == userId) {
            return 0;
        }
        return TSceneManageMapper.updateSceneUserById(dataId, userId);
    }

    @Override
    public void updateFileByScriptId(UpdateSceneFileRequest request) {
        // ???????????????????????????????????????
        log.info("??????????????????????????? --> ???????????????, ???????????????????????????");
        List<SceneManageListFromUpdateScriptResult> sceneManageList = sceneManageDAO.listFromUpdateScript();
        if (sceneManageList.isEmpty()) {
            return;
        }

        // ?????? scriptId ??????, ????????? sceneId
        List<Long> scriptIdAboutSceneIds = sceneManageList.stream()
            .filter(sceneManage -> {
                // ????????????
                String features = sceneManage.getFeatures();
                if (StringUtils.isBlank(features)) {
                    return false;
                }

                // ?????? scriptId
                SceneMangeFeaturesVO featuresVO = JSONUtil.toBean(features, SceneMangeFeaturesVO.class);
                // ?????????, ?????????????????? null
                return Objects.equals(featuresVO.getScriptId(), request.getScriptId());
            })
            .map(SceneManageListFromUpdateScriptResult::getId)
            .collect(Collectors.toList());

        if (scriptIdAboutSceneIds.isEmpty()) {
            return;
        }

        // ????????????
        this.doUpdateFileByScriptId(request, scriptIdAboutSceneIds);
        log.info("??????????????????????????? --> ??????????????????");
    }

    @Override
    public List<SceneManageWrapperOutput> getByIds(List<Long> sceneIds) {
        if (CollectionUtils.isEmpty(sceneIds)) {
            return Lists.newArrayList();

        }
        List<SceneManage> byIds = TSceneManageMapper.getByIds(sceneIds);
        List<SceneManageWrapperOutput> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(byIds)) {
            byIds.stream().forEach(sceneManage -> {
                SceneManageWrapperOutput output = new SceneManageWrapperOutput();
                output.setPressureTestSceneName(sceneManage.getSceneName());
                output.setId(sceneManage.getId());
                result.add(output);
            });
        }
        return result;
    }


    /**
     * ????????????
     *
     * @param request         ????????????
     * @param scriptIdAboutSceneIds ??????id???????????????ids
     */
    private void doUpdateFileByScriptId(UpdateSceneFileRequest request, List<Long> scriptIdAboutSceneIds) {
        // ??????????????????
        List<UploadFileDTO> uploadFiles = request.getUploadFiles();
        List<SceneScriptRefInput> inputList = this.uploadFiles2InputList(uploadFiles);

        log.info("??????????????????????????? --> ??????????????????");
        // ???????????????
        List<SceneScriptRef> sceneScriptList = this.buildScriptRef(inputList, request.getScriptType());
        for (Long sceneId : scriptIdAboutSceneIds) {
            // ????????????????????????
            for (SceneScriptRef sceneScript : sceneScriptList) {
                sceneScript.setUploadPath(sceneId + "/" + sceneScript.getFileName());
                sceneScript.setSceneId(sceneId);
            }

            // ??????
            this.dealScriptRefFileByScriptManage(sceneScriptList, sceneId);
            if (StringUtils.isBlank(scriptPath)) {
                continue;
            }

            this.operateFileOnSystem(inputList, sceneId);
        }
    }

    /**
     * ?????????????????????
     *
     * @param inputList ????????????
     * @param sceneId ??????id
     */
    private void operateFileOnSystem(List<SceneScriptRefInput> inputList, Long sceneId) {
        String destPath = scriptPath + SceneManageConstant.FILE_SPLIT + sceneId + SceneManageConstant.FILE_SPLIT;
        this.delDirFile(scriptPath + SceneManageConstant.FILE_SPLIT + sceneId);

        // ???????????????????????????
        List<SceneScriptRefInput> normalFileList = inputList.stream()
            .filter(input -> FileTypeBusinessUtil.isScriptOrData(input.getFileType()))
            .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(normalFileList)) {
            for (SceneScriptRefInput file : normalFileList) {
                this.copyFile(file.getUploadPath(), destPath);
            }
        }

        // ??????
        List<SceneScriptRefInput> attachmentFileList = inputList.stream()
            .filter(input -> FileTypeBusinessUtil.isAttachment(input.getFileType()))
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(attachmentFileList)) {
            String attachmentPath = destPath + SceneManageConstant.FILE_SPLIT + "attachments";
            for (SceneScriptRefInput file : attachmentFileList) {
                this.copyFile(file.getUploadPath(), attachmentPath);
            }
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param uploadFiles ????????????
     * @return ???????????????
     */
    private List<SceneScriptRefInput> uploadFiles2InputList(List<UploadFileDTO> uploadFiles) {
        return uploadFiles.stream().map(uploadFile -> {
                SceneScriptRefInput input = new SceneScriptRefInput();
                BeanUtils.copyProperties(uploadFile, input);
                return input;
            })
            .collect(Collectors.toList());
    }

    private Boolean existSceneManage(Long sceneId) {
        return TSceneManageMapper.selectByPrimaryKey(sceneId) != null;
    }

    @Override
    public void delete(Long id) {
        TSceneManageMapper.deleteByPrimaryKey(id);
    }

    @Override
    public SceneManageWrapperOutput getSceneManage(Long id, SceneManageQueryOpitons options) {
        SceneManage sceneManage = getSceneManage(id);
        if (options == null) {
            options = new SceneManageQueryOpitons();
        }
        SceneManageWrapperOutput wrapperDTO = new SceneManageWrapperOutput();
        fillBase(wrapperDTO, sceneManage);
        if (Boolean.TRUE.equals(options.getIncludeBusinessActivity())) {
            List<SceneBusinessActivityRef> businessActivityList = TSceneBusinessActivityRefMapper.selectBySceneId(id);
            List<SceneBusinessActivityRefOutput> dtoList = SceneManageDTOConvert.INSTANCE.ofBusinessActivityList(
                    businessActivityList);
            wrapperDTO.setBusinessActivityConfig(dtoList);
        }

        if (Boolean.TRUE.equals(options.getIncludeScript())) {
            List<SceneScriptRef> scriptList = TSceneScriptRefMapper.selectBySceneIdAndScriptType(id,
                    wrapperDTO.getScriptType());
            List<SceneScriptRefOutput> dtoList = SceneManageDTOConvert.INSTANCE.ofScriptList(scriptList);
            wrapperDTO.setUploadFile(dtoList);
        }

        if (Boolean.TRUE.equals(options.getIncludeSLA())) {
            List<SceneSlaRef> slaList = TSceneSlaRefMapper.selectBySceneId(id);
            List<SceneSlaRefOutput> dtoList = SceneManageDTOConvert.INSTANCE.ofSlaList(slaList);
            wrapperDTO.setStopCondition(
                    dtoList.stream().filter(data -> SceneManageConstant.EVENT_DESTORY.equals(data.getEvent()))
                            .collect(Collectors.toList()));
            wrapperDTO.setWarningCondition(
                    dtoList.stream().filter(data -> SceneManageConstant.EVENT_WARN.equals(data.getEvent()))
                            .collect(Collectors.toList()));
        }

        wrapperDTO.setTotalTestTime(wrapperDTO.getPressureTestSecond());
        wrapperDTO.setManagerId(sceneManage.getUserId());
        return wrapperDTO;
    }

    private SceneManage getSceneManage(Long id) {
        if (id == null) {
            throw ApiException.create(500, "ID????????????");
        }
        SceneManage sceneManage = TSceneManageMapper.selectByPrimaryKey(id);
        if (sceneManage == null) {
            throw ApiException.create(500, "?????????????????????");
        }
        return sceneManage;
    }

    @Override
    public List<SceneBusinessActivityRefOutput> getBusinessActivityBySceneId(Long sceneId) {
        List<SceneBusinessActivityRef> businessActivityList = TSceneBusinessActivityRefMapper.selectBySceneId(sceneId);
        return SceneManageDTOConvert.INSTANCE.ofBusinessActivityList(businessActivityList);
    }

    @Override
    public BigDecimal calcEstimateFlow(SceneManageWrapperInput wrapperVO) {
        return FlowUtil.calcEstimateFlow(ConvertUtil.convert(wrapperVO));
    }

    @Override
    public Map<String, Object> parseScript(Long scriptId, String uploadPath, boolean absolutePath) {
        String path = "";
        if (!absolutePath) {
            path = scriptTempPath + SceneManageConstant.FILE_SPLIT + uploadPath;
            if (scriptId != null) {
                path = scriptPath + SceneManageConstant.FILE_SPLIT + uploadPath;
            }
        } else {
            path = uploadPath;
        }

        return SaxUtil.parseXML(path);
    }

    @Override
    public Map<String, Object> parseAndUpdateScript(Long scriptId, String uploadPath, boolean absolutePath) {
        String path = "";
        if (!absolutePath) {
            path = scriptTempPath + SceneManageConstant.FILE_SPLIT + uploadPath;
            if (scriptId != null) {
                path = scriptPath + SceneManageConstant.FILE_SPLIT + uploadPath;
            }
        } else {
            path = uploadPath;
        }
        return SaxUtil.parseAndUpdateXML(path);
    }


    private void fillBase(SceneManageWrapperOutput wrapperOutput, SceneManage sceneManage) {
        wrapperOutput.setId(sceneManage.getId());
        wrapperOutput.setScriptType(sceneManage.getScriptType());
        wrapperOutput.setPressureTestSceneName(sceneManage.getSceneName());
        wrapperOutput.setType(sceneManage.getType());
        // ????????????
        wrapperOutput.setStatus(SceneManageStatusEnum.getAdaptStatus(sceneManage.getStatus()));
        wrapperOutput.setCustomId(sceneManage.getCustomId());
        wrapperOutput.setUpdateTime(DateUtil.getDate(sceneManage.getUpdateTime(), DateUtil.YYYYMMDDHHMMSS));
        wrapperOutput.setLastPtTime(DateUtil.getDate(sceneManage.getLastPtTime(), DateUtil.YYYYMMDDHHMMSS));
        fillPtConfig(wrapperOutput, sceneManage);
        wrapperOutput.setFeatures(sceneManage.getFeatures());

    }

    private void fillPtConfig(SceneManageWrapperOutput wrapperOutput, SceneManage sceneManage) {
        try {
            JSONObject jsonObject = JSON.parseObject(sceneManage.getPtConfig());
            wrapperOutput.setConcurrenceNum(jsonObject.getInteger(SceneManageConstant.THREAD_NUM));
            wrapperOutput.setIpNum(jsonObject.getInteger(SceneManageConstant.HOST_NUM));
            Integer pressureType = jsonObject.getInteger(SceneManageConstant.PT_TYPE);
            wrapperOutput.setPressureType(pressureType == null ? 0 : pressureType);
            //????????????
            wrapperOutput.setPressureTestTime(new TimeBean(jsonObject.getLong(SceneManageConstant.PT_DURATION),
                    jsonObject.getString(SceneManageConstant.PT_DURATION_UNIT)));
            wrapperOutput.setPressureTestSecond(convertTime(jsonObject.getLong(SceneManageConstant.PT_DURATION),
                    jsonObject.getString(SceneManageConstant.PT_DURATION_UNIT)));
            wrapperOutput.setPressureMode(jsonObject.getInteger(SceneManageConstant.PT_MODE));
            //????????????
            wrapperOutput.setIncreasingTime(new TimeBean(jsonObject.getLong(SceneManageConstant.STEP_DURATION),
                    jsonObject.getString(SceneManageConstant.STEP_DURATION_UNIT)));
            wrapperOutput.setIncreasingSecond(convertTime(jsonObject.getLong(SceneManageConstant.STEP_DURATION),
                    jsonObject.getString(SceneManageConstant.STEP_DURATION_UNIT)));
            wrapperOutput.setStep(jsonObject.getInteger(SceneManageConstant.STEP));
            //??????????????????
            BigDecimal flow = jsonObject.getBigDecimal(SceneManageConstant.ESTIMATE_FLOW);
            wrapperOutput.setEstimateFlow(flow != null ? flow.setScale(2, RoundingMode.HALF_UP) : null);
        } catch (Exception e) {
            throw ApiException.create(500, "????????????json????????????");
        }
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    private Long convertTime(Long time, String unit) {
        if (time == null) {
            return 0L;
        }
        Long t = 0L;
        if (TimeUnitEnum.HOUR.getValue().equals(unit)) {
            t = time * 60 * 60;
        } else if (TimeUnitEnum.MINUTE.getValue().equals(unit)) {
            t = time * 60;
        } else {
            t = time;
        }
        return t;
    }

    private SceneManage buildSceneManage(SceneManageWrapperInput wrapperVO) {
        SceneManage sceneManage = new SceneManage();
        sceneManage.setId(wrapperVO.getId());
        sceneManage.setCustomId(wrapperVO.getCustomId());
        sceneManage.setSceneName(wrapperVO.getPressureTestSceneName());
        sceneManage.setScriptType(wrapperVO.getScriptType());
        sceneManage.setPtConfig(buildPtConfig(wrapperVO));
        sceneManage.setStatus(SceneManageStatusEnum.WAIT.getValue());
        sceneManage.setType(wrapperVO.getType() == null ? 0 : wrapperVO.getType());
        sceneManage.setFeatures(wrapperVO.getFeatures());
        sceneManage.setDeptId(wrapperVO.getDeptId());
        sceneManage.setUserId(wrapperVO.getUserId());
        return sceneManage;
    }

    private String buildPtConfig(SceneManageWrapperInput wrapperVO) {
        Map<String, Object> map = Maps.newHashMap();
        //?????????????????????????????????????????????
        if (wrapperVO.getPressureType() == null){
            wrapperVO.setPressureType(0);
        }
        map.put(SceneManageConstant.PT_TYPE,wrapperVO.getPressureType());
        if (PressureTypeEnums.isConcurrency(wrapperVO.getPressureType()) && wrapperVO.getConcurrenceNum() == null){
            throw new TroCloudException(TroCloudExceptionEnum.SCENEMANAGE_BULID_PARAM_ERROR,"???????????????????????????????????????");
        }
        map.put(SceneManageConstant.THREAD_NUM, wrapperVO.getConcurrenceNum());
        map.put(SceneManageConstant.HOST_NUM, wrapperVO.getIpNum());
        map.put(SceneManageConstant.PT_DURATION, wrapperVO.getPressureTestTime().getTime());
        map.put(SceneManageConstant.PT_DURATION_UNIT, wrapperVO.getPressureTestTime().getUnit());
        map.put(SceneManageConstant.PT_MODE, wrapperVO.getPressureMode());
        map.put(SceneManageConstant.STEP_DURATION,
                wrapperVO.getIncreasingTime() != null ? wrapperVO.getIncreasingTime().getTime() : null);
        map.put(SceneManageConstant.STEP_DURATION_UNIT,
                wrapperVO.getIncreasingTime() != null ? wrapperVO.getIncreasingTime().getUnit() : null);
        map.put(SceneManageConstant.STEP, wrapperVO.getStep());
        map.put(SceneManageConstant.ESTIMATE_FLOW, FlowUtil.calcEstimateFlow(ConvertUtil.convert(wrapperVO)));
        return JSON.toJSONString(map);
    }

    private List<SceneBusinessActivityRef> buildSceneBusinessActivityRef(List<SceneBusinessActivityRefInput> voList) {
        List<SceneBusinessActivityRef> businessActivityList = Lists.newArrayList();
        for (SceneBusinessActivityRefInput data : voList) {
            SceneBusinessActivityRef ref = new SceneBusinessActivityRef();
            ref.setBusinessActivityId(data.getBusinessActivityId());
            ref.setBusinessActivityName(data.getBusinessActivityName());
            ref.setBindRef(data.getBindRef());
            ref.setApplicationIds(data.getApplicationIds());
            ref.setGoalValue(buildGoalValue(data));
            businessActivityList.add(ref);
        }
        return businessActivityList;
    }

    private String buildGoalValue(SceneBusinessActivityRefInput vo) {
        Map<String, Object> map = Maps.newHashMap();
        map.put(SceneManageConstant.TPS, vo.getTargetTPS());
        map.put(SceneManageConstant.RT, vo.getTargetRT());
        map.put(SceneManageConstant.SUCCESS_RATE, vo.getTargetSuccessRate());
        map.put(SceneManageConstant.SA, vo.getTargetSA());
        return JSON.toJSONString(map);
    }

    private List<SceneScriptRef> buildScriptRef(List<SceneScriptRefInput> voList, Integer scriptType) {
        List<SceneScriptRef> scriptList = Lists.newArrayList();
        voList.forEach(data -> {
            SceneScriptRef ref = new SceneScriptRef();
            ref.setId(data.getId());
            ref.setScriptType(scriptType);
            ref.setUploadId(data.getUploadId());
            ref.setFileName(data.getFileName());
            ref.setFileType(data.getFileType());
            ref.setFileSize(data.getFileSize());
            if (data.getUploadId() != null) {
                ref.setUploadPath(data.getUploadId() + SceneManageConstant.FILE_SPLIT + data.getFileName());
            }
            if (data.getId() != null) {
                ref.setUploadPath(data.getUploadPath());
            }

            Map<String, Object> extend = Maps.newHashMap();
            extend.put(SceneManageConstant.DATA_COUNT, data.getUploadedData());
            extend.put(SceneManageConstant.IS_SPLIT, data.getIsSplit());
            extend.put(SceneManageConstant.TOPIC, data.getTopic());
            ref.setFileExtend(JSON.toJSONString(extend));

            ref.setIsDeleted(data.getIsDeleted());
            ref.setUploadTime(DateUtil.getDate(data.getUploadTime()));
            scriptList.add(ref);
        });
        return scriptList;
    }

    private List<SceneSlaRef> buildSceneSlaRef(List<SceneSlaRefInput> voList, String event) {
        List<SceneSlaRef> slaList = Lists.newArrayList();
        voList = voList.stream().filter(
                data -> data.getBusinessActivity() != null && data.getBusinessActivity().length > 0).collect(
                Collectors.toList());
        if (CollectionUtils.isEmpty(voList)) {
            return slaList;
        }
        voList.stream().forEach(data -> {
            SceneSlaRef ref = new SceneSlaRef();
            ref.setSlaName(data.getRuleName());
            if (data.getBusinessActivity() != null && data.getBusinessActivity().length > 0) {
                StringBuffer sb = new StringBuffer();
                for (String s : data.getBusinessActivity()) {
                    sb.append(s);
                    sb.append(",");
                }
                sb.deleteCharAt(sb.lastIndexOf(","));
                ref.setBusinessActivityIds(sb.toString());
            }
            ref.setTargetType(data.getRule().getIndexInfo());
            ref.setCondition(buildSlaCondition(data.getRule(), event));
            ref.setStatus(data.getStatus());
            slaList.add(ref);
        });
        return slaList;
    }

    private String buildSlaCondition(RuleBean vo, String event) {
        Map<String, Object> map = Maps.newHashMap();
        map.put(SceneManageConstant.COMPARE_TYPE, vo.getCondition());
        map.put(SceneManageConstant.COMPARE_VALUE, vo.getDuring());
        map.put(SceneManageConstant.ACHIEVE_TIMES, vo.getTimes());
        map.put(SceneManageConstant.EVENT, event);
        return JSON.toJSONString(map);
    }

    private void fillSceneId(List<? extends SceneRef> refList, Long sceneId) {
        if (CollectionUtils.isEmpty(refList)) {
            return;
        }
        refList.stream().forEach(data -> data.setSceneId(sceneId));
    }

    private void moveTempFile(List<SceneScriptRef> scriptList, Long sceneId) {
        String dirPath = scriptPath + SceneManageConstant.FILE_SPLIT + sceneId;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        scriptList.stream().filter(data -> data.getUploadId() != null).forEach(data -> {
            String tempPath = scriptTempPath
                    + SceneManageConstant.FILE_SPLIT
                    + data.getUploadId()
                    + SceneManageConstant.FILE_SPLIT
                    + data.getFileName();
            File file = new File(tempPath);
            data.setFileSize(LinuxUtil.getPrintSize(file.length()));
            data.setUploadPath(sceneId + SceneManageConstant.FILE_SPLIT + data.getFileName());
            LinuxUtil.executeLinuxCmd("mv " + tempPath + " " + dirPath);
            LinuxUtil.executeLinuxCmd("rm -rf " + scriptTempPath
                    + SceneManageConstant.FILE_SPLIT
                    + data.getUploadId());
        });
    }

    private void deleteUploadFile(List<String> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        files.stream().forEach(file ->
                LinuxUtil.executeLinuxCmd("rm -rf " + scriptPath
                        + SceneManageConstant.FILE_SPLIT
                        + file)
        );
    }
}
