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

package io.shulie.tro.web.app.service.scriptmanage;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pamirs.tro.common.constant.SceneManageConstant;
import com.pamirs.tro.common.enums.amdb.common.enums.RpcType;
import com.pamirs.tro.common.util.parse.UrlUtil;
import com.pamirs.tro.entity.dao.linkmanage.TBusinessLinkManageTableMapper;
import com.pamirs.tro.entity.dao.linkmanage.TSceneLinkRelateMapper;
import com.pamirs.tro.entity.dao.linkmanage.TSceneMapper;
import com.pamirs.tro.entity.domain.dto.linkmanage.BusinessActiveIdAndNameDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.BusinessFlowIdAndNameDto;
import com.pamirs.tro.entity.domain.dto.scenemanage.ScriptCheckDTO;
import com.pamirs.tro.entity.domain.entity.linkmanage.BusinessLinkManageTable;
import com.pamirs.tro.entity.domain.entity.linkmanage.Scene;
import com.pamirs.tro.entity.domain.entity.linkmanage.SceneLinkRelate;
import com.pamirs.tro.entity.domain.entity.user.User;
import com.pamirs.tro.entity.domain.vo.scenemanage.ScriptUrlVO;
import io.shulie.tro.cloud.common.pojo.dto.scenemanage.UploadFileDTO;
import io.shulie.tro.cloud.open.req.engine.EnginePluginDetailsWrapperReq;
import io.shulie.tro.cloud.open.req.engine.EnginePluginFetchWrapperReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneParseReq;
import io.shulie.tro.cloud.open.req.scenemanage.UpdateSceneFileRequest;
import io.shulie.tro.cloud.open.resp.engine.EnginePluginDetailResp;
import io.shulie.tro.cloud.open.resp.engine.EnginePluginSimpleInfoResp;
import io.shulie.tro.cloud.open.resp.scenemanage.SceneManageListResp;
import io.shulie.tro.common.beans.page.PagingList;
import io.shulie.tro.common.beans.response.ResponseResult;
import io.shulie.tro.utils.json.JsonHelper;
import io.shulie.tro.utils.linux.LinuxHelper;
import io.shulie.tro.utils.string.StringUtil;
import io.shulie.tro.utils.xml.XmlHelper;
import io.shulie.tro.web.app.common.RestContext;
import io.shulie.tro.web.app.convert.performace.TraceManageResponseConvertor;
import io.shulie.tro.web.app.exception.ExceptionCode;
import io.shulie.tro.web.app.exception.TroWebException;
import io.shulie.tro.web.app.request.filemanage.FileManageCreateRequest;
import io.shulie.tro.web.app.request.filemanage.FileManageUpdateRequest;
import io.shulie.tro.web.app.request.scriptmanage.ScriptManageDeployCreateRequest;
import io.shulie.tro.web.app.request.scriptmanage.ScriptManageDeployPageQueryRequest;
import io.shulie.tro.web.app.request.scriptmanage.ScriptManageDeployUpdateRequest;
import io.shulie.tro.web.app.request.scriptmanage.ScriptTagCreateRefRequest;
import io.shulie.tro.web.app.request.scriptmanage.SupportJmeterPluginNameRequest;
import io.shulie.tro.web.app.request.scriptmanage.SupportJmeterPluginVersionRequest;
import io.shulie.tro.web.app.response.filemanage.FileManageResponse;
import io.shulie.tro.web.app.response.scriptmanage.PluginConfigDetailResponse;
import io.shulie.tro.web.app.response.scriptmanage.ScriptManageActivityResponse;
import io.shulie.tro.web.app.response.scriptmanage.ScriptManageDeployActivityResponse;
import io.shulie.tro.web.app.response.scriptmanage.ScriptManageDeployDetailResponse;
import io.shulie.tro.web.app.response.scriptmanage.ScriptManageDeployResponse;
import io.shulie.tro.web.app.response.scriptmanage.ScriptManageSceneManageResponse;
import io.shulie.tro.web.app.response.scriptmanage.ScriptManageXmlContentResponse;
import io.shulie.tro.web.app.response.scriptmanage.SinglePluginRenderResponse;
import io.shulie.tro.web.app.response.scriptmanage.SupportJmeterPluginNameResponse;
import io.shulie.tro.web.app.response.scriptmanage.SupportJmeterPluginVersionResponse;
import io.shulie.tro.web.app.response.tagmanage.TagManageResponse;
import io.shulie.tro.web.app.service.linkManage.LinkManageService;
import io.shulie.tro.web.app.utils.exception.ScriptManageExceptionUtil;
import io.shulie.tro.web.auth.api.UserService;
import io.shulie.tro.web.common.constant.AppConstants;
import io.shulie.tro.web.common.constant.FeaturesConstants;
import io.shulie.tro.web.common.constant.FileManageConstant;
import io.shulie.tro.web.common.constant.RemoteConstant;
import io.shulie.tro.web.common.constant.ScriptManageConstant;
import io.shulie.tro.web.common.enums.script.FileTypeEnum;
import io.shulie.tro.web.common.enums.script.ScriptManageDeployStatusEnum;
import io.shulie.tro.web.common.pojo.vo.file.FileExtendVO;
import io.shulie.tro.web.common.util.ActivityUtil;
import io.shulie.tro.web.common.util.ActivityUtil.EntranceJoinEntity;
import io.shulie.tro.web.common.util.FileUtil;
import io.shulie.tro.web.data.dao.filemanage.FileManageDAO;
import io.shulie.tro.web.data.dao.linkmanage.BusinessLinkManageDAO;
import io.shulie.tro.web.data.dao.linkmanage.LinkManageDAO;
import io.shulie.tro.web.data.dao.scriptmanage.ScriptFileRefDAO;
import io.shulie.tro.web.data.dao.scriptmanage.ScriptManageDAO;
import io.shulie.tro.web.data.dao.scriptmanage.ScriptTagRefDAO;
import io.shulie.tro.web.data.dao.tagmanage.TagManageDAO;
import io.shulie.tro.web.data.param.filemanage.FileManageCreateParam;
import io.shulie.tro.web.data.param.linkmanage.LinkManageQueryParam;
import io.shulie.tro.web.data.param.scriptmanage.ScriptManageDeployCreateParam;
import io.shulie.tro.web.data.param.scriptmanage.ScriptManageDeployPageQueryParam;
import io.shulie.tro.web.data.param.tagmanage.TagManageParam;
import io.shulie.tro.web.data.result.filemanage.FileManageResult;
import io.shulie.tro.web.data.result.linkmange.BusinessLinkResult;
import io.shulie.tro.web.data.result.linkmange.LinkManageResult;
import io.shulie.tro.web.data.result.scriptmanage.ScriptFileRefResult;
import io.shulie.tro.web.data.result.scriptmanage.ScriptManageDeployResult;
import io.shulie.tro.web.data.result.scriptmanage.ScriptManageResult;
import io.shulie.tro.web.data.result.scriptmanage.ScriptTagRefResult;
import io.shulie.tro.web.data.result.tagmanage.TagManageResult;
import io.shulie.tro.web.diff.api.DiffFileApi;
import io.shulie.tro.web.diff.api.scenemanage.SceneManageApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zhaoyong
 */
@Component
@Slf4j
public class ScriptManageServiceImpl implements ScriptManageService {

    @Value("${file.upload.url:''}")
    private String fileUploadUrl;
    @Value("${file.upload.tmp.path:'/tmp/tro/'}")
    private String tmpFilePath;
    @Value("${file.upload.script.path:'/nfs/tro/script/'}")
    private String scriptFilePath;
    @Value("${script.check:true}")
    private Boolean scriptCheck;

    @Value("${file.upload.user.data.dir:/data/tmp}")
    private String fileDir;

    @Autowired
    private DiffFileApi fileApi;
    @Autowired
    private ScriptManageDAO scriptManageDAO;
    @Autowired
    private ScriptFileRefDAO scriptFileRefDAO;
    @Autowired
    private FileManageDAO fileManageDAO;
    @Autowired
    private ScriptTagRefDAO scriptTagRefDAO;
    @Autowired
    private TagManageDAO tagManageDAO;
    //FIXME ?????????????????????mapper??????dao?????????mapper?????????????????????T?????? ???T??????????????????
    @Autowired
    private TBusinessLinkManageTableMapper tBusinessLinkManageTableMapper;
    //FIXME ?????????????????????mapper??????dao?????????mapper?????????????????????T?????? ???T??????????????????
    @Autowired
    private TSceneMapper tSceneMapper;
    @Autowired
    private LinkManageService linkManageService;
    @Autowired
    private SceneManageApi sceneManageApi;
    //FIXME ?????????????????????mapper??????dao?????????mapper?????????????????????T?????? ???T??????????????????
    @Autowired
    private TSceneLinkRelateMapper tSceneLinkRelateMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private BusinessLinkManageDAO businessLinkManageDAO;
    @Autowired
    private LinkManageDAO linkManageDAO;

    @Override
    public String getZipFileUrl(Long scriptDeployId) {
        ScriptManageDeployResult scriptManageDeployResult = scriptManageDAO.selectScriptManageDeployById(
            scriptDeployId);
        if (scriptManageDeployResult == null) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_DOWNLOAD_VALID_ERROR, "??????????????????");
        }
        List<ScriptFileRefResult> scriptFileRefResults = scriptFileRefDAO.selectFileIdsByScriptDeployId(scriptDeployId);
        if (CollectionUtils.isEmpty(scriptFileRefResults)) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_DOWNLOAD_VALID_ERROR, "????????????????????????");
        }
        List<Long> fileIds = scriptFileRefResults.stream().map(ScriptFileRefResult::getFileId).collect(
            Collectors.toList());
        List<FileManageResult> fileManageResults = fileManageDAO.selectFileManageByIds(fileIds);
        List<String> uploadPaths = fileManageResults.stream().map(FileManageResult::getUploadPath).collect(
            Collectors.toList());
        String targetScriptPath = getTargetScriptPath(scriptManageDeployResult);
        String fileName = scriptManageDeployResult.getName() + ".zip";
        Boolean result = fileApi.zipFile(targetScriptPath, uploadPaths, fileName);
        if (!result) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_DOWNLOAD_VALID_ERROR, "???????????????????????????");
        }
        // todo ????????????
        String url = null;
        try {
            url = fileUploadUrl + FileManageConstant.CLOUD_FILE_DOWN_LOAD_API + targetScriptPath + URLEncoder.encode(fileName, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] cmds = {"curl","-o",fileDir +"/" + fileName,"-OL","-H", "licenseKey:"+RemoteConstant.LICENSE_VALUE, url};
        LinuxHelper.execCurl(cmds);
        return fileDir + "/" + fileName;
    }

    @Override
    public Long createScriptManage(ScriptManageDeployCreateRequest scriptManageDeployCreateRequest) {
        checkCreateScriptManageParam(scriptManageDeployCreateRequest);
        scriptManageDeployCreateRequest.setName(scriptManageDeployCreateRequest.getName().trim());
        List<ScriptManageResult> scriptManageResults = scriptManageDAO.selectScriptManageByName(
            scriptManageDeployCreateRequest.getName());
        if (CollectionUtils.isNotEmpty(scriptManageResults)) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_VALID_ERROR, "?????????????????????");
        }
        uploadCreateScriptFile(scriptManageDeployCreateRequest.getFileManageCreateRequests());
        List<FileManageCreateRequest> scriptFile = scriptManageDeployCreateRequest.getFileManageCreateRequests()
            .stream().filter(o -> o.getFileType() == 0).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(scriptFile) || scriptFile.size() != 1) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_VALID_ERROR, "????????????????????????");
        }
        ScriptCheckDTO scriptCheckDTO = checkAndUpdateScript(scriptManageDeployCreateRequest.getRefType(),
            scriptManageDeployCreateRequest.getRefValue(),
            tmpFilePath + File.separatorChar+ scriptFile.get(0).getUploadId() + "/" + scriptFile.get(0).getFileName());
        if (scriptCheckDTO != null && !StringUtil.isBlank(scriptCheckDTO.getErrmsg())) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_VALID_ERROR, scriptCheckDTO.getErrmsg());
        }
        if (scriptCheckDTO != null && scriptCheckDTO.getIsHttp() != null && !scriptCheckDTO.getIsHttp()) {
            if (CollectionUtils.isEmpty(scriptManageDeployCreateRequest.getPluginConfigCreateRequests())) {
                throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_VALID_ERROR, "????????????http???????????????????????????????????????");
            }
        }

        ScriptManageDeployCreateParam scriptManageDeployCreateParam = new ScriptManageDeployCreateParam();
        BeanUtils.copyProperties(scriptManageDeployCreateRequest, scriptManageDeployCreateParam);
        scriptManageDeployCreateParam.setStatus(0);
        scriptManageDeployCreateParam.setScriptVersion(1);

        if (CollectionUtils.isNotEmpty(scriptManageDeployCreateRequest.getPluginConfigCreateRequests())) {
            Map<String, Object> features = Maps.newHashMap();
            features.put(FeaturesConstants.PLUGIN_CONFIG,
                scriptManageDeployCreateRequest.getPluginConfigCreateRequests());
            scriptManageDeployCreateParam.setFeature(JSON.toJSONString(features));
        }
        ScriptManageDeployResult scriptManageDeployResult = scriptManageDAO.createScriptManageDeploy(
            scriptManageDeployCreateParam);
        String targetScriptPath = getTargetScriptPath(scriptManageDeployResult);
        List<String> tmpFilePaths = scriptManageDeployCreateRequest.getFileManageCreateRequests().stream().map(
            o -> tmpFilePath + o.getUploadId() + "/" + o.getFileName()).collect(Collectors.toList());
        fileApi.copyFile(targetScriptPath, tmpFilePaths);
        fileApi.deleteFile(tmpFilePaths);
        List<FileManageCreateParam> fileManageCreateParams = getFileManageCreateParams(
            scriptManageDeployCreateRequest.getFileManageCreateRequests(), targetScriptPath);
        List<Long> fileIds = fileManageDAO.createFileManageList(fileManageCreateParams);
        scriptFileRefDAO.createScriptFileRefs(fileIds, scriptManageDeployResult.getId());
        return scriptManageDeployResult.getId();
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param fileManageCreateRequests
     */
    private void uploadCreateScriptFile(List<FileManageCreateRequest> fileManageCreateRequests) {
        if (CollectionUtils.isNotEmpty(fileManageCreateRequests)) {
            for (FileManageCreateRequest fileManageCreateRequest : fileManageCreateRequests) {
                if (fileManageCreateRequest.getIsDeleted() == 0 && fileManageCreateRequest.getFileType() == 0
                    && !StringUtil.isBlank(fileManageCreateRequest.getScriptContent())) {
                    if (!StringUtil.isBlank(fileManageCreateRequest.getUploadId())) {
                        String tempFile = tmpFilePath + fileManageCreateRequest.getUploadId() + "/"
                            + fileManageCreateRequest.getFileName();
                        fileApi.deleteFile(Collections.singletonList(tempFile));
                    }
                    UUID uuid = UUID.randomUUID();
                    fileManageCreateRequest.setUploadId(uuid.toString());
                    String tempFile = tmpFilePath + fileManageCreateRequest.getUploadId() + "/"
                        + fileManageCreateRequest.getFileName();
                    fileApi.createFileByPathAndString(tempFile, fileManageCreateRequest.getScriptContent());
                }
            }

        }
    }

    /**
     * ???????????????????????????
     *
     * @param fileManageCreateRequests ??????????????????
     */
    private void uploadUpdateScriptFile(List<FileManageUpdateRequest> fileManageCreateRequests) {
        if (CollectionUtils.isEmpty(fileManageCreateRequests)) {
            return;
        }

        // ????????????????????????, ??????
        for (FileManageUpdateRequest fileManageUpdateRequest : fileManageCreateRequests) {
            if (StringUtil.isNotBlank(fileManageUpdateRequest.getScriptContent())) {
                // uploadId ????????????, ??????
                if (StringUtil.isNotBlank(fileManageUpdateRequest.getUploadId())) {
                    String tempFile = tmpFilePath + fileManageUpdateRequest.getUploadId() + "/"
                        + fileManageUpdateRequest.getFileName();
                    fileApi.deleteFile(Collections.singletonList(tempFile));
                }

                // ??????
                UUID uuid = UUID.randomUUID();
                fileManageUpdateRequest.setUploadId(uuid.toString());
                fileManageUpdateRequest.setId(null);
                String tempFile = tmpFilePath + fileManageUpdateRequest.getUploadId() + "/"
                    + fileManageUpdateRequest.getFileName();
                fileApi.createFileByPathAndString(tempFile, fileManageUpdateRequest.getScriptContent());
            }
        }
    }

    @Override
    public ScriptCheckDTO checkAndUpdateScript(String refType, String refValue, String scriptFileUploadPath) {
        ScriptCheckDTO dto = new ScriptCheckDTO();
        if (scriptCheck == null || !scriptCheck) {
            return dto;
        }

        SceneParseReq sceneParseReq = new SceneParseReq();
        sceneParseReq.setUploadPath(scriptFileUploadPath);
        sceneParseReq.setAbsolutePath(true);
        sceneParseReq.setScriptId(1L);
        ResponseResult<Map<String, Object>> mapResponseResult = sceneManageApi.parseAndUpdateScript(sceneParseReq);
        if (mapResponseResult == null || mapResponseResult.getData() == null) {
            log.error("?????????????????????????????????{}", scriptFileUploadPath);
            dto.setErrmsg("?????????????????????????????????" + scriptFileUploadPath);
            return dto;
        }
        Map<String, Object> dataMap = mapResponseResult.getData();
        List<Map<String, Object>> voList = (List<Map<String, Object>>)dataMap.get("requestUrl");
        if (CollectionUtils.isEmpty(voList)) {
            dto.setErrmsg("???????????????????????????????????????");
            return dto;
        }
        List<BusinessLinkManageTable> businessActivityList = getBusinessActivity(refType, refValue);
        if (CollectionUtils.isEmpty(businessActivityList)) {
            dto.setErrmsg("?????????????????????????????????");
            return dto;
        }
        Set<String> errorSet = new HashSet<>();
        int unbindCount = 0;
        Map<String, Integer> urlMap = new HashMap<>();
        for (BusinessLinkManageTable data : businessActivityList) {
            if (StringUtil.isBlank(data.getEntrace())) {
                continue;
            }
            Set<String> tempErrorSet = new HashSet<>();
            EntranceJoinEntity entranceJoinEntity = ActivityUtil.covertEntrance(data.getEntrace());
            if (!entranceJoinEntity.getRpcType().equals(RpcType.TYPE_WEB_SERVER + "")) {
                dto.setIsHttp(false);
            }
            Map<String, String> map = UrlUtil.convertUrl(entranceJoinEntity);
            for (Map<String, Object> temp : voList) {
                ScriptUrlVO urlVO = JsonHelper.json2Bean(JsonHelper.bean2Json(temp), ScriptUrlVO.class);
                if (UrlUtil.checkEqual(map.get("url"), urlVO.getPath()) && urlVO.getEnable()) {
                    unbindCount = unbindCount + 1;
                    tempErrorSet.clear();
                    if (!urlMap.containsKey(urlVO.getName())) {
                        urlMap.put(urlVO.getName(), 1);
                    } else {
                        urlMap.put(urlVO.getName(), urlMap.get(urlVO.getName()) + 1);
                    }
                    break;
                } else {
                    tempErrorSet.add(data.getLinkName());
                }
            }
            errorSet.addAll(tempErrorSet);
        }
        Set<String> urlErrorSet = new HashSet<>();
        urlMap.forEach((k, v) -> {
            if (v > 1) {
                urlErrorSet.add("?????????[" + k + "]??????" + v + "???");
            }
        });
        if (urlErrorSet.size() > 0) {
            dto.setMatchActivity(false);
            dto.setErrmsg("???????????????????????????:" + urlErrorSet.toString());
        }
        //?????????????????????????????????????????????????????????
        if (businessActivityList.size() > unbindCount) {
            dto.setMatchActivity(false);
            dto.setErrmsg("????????????????????????????????????:" + errorSet.toString());
        }

        return dto;
    }

    private List<BusinessLinkManageTable> getBusinessActivity(String refType, String refValue) {
        if (ScriptManageConstant.BUSINESS_ACTIVITY_REF_TYPE.equals(refType)) {
            return tBusinessLinkManageTableMapper.selectBussinessLinkByIdList(
                Collections.singletonList(Long.valueOf(refValue)));
        }
        if (ScriptManageConstant.BUSINESS_PROCESS_REF_TYPE.equals(refType)) {
            List<SceneLinkRelate> sceneLinkRelates = tSceneLinkRelateMapper.selectBySceneId(Long.valueOf(refValue));
            if (CollectionUtils.isNotEmpty(sceneLinkRelates)) {
                List<Long> businessActivityIds = sceneLinkRelates.stream().map(o -> Long.valueOf(o.getBusinessLinkId()))
                    .collect(Collectors.toList());
                return tBusinessLinkManageTableMapper.selectBussinessLinkByIdList(businessActivityIds);
            }
        }
        return null;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void updateScriptManage(ScriptManageDeployUpdateRequest scriptManageDeployUpdateRequest) {
        // ????????????
        this.checkUpdateScriptManageParam(scriptManageDeployUpdateRequest);
        // ??????????????????
        scriptManageDeployUpdateRequest.setName(scriptManageDeployUpdateRequest.getName().trim());

        // ????????????
        List<FileManageUpdateRequest> scriptFile =
            scriptManageDeployUpdateRequest.getFileManageUpdateRequests().stream()
                .filter(o -> o.getIsDeleted() == 0
                    && FileTypeEnum.SCRIPT.getCode().equals(o.getFileType()))
                .collect(Collectors.toList());
        // ??????
        ScriptManageExceptionUtil.isUpdateValidError(CollectionUtils.isEmpty(scriptFile)
                || scriptFile.size() != 1, "?????????????????????!");

        // ???????????????????????????
        this.uploadUpdateScriptFile(scriptFile);

        // ??????
        ScriptManageExceptionUtil.isUpdateValidError(CollectionUtils.isEmpty(scriptFile) || scriptFile.size() != 1, "?????????????????????!");

        // ???????????? url
        String scriptFileUrl;
        if (scriptFile.get(0).getId() == null) {
            scriptFileUrl = tmpFilePath + scriptFile.get(0).getUploadId() + "/" + scriptFile.get(0).getFileName();
        } else {
            List<FileManageResult> fileManageResults = fileManageDAO.selectFileManageByIds(
                Collections.singletonList(scriptFile.get(0).getId()));
            if (CollectionUtils.isEmpty(fileManageResults)) {
                log.error("??????????????????????????????fileId:{}", scriptFile.get(0).getId());
                throw ScriptManageExceptionUtil.getUpdateValidError("????????????????????????????????????");
            }
            scriptFileUrl = fileManageResults.get(0).getUploadPath();
        }

        // cloud ??????, ??????, ????????????
        ScriptCheckDTO scriptCheckDTO = this.checkAndUpdateScript(scriptManageDeployUpdateRequest.getRefType(),
            scriptManageDeployUpdateRequest.getRefValue(), scriptFileUrl);

        // ??????????????????
        if (scriptCheckDTO != null && !StringUtil.isBlank(scriptCheckDTO.getErrmsg())) {
            throw ScriptManageExceptionUtil.getUpdateValidError(scriptCheckDTO.getErrmsg());
        }

        if (scriptCheckDTO != null && scriptCheckDTO.getIsHttp() != null && !scriptCheckDTO.getIsHttp()) {
            ScriptManageExceptionUtil.isCreateValidError(CollectionUtils.isEmpty(scriptManageDeployUpdateRequest.getPluginConfigUpdateRequests()), "????????????http????????????????????????????????????!");
        }

        // ????????????, ????????????????????????
        ScriptManageDeployResult scriptManageDeployResult = this.updateScriptAndCreateScriptDeployAndGet(scriptManageDeployUpdateRequest);
        String targetScriptPath = this.getTargetScriptPath(scriptManageDeployUpdateRequest, scriptManageDeployResult);

        // ??????, ????????????, ??????, ??????????????????
        List<FileManageUpdateRequest> addFileManageUpdateRequests = scriptManageDeployUpdateRequest
            .getFileManageUpdateRequests().stream()
            .filter(o -> o.getIsDeleted() == 0).collect(Collectors.toList());

        // ??????????????????????????????
        List<FileManageCreateParam> fileManageCreateParams = getFileManageCreateParamsByUpdateReq(
            addFileManageUpdateRequests, targetScriptPath);

        // ??????????????????, ????????????ids
        List<Long> fileIds = fileManageDAO.createFileManageList(fileManageCreateParams);

        // ?????? cloud ???, ?????????id
        // ???????????? id
        Long scriptId = scriptManageDeployUpdateRequest.getId();
        // ??????: ???????????????, ??????, ??????????????????????????????
        this.updateCloudFileByScriptId(scriptId, scriptManageDeployResult.getType(), fileManageCreateParams);

        // ??????ids???????????????id?????????
        scriptFileRefDAO.createScriptFileRefs(fileIds, scriptManageDeployResult.getId());
    }

    /**
     * ?????? scriptId ?????????????????????????????????????????????
     * @param scriptId                 ????????????id
     * @param scriptType ????????????
     * @param files ????????????
     */
    private void updateCloudFileByScriptId(Long scriptId, Integer scriptType, List<FileManageCreateParam> files) {
        if (scriptId == null || CollectionUtils.isEmpty(files)) {
            return;
        }

        // ????????????
        UpdateSceneFileRequest request = new UpdateSceneFileRequest();
        request.setScriptId(scriptId);
        request.setScriptType(scriptType);

        List<UploadFileDTO> uploadFiles = files.stream().map(file -> {
            UploadFileDTO uploadFileDTO = new UploadFileDTO();
            BeanUtils.copyProperties(file, uploadFileDTO);
            uploadFileDTO.setIsDeleted(0);
            String fileExtend = file.getFileExtend();
            if (StringUtils.isNotBlank(fileExtend)) {
                FileExtendVO fileExtendVO = JSONUtil.toBean(fileExtend, FileExtendVO.class);
                uploadFileDTO.setIsSplit(fileExtendVO.getIsSplit());
            }
            uploadFileDTO.setUploadedData(0L);
            uploadFileDTO.setUploadTime(DateUtil.format(file.getUploadTime(), AppConstants.DATE_FORMAT_STRING));
            return uploadFileDTO;
        }).collect(Collectors.toList());
        request.setUploadFiles(uploadFiles);

        // cloud ??????
        ResponseResult response = sceneManageApi.updateSceneFileByScriptId(request);
        if (!response.getSuccess()) {
            log.error("???????????? --> ????????? cloud ??????, ????????????????????????, ????????????: {}", JSONUtil.toJsonStr(response));
            throw ScriptManageExceptionUtil.getUpdateValidError(String.format("????????? cloud ??????????????????, ????????????: %s", response.getError().getSolution()));
        }
    }

    /**
     * ????????????????????????
     *
     * @param scriptManageDeployUpdateRequest ????????????????????????
     * @param scriptManageDeployResult ????????????
     * @return ??????????????????
     */
    private String getTargetScriptPath(ScriptManageDeployUpdateRequest scriptManageDeployUpdateRequest,
        ScriptManageDeployResult scriptManageDeployResult) {
        List<String> sourcePaths = new ArrayList<>();
        // ?????????????????? ?????? + ??????id + ??????
        String targetScriptPath = this.getTargetScriptPath(scriptManageDeployResult);

        List<String> tmpFilePaths = scriptManageDeployUpdateRequest.getFileManageUpdateRequests().stream().filter(
            o -> o.getIsDeleted() == 0
                && !StringUtil.isBlank(o.getUploadId())).map(
            o -> tmpFilePath + o.getUploadId() + "/" + o.getFileName()).collect(Collectors.toList());
        List<Long> existFileIds = scriptManageDeployUpdateRequest.getFileManageUpdateRequests().stream().filter(
            o -> o.getIsDeleted() == 0
                && StringUtil.isBlank(o.getUploadId()) && o.getId() != null).map(FileManageUpdateRequest::getId)
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(existFileIds)) {
            List<FileManageResult> fileManageResults = fileManageDAO.selectFileManageByIds(existFileIds);
            List<String> uploadPaths = fileManageResults.stream().map(FileManageResult::getUploadPath).collect(
                Collectors.toList());
            sourcePaths.addAll(uploadPaths);
        }

        if (CollectionUtils.isNotEmpty(tmpFilePaths)) {
            sourcePaths.addAll(tmpFilePaths);
        }
        fileApi.copyFile(targetScriptPath, sourcePaths);
        if (CollectionUtils.isNotEmpty(tmpFilePaths)) {
            fileApi.deleteFile(tmpFilePaths);
        }
        return targetScriptPath;
    }

    /**
     * ????????????, ????????????????????????, ????????????????????????
     *
     * @param scriptManageDeployUpdateRequest ??????, ??????, ????????????
     * @return ????????????????????????
     */
    private ScriptManageDeployResult updateScriptAndCreateScriptDeployAndGet(
        ScriptManageDeployUpdateRequest scriptManageDeployUpdateRequest) {
        // ????????????id
        Long scriptDeployId = scriptManageDeployUpdateRequest.getId();
        // ??????????????????id??????????????????
        ScriptManageDeployResult oldScriptManageDeployResult = scriptManageDAO.selectScriptManageDeployById(
            scriptDeployId);
        ScriptManageExceptionUtil.isUpdateValidError(oldScriptManageDeployResult == null, "??????????????????????????????!");

        // ??????id, ??????????????????id
        Long scriptId = oldScriptManageDeployResult.getScriptId();
        // ????????????
        ScriptManageResult scriptManageResult = scriptManageDAO.selectScriptManageById(scriptId);
        ScriptManageExceptionUtil.isUpdateValidError(scriptManageResult == null, "?????????????????????????????????????????????!");

        // ????????????????????????
        ScriptManageDeployCreateParam scriptManageDeployCreateParam = new ScriptManageDeployCreateParam();
        BeanUtils.copyProperties(scriptManageDeployUpdateRequest, scriptManageDeployCreateParam);
        scriptManageDeployCreateParam.setScriptId(scriptId);
        scriptManageDeployCreateParam.setStatus(ScriptManageDeployStatusEnum.NEW.getCode());
        // ?????? + 1
        int scriptNewVersion = scriptManageResult.getScriptVersion() + 1;
        scriptManageDeployCreateParam.setScriptVersion(scriptNewVersion);

        //?????????????????????????????????????????????????????????
        scriptManageDAO.updateScriptVersion(scriptId, scriptNewVersion);

        // ????????????????????????
        Map<String, Object> features = Maps.newHashMap();
        features.put(FeaturesConstants.PLUGIN_CONFIG, scriptManageDeployUpdateRequest.getPluginConfigUpdateRequests());
        scriptManageDeployCreateParam.setFeature(JSON.toJSONString(features));
        return scriptManageDAO.createScriptManageDeploy(scriptManageDeployCreateParam);
    }

    @Override
    public List<ScriptManageSceneManageResponse> getAllScenes(String businessFlowName) {
        List<ScriptManageSceneManageResponse> scriptManageSceneManageResponses = new ArrayList<>();
        try {
            List<BusinessFlowIdAndNameDto> businessFlowIdAndNameDtos = linkManageService.businessFlowIdFuzzSearch(
                businessFlowName);
            if (CollectionUtils.isNotEmpty(businessFlowIdAndNameDtos)) {
                scriptManageSceneManageResponses = businessFlowIdAndNameDtos.stream().map(businessFlowIdAndNameDto -> {
                    ScriptManageSceneManageResponse scriptManageSceneManageResponse
                        = new ScriptManageSceneManageResponse();
                    scriptManageSceneManageResponse.setId(businessFlowIdAndNameDto.getId());
                    scriptManageSceneManageResponse.setSceneName(businessFlowIdAndNameDto.getBusinessFlowName());
                    return scriptManageSceneManageResponse;

                }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("????????????????????????");
        }
        if (CollectionUtils.isNotEmpty(scriptManageSceneManageResponses)) {
            List<String> systemProcessIds = scriptManageSceneManageResponses.stream().map(
                ScriptManageSceneManageResponse::getId).collect(Collectors.toList());
            List<Integer> status = new ArrayList<>();
            status.add(0);
            status.add(1);
            List<ScriptManageDeployResult> scriptManageDeployResults = scriptManageDAO.selectByRefIdsAndType(
                systemProcessIds, ScriptManageConstant.BUSINESS_PROCESS_REF_TYPE, status);
            if (CollectionUtils.isNotEmpty(scriptManageDeployResults)) {
                Map<String, List<ScriptManageDeployResult>> systemProcessScriptMap = scriptManageDeployResults.stream()
                    .collect(Collectors.groupingBy(ScriptManageDeployResult::getRefValue));
                scriptManageSceneManageResponses.forEach(scriptManageSceneManageResponse -> {
                    List<ScriptManageDeployResult> scriptManageDeploys = systemProcessScriptMap.get(
                        scriptManageSceneManageResponse.getId());
                    scriptManageSceneManageResponse.setScriptManageDeployResponses(
                        getScriptManageDeployResponses(scriptManageDeploys));
                });
            }
        }
        return scriptManageSceneManageResponses;
    }

    private List<ScriptManageDeployActivityResponse> getScriptManageDeployResponses(
        List<ScriptManageDeployResult> scriptManageDeployResults) {
        if (CollectionUtils.isNotEmpty(scriptManageDeployResults)) {
            List<ScriptManageDeployActivityResponse> scriptManageDeployActivityResponses = new ArrayList<>();
            Map<Long, List<ScriptManageDeployResult>> collect = scriptManageDeployResults.stream().collect(
                Collectors.groupingBy(ScriptManageDeployResult::getScriptId));
            collect.forEach((k, v) -> {
                ScriptManageDeployResult scriptManageDeploy = v.stream().max(
                    Comparator.comparing(ScriptManageDeployResult::getScriptVersion)).get();
                ScriptManageDeployActivityResponse scriptManageDeployActivityResponse
                    = new ScriptManageDeployActivityResponse();
                scriptManageDeployActivityResponse.setId(scriptManageDeploy.getId());
                scriptManageDeployActivityResponse.setName(
                    scriptManageDeploy.getName() + " ??????" + scriptManageDeploy.getScriptVersion());
                scriptManageDeployActivityResponses.add(scriptManageDeployActivityResponse);
            });
            return scriptManageDeployActivityResponses;
        }
        return null;
    }

    @Override
    public List<ScriptManageActivityResponse> listAllActivities(String activityName) {
        // ???????????????id?????????
        // ???????????????????????????????????????, ?????????????????????????????????????????????
        List<BusinessActiveIdAndNameDto> businessActiveIdAndNameList = linkManageService.businessActiveNameFuzzSearch(
            activityName);
        if (CollectionUtils.isEmpty(businessActiveIdAndNameList)) {
            return Collections.emptyList();
        }

        // ??????
        List<ScriptManageActivityResponse> scriptManageActivityResponses = businessActiveIdAndNameList.stream()
            .map(businessActiveIdAndNameDto -> {
                ScriptManageActivityResponse scriptManageActivityResponse = new ScriptManageActivityResponse();
                scriptManageActivityResponse.setId(businessActiveIdAndNameDto.getId());
                scriptManageActivityResponse.setBusinessActiveName(businessActiveIdAndNameDto.getBusinessActiveName());
                return scriptManageActivityResponse;
            }).collect(Collectors.toList());

        // ????????????????????????id
        List<String> activityIds = scriptManageActivityResponses.stream()
            .map(ScriptManageActivityResponse::getId)
            .collect(Collectors.toList());
        if (activityIds.isEmpty()) {
            return scriptManageActivityResponses;
        }

        // ?????????????????????, ??????, ??? ???????????????
        List<Integer> scriptManageDeployStatusList = Arrays.asList(ScriptManageDeployStatusEnum.NEW.getCode(),
            ScriptManageDeployStatusEnum.PASS.getCode());

        // ????????????????????????????????????????????????
        List<ScriptManageDeployResult> scriptManageDeployResults = scriptManageDAO.selectByRefIdsAndType(
            activityIds, ScriptManageConstant.BUSINESS_ACTIVITY_REF_TYPE, scriptManageDeployStatusList);
        if (CollectionUtils.isEmpty(scriptManageDeployResults)) {
            return scriptManageActivityResponses;
        }

        // ????????????, ??????????????????id, ??????
        Map<String, List<ScriptManageDeployResult>> activityScriptMap = scriptManageDeployResults.stream()
            .filter(scriptManageDeploy -> StringUtils.isNotBlank(scriptManageDeploy.getRefValue()))
            .collect(Collectors.groupingBy(ScriptManageDeployResult::getRefValue));
        // ??????????????????????????????
        scriptManageActivityResponses.forEach(scriptManageActivityResponse -> {
            List<ScriptManageDeployResult> scriptManageDeploys = activityScriptMap.get(scriptManageActivityResponse.getId());
            scriptManageActivityResponse.setScriptManageDeployResponses(getScriptManageDeployResponses(scriptManageDeploys));
        });

        return scriptManageActivityResponses;
    }

    @Override
    public String explainScriptFile(String scriptFileUploadPath) {
        SceneParseReq sceneParseReq = new SceneParseReq();
        sceneParseReq.setUploadPath(scriptFileUploadPath);
        sceneParseReq.setScriptId(1L);
        sceneParseReq.setAbsolutePath(true);
        ResponseResult<Map<String, Object>> mapResponseResult = sceneManageApi.parseScript(sceneParseReq);
        if (mapResponseResult != null && mapResponseResult.getData() != null) {
            Map<String, Object> data = mapResponseResult.getData();
            if (data.get("xmlContent") != null) {
                return XmlHelper.formatXml(data.get("xmlContent").toString());
            }
        }
        return "";
    }

    @Override
    public String getFileDownLoadUrl(String filePath) {
        if(StringUtils.isBlank(filePath)) {
            return "";
        }
        String[] file = filePath.split("/");
        String fileName = file[file.length -1];
        String url = null;
        try {
            url = fileUploadUrl + FileManageConstant.CLOUD_FILE_DOWN_LOAD_API + URLEncoder.encode(filePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // todo ????????????
        String[] cmds = {"curl","-o",fileDir +"/" + fileName,"-OL","-H", "licenseKey:"+RemoteConstant.LICENSE_VALUE,url};
        LinuxHelper.execCurl(cmds);
        return  fileDir + "/" +fileName;
    }

    @Override
    public List<ScriptManageDeployResponse> listScriptDeployByScriptId(Long scriptId) {
        return TraceManageResponseConvertor.INSTANCE.ofListScriptManageDeployResponse(scriptManageDAO.selectScriptManageDeployByScriptId(scriptId));
    }

    @Override
    public String rollbackScriptDeploy(Long scriptDeployId) {
        ScriptManageDeployResult scriptManageDeployResult = scriptManageDAO.selectScriptManageDeployById(scriptDeployId);
        ScriptManageResult scriptManageResult = scriptManageDAO.selectScriptManageById(scriptManageDeployResult.getScriptId());
        if (scriptManageDeployResult.getScriptVersion().equals(scriptManageResult.getScriptVersion())){
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_ROLLBACK_VALID_ERROR, "???????????????????????????????????????");
        }
        List<ScriptFileRefResult> scriptFileRefResults = scriptFileRefDAO.selectFileIdsByScriptDeployId(scriptManageDeployResult.getId());
        if (CollectionUtils.isEmpty(scriptFileRefResults)){
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_ROLLBACK_VALID_ERROR, "????????????????????????????????????");
        }
        List<Long> scriptFileIds = scriptFileRefResults.stream().map(ScriptFileRefResult::getFileId).collect(Collectors.toList());
        List<FileManageResult> fileManageResults = fileManageDAO.selectFileManageByIds(scriptFileIds);
        ScriptManageDeployUpdateRequest scriptManageDeployUpdateRequest = getScriptManageDeployUpdateRequest(scriptManageDeployResult,fileManageResults);
        updateScriptManage(scriptManageDeployUpdateRequest);
        return scriptManageResult.getName();
    }

    @Override
    public List<ScriptManageXmlContentResponse> getScriptManageDeployXmlContent(List<Long> scriptManageDeployIds) {
        List<ScriptManageXmlContentResponse> results = new ArrayList<>();
        List<ScriptFileRefResult> scriptFileRefResults = scriptFileRefDAO.selectFileIdsByScriptDeployIds(scriptManageDeployIds);
        if (CollectionUtils.isNotEmpty(scriptFileRefResults)){
            Map<Long, List<ScriptFileRefResult>> collect = scriptFileRefResults.stream().collect(Collectors.groupingBy(ScriptFileRefResult::getScriptDeployId));
            collect.forEach((k,v) -> {
                ScriptManageXmlContentResponse scriptManageXmlContentResponse = new ScriptManageXmlContentResponse();
                scriptManageXmlContentResponse.setScriptManageDeployId(k);
                List<Long> fileIds = v.stream().map(ScriptFileRefResult::getFileId).collect(Collectors.toList());
                List<FileManageResult> fileManageResults = fileManageDAO.selectFileManageByIds(fileIds);
                List<FileManageResult> scriptFiles = fileManageResults.stream().filter(o -> o.getFileType() == 0).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(scriptFiles)){
                    scriptManageXmlContentResponse.setContent(explainScriptFile(scriptFiles.get(0).getUploadPath()));
                }
                results.add(scriptManageXmlContentResponse);
            });

        }
        return results;
    }

    private ScriptManageDeployUpdateRequest getScriptManageDeployUpdateRequest(ScriptManageDeployResult scriptManageDeployResult,List<FileManageResult> fileManageResults) {
        ScriptManageDeployUpdateRequest scriptManageDeployUpdateRequest = new ScriptManageDeployUpdateRequest();
        scriptManageDeployUpdateRequest.setId(scriptManageDeployResult.getId());
        scriptManageDeployUpdateRequest.setName(scriptManageDeployResult.getName());
        scriptManageDeployUpdateRequest.setRefType(scriptManageDeployResult.getRefType());
        scriptManageDeployUpdateRequest.setRefValue(scriptManageDeployResult.getRefValue());
        scriptManageDeployUpdateRequest.setType(scriptManageDeployResult.getType());
        List<FileManageUpdateRequest> fileManageUpdateRequests = new ArrayList<>();
        fileManageResults.forEach(fileManageResult -> {
            FileManageUpdateRequest fileManageUpdateRequest = new FileManageUpdateRequest();
            fileManageUpdateRequest.setId(fileManageResult.getId());
            fileManageUpdateRequest.setFileName(fileManageResult.getFileName());
            fileManageUpdateRequest.setFileSize(fileManageResult.getFileSize());
            fileManageUpdateRequest.setFileType(fileManageResult.getFileType());
            if(fileManageResult.getFileExtend() != null) {
                Map<String, Object> stringObjectMap = JsonHelper.json2Map(fileManageResult.getFileExtend(),
                        String.class, Object.class);
                if (stringObjectMap != null && stringObjectMap.get("dataCount") != null && !StringUtils.isBlank(stringObjectMap.get("dataCount").toString())) {
                    fileManageUpdateRequest.setDataCount(Long.valueOf(stringObjectMap.get("dataCount").toString()));
                }
                if (stringObjectMap != null && stringObjectMap.get("isSplit") != null && !StringUtils.isBlank(stringObjectMap.get("isSplit").toString())) {
                    fileManageUpdateRequest.setIsSplit(Integer.valueOf(stringObjectMap.get("isSplit").toString()));
                }
            }
            fileManageUpdateRequest.setIsDeleted(fileManageResult.getIsDeleted());
            fileManageUpdateRequest.setUploadTime(fileManageResult.getUploadTime());
            fileManageUpdateRequests.add(fileManageUpdateRequest);
        });
        scriptManageDeployUpdateRequest.setFileManageUpdateRequests(fileManageUpdateRequests);
        return scriptManageDeployUpdateRequest;
    }


    @Override
    public List<SupportJmeterPluginNameResponse> getSupportJmeterPluginNameList(
        SupportJmeterPluginNameRequest nameRequest) {
        List<SupportJmeterPluginNameResponse> nameResponseList = Lists.newArrayList();
        String refType = nameRequest.getRelatedType();
        String refValue = nameRequest.getRelatedId();
        //????????????????????????
        List<BusinessLinkManageTable> businessActivityList = getBusinessActivity(refType, refValue);
        if (CollectionUtils.isEmpty(businessActivityList)) {
            log.error("??????????????????????????????:[refType:{},refValue:{}]", refType, refValue);
            return nameResponseList;
        }
        //????????????????????????id
        List<Long> businessLinkIdList = businessActivityList.stream()
            .map(BusinessLinkManageTable::getLinkId)
            .collect(Collectors.toList());
        List<BusinessLinkResult> businessLinkResultList = businessLinkManageDAO.getListByIds(businessLinkIdList);
        List<Long> techLinkIdList = businessLinkResultList.stream()
            .map(BusinessLinkResult::getRelatedTechLink)
            .map(Long::parseLong)
            .collect(Collectors.toList());
        //????????????????????????????????????
        LinkManageQueryParam queryParam = new LinkManageQueryParam();
        queryParam.setLinkIdList(techLinkIdList);
        List<LinkManageResult> linkManageResultList = linkManageDAO.selectList(queryParam);
        if (CollectionUtils.isEmpty(linkManageResultList)) {
            log.error("????????????????????????:[techLinkIdList:{}]", JSON.toJSONString(techLinkIdList));
            return nameResponseList;
        }
        List<String> typeList = linkManageResultList.stream()
            .map(LinkManageResult::getFeatures)
            .map(features -> JSON.parseObject(features, Map.class))
            .map(featuresObj -> featuresObj.get(FeaturesConstants.SERVER_MIDDLEWARE_TYPE_KEY))
            .map(String::valueOf)
            .distinct()
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(typeList)) {
            log.error("????????????????????????:[techLinkIdList:{}]", JSON.toJSONString(techLinkIdList));
            return nameResponseList;
        }
        EnginePluginFetchWrapperReq fetchWrapperReq = new EnginePluginFetchWrapperReq();
        fetchWrapperReq.setPluginTypes(typeList);
        ResponseResult<Map<String, List<EnginePluginSimpleInfoResp>>> responseResult
            = sceneManageApi.listEnginePlugins(fetchWrapperReq);
        Map<String, List<EnginePluginSimpleInfoResp>> dataMap = responseResult.getData();
        if (Objects.isNull(responseResult) || dataMap.isEmpty()) {
            log.error("????????????????????????:[typeList:{}]", JSON.toJSONString(typeList));
            return nameResponseList;
        }
        nameResponseList = typeList.stream().map(type -> {
            SupportJmeterPluginNameResponse nameResponse = new SupportJmeterPluginNameResponse();
            nameResponse.setType(type);
            type = type.toLowerCase();
            if (dataMap.containsKey(type)) {
                List<EnginePluginSimpleInfoResp> pluginSimpleInfoResps = dataMap.get(type);
                if (CollectionUtils.isNotEmpty(pluginSimpleInfoResps)) {
                    List<SinglePluginRenderResponse> singlePluginRenderResponseList;
                    singlePluginRenderResponseList = pluginSimpleInfoResps.stream().map(enginePluginSimpleInfoResp -> {
                        SinglePluginRenderResponse renderResponse = new SinglePluginRenderResponse();
                        renderResponse.setLabel(enginePluginSimpleInfoResp.getPluginName());
                        renderResponse.setValue(enginePluginSimpleInfoResp.getPluginId());
                        return renderResponse;
                    }).collect(Collectors.toList());
                    nameResponse.setSinglePluginRenderResponseList(singlePluginRenderResponseList);
                }
            }
            return nameResponse;
        }).collect(Collectors.toList());
        return nameResponseList;
    }

    @Override
    public SupportJmeterPluginVersionResponse getSupportJmeterPluginVersionList(
        SupportJmeterPluginVersionRequest versionRequest) {
        Long pluginId = versionRequest.getPluginId();
        EnginePluginDetailsWrapperReq wrapperReq = new EnginePluginDetailsWrapperReq();
        wrapperReq.setPluginId(pluginId);
        ResponseResult<EnginePluginDetailResp> responseResult = sceneManageApi.getEnginePluginDetails(wrapperReq);
        EnginePluginDetailResp detailResp = responseResult.getData();
        if (!Objects.isNull(detailResp)) {
            SupportJmeterPluginVersionResponse versionResponse = new SupportJmeterPluginVersionResponse();
            versionResponse.setVersionList(detailResp.getSupportedVersions());
            return versionResponse;
        }
        return null;
    }

    private List<FileManageCreateParam> getFileManageCreateParamsByUpdateReq(
        List<FileManageUpdateRequest> fileManageUpdateRequests, String targetScriptPath) {
        return fileManageUpdateRequests.stream().map(fileManageUpdateRequest -> {
            FileManageCreateParam fileManageCreateParam = new FileManageCreateParam();
            fileManageCreateParam.setFileName(fileManageUpdateRequest.getFileName());
            fileManageCreateParam.setFileSize(fileManageUpdateRequest.getFileSize());
            fileManageCreateParam.setFileType(fileManageUpdateRequest.getFileType());
            Map<String, Object> fileExtend = new HashMap<>();
            fileExtend.put("dataCount", fileManageUpdateRequest.getDataCount());
            fileExtend.put("isSplit", fileManageUpdateRequest.getIsSplit());
            fileManageCreateParam.setCustomerId(RestContext.getUser().getCustomerId());
            fileManageCreateParam.setFileExtend(JsonHelper.bean2Json(fileExtend));
            fileManageCreateParam.setUploadPath(targetScriptPath + fileManageUpdateRequest.getFileName());
            fileManageCreateParam.setUploadTime(fileManageUpdateRequest.getUploadTime());
            return fileManageCreateParam;
        }).collect(Collectors.toList());

    }

    private List<FileManageCreateParam> getFileManageCreateParams(
        List<FileManageCreateRequest> fileManageCreateRequests, String targetScriptPath) {
        return fileManageCreateRequests.stream().map(fileManageCreateRequest -> {
            FileManageCreateParam fileManageCreateParam = new FileManageCreateParam();
            fileManageCreateParam.setFileName(fileManageCreateRequest.getFileName());
            fileManageCreateParam.setFileSize(fileManageCreateRequest.getFileSize());
            fileManageCreateParam.setFileType(fileManageCreateRequest.getFileType());
            Map<String, Object> fileExtend = new HashMap<>();
            fileExtend.put("dataCount", fileManageCreateRequest.getDataCount());
            fileExtend.put("isSplit", fileManageCreateRequest.getIsSplit());
            fileManageCreateParam.setFileExtend(JsonHelper.bean2Json(fileExtend));
            fileManageCreateParam.setCustomerId(RestContext.getUser().getCustomerId());
            fileManageCreateParam.setUploadPath(targetScriptPath + fileManageCreateRequest.getFileName());
            fileManageCreateParam.setUploadTime(fileManageCreateRequest.getUploadTime());
            return fileManageCreateParam;
        }).collect(Collectors.toList());

    }

    @Override
    public PagingList<ScriptManageDeployResponse> pageQueryScriptManage(
        ScriptManageDeployPageQueryRequest scriptManageDeployPageQueryRequest) {
        ScriptManageDeployPageQueryParam scriptManageDeployPageQueryParam = new ScriptManageDeployPageQueryParam();
        if (scriptManageDeployPageQueryRequest != null) {
            BeanUtils.copyProperties(scriptManageDeployPageQueryRequest, scriptManageDeployPageQueryParam);
            if (!StringUtil.isBlank(scriptManageDeployPageQueryRequest.getBusinessActivityId())) {
                scriptManageDeployPageQueryParam.setRefType(ScriptManageConstant.BUSINESS_ACTIVITY_REF_TYPE);
                scriptManageDeployPageQueryParam.setRefValue(
                    scriptManageDeployPageQueryRequest.getBusinessActivityId());
            }
            if (!StringUtil.isBlank(scriptManageDeployPageQueryRequest.getBusinessFlowId())) {
                scriptManageDeployPageQueryParam.setRefType(ScriptManageConstant.BUSINESS_PROCESS_REF_TYPE);
                scriptManageDeployPageQueryParam.setRefValue(scriptManageDeployPageQueryRequest.getBusinessFlowId());
            }
            if (!StringUtil.isBlank(scriptManageDeployPageQueryRequest.getBusinessActivityId()) && !StringUtil
                .isBlank(scriptManageDeployPageQueryRequest.getBusinessFlowId())) {
                return PagingList.empty();
            }
            if (!CollectionUtils.isEmpty(scriptManageDeployPageQueryRequest.getTagIds())) {
                List<ScriptTagRefResult> scriptTagRefResults = scriptTagRefDAO.selectScriptTagRefByTagIds(
                    scriptManageDeployPageQueryRequest.getTagIds());
                if (CollectionUtils.isEmpty(scriptTagRefResults)) {
                    //???????????????????????????????????????????????????
                    return PagingList.empty();
                }
                Map<Long, List<ScriptTagRefResult>> scriptTagRefMap = scriptTagRefResults.stream().collect(
                    Collectors.groupingBy(ScriptTagRefResult::getScriptId));
                List<Long> scriptIds = new ArrayList<>();
                for (Map.Entry<Long, List<ScriptTagRefResult>> entry : scriptTagRefMap.entrySet()) {
                    if (entry.getValue().size() == scriptManageDeployPageQueryRequest.getTagIds().size()) {
                        scriptIds.add(entry.getKey());
                    }
                }
                if (CollectionUtils.isEmpty(scriptIds)) {
                    return PagingList.empty();
                }
                scriptManageDeployPageQueryParam.setScriptIds(scriptIds);
            }
        }
        scriptManageDeployPageQueryParam.setCurrent(scriptManageDeployPageQueryRequest.getCurrent());
        scriptManageDeployPageQueryParam.setPageSize(scriptManageDeployPageQueryRequest.getPageSize());
        // todo ?????????jmx ????????????????????????????????????
        scriptManageDeployPageQueryParam.setScriptType(0);
        List<Long> userIdList = RestContext.getQueryAllowUserIdList();
        if (CollectionUtils.isNotEmpty(userIdList)) {
            scriptManageDeployPageQueryParam.setUserIdList(userIdList);
        }
        PagingList<ScriptManageDeployResult> scriptManageDeployResults = scriptManageDAO
            .pageQueryRecentScriptManageDeploy(
                scriptManageDeployPageQueryParam);
        if (scriptManageDeployResults.isEmpty()) {
            return PagingList.empty();
        }
        // ??????????????????
        Map<Long, Long> numMaps = scriptManageDAO.selectScriptDeployNumResult();
        //??????ids
        List<Long> userIds = scriptManageDeployResults.getList().stream().filter(data -> null != data.getUserId()).map(
            ScriptManageDeployResult::getUserId).collect(Collectors.toList());
        //????????????Map key:userId  value:user??????
        Map<Long, User> userMap = userService.getUserMapByIds(userIds);
        List<Long> allowUpdateUserIdList = RestContext.getUpdateAllowUserIdList();
        List<Long> allowDeleteUserIdList = RestContext.getDeleteAllowUserIdList();
        List<Long> allowDownloadUserIdList = RestContext.getDownloadAllowUserIdList();
        List<ScriptManageDeployResponse> scriptManageDeployResponses = scriptManageDeployResults.getList().stream().map(
                scriptManageDeployResult -> {
                    ScriptManageDeployResponse response = new ScriptManageDeployResponse();
                    BeanUtils.copyProperties(scriptManageDeployResult, response);
                    // ???????????????????????????
                    if(numMaps.get(response.getScriptId()) != null && numMaps.get(response.getScriptId()) > 1) {
                        response.setOnlyOne(false);
                    }
                    if (CollectionUtils.isNotEmpty(allowUpdateUserIdList)) {
                        response.setCanEdit(allowUpdateUserIdList.contains(scriptManageDeployResult.getUserId()));
                    }
                    if (CollectionUtils.isNotEmpty(allowDeleteUserIdList)) {
                        response.setCanRemove(allowDeleteUserIdList.contains(scriptManageDeployResult.getUserId()));
                    }
                    if (CollectionUtils.isNotEmpty(allowDownloadUserIdList)) {
                        response.setCanDownload(allowDownloadUserIdList.contains(scriptManageDeployResult.getUserId()));
                    }
                    //?????????id
                    response.setManagerId(scriptManageDeployResult.getUserId());
                    //???????????????
                    String userName = Optional.ofNullable(userMap.get(scriptManageDeployResult.getUserId()))
                            .map(u -> u.getName())
                            .orElse("");
                    response.setManagerName(userName);
                    return response;
                }).collect(Collectors.toList());
        setFileList(scriptManageDeployResponses);
        setTagList(scriptManageDeployResponses);
        setRefName(scriptManageDeployResponses, scriptManageDeployResults);
        return PagingList.of(scriptManageDeployResponses, scriptManageDeployResults.getTotal());
    }

    private void setTagList(List<ScriptManageDeployResponse> scriptManageDeployResponses) {
        if (scriptManageDeployResponses == null || CollectionUtils.isEmpty(scriptManageDeployResponses)) {
            return;
        }
        List<Long> scriptIds = scriptManageDeployResponses.stream().map(ScriptManageDeployResponse::getScriptId)
            .collect(Collectors.toList());
        List<ScriptTagRefResult> scriptTagRefResults = scriptTagRefDAO.selectScriptTagRefByScriptIds(scriptIds);
        if (CollectionUtils.isEmpty(scriptTagRefResults)) {
            return;
        }
        List<Long> tagIds = scriptTagRefResults.stream().map(ScriptTagRefResult::getTagId).collect(Collectors.toList());
        List<TagManageResult> tagManageResults = tagManageDAO.selectScriptTagsByIds(tagIds);
        if (CollectionUtils.isEmpty(tagManageResults)) {
            return;
        }
        Map<Long, List<ScriptTagRefResult>> scriptTagRefMap = scriptTagRefResults.stream().collect(
            Collectors.groupingBy(ScriptTagRefResult::getScriptId));
        List<TagManageResponse> tagManageResponses = tagManageResults.stream().map(tagManageResult -> {
            TagManageResponse tagManageResponse = new TagManageResponse();
            tagManageResponse.setId(tagManageResult.getId());
            tagManageResponse.setTagName(tagManageResult.getTagName());
            return tagManageResponse;
        }).collect(Collectors.toList());
        Map<Long, TagManageResponse> tagManageResponseMap = tagManageResponses.stream().collect(
            Collectors.toMap(TagManageResponse::getId, a -> a, (k1, k2) -> k1));
        scriptManageDeployResponses.forEach(scriptManageDeployResponse -> {
            List<ScriptTagRefResult> scriptTagRefList = scriptTagRefMap.get(scriptManageDeployResponse.getScriptId());
            if (CollectionUtils.isEmpty(scriptTagRefList)) {
                return;
            }
            List<TagManageResponse> resultTagManageResponses = scriptTagRefList.stream().map(
                scriptTagRefResult -> tagManageResponseMap.get(scriptTagRefResult.getTagId())).collect(
                Collectors.toList());
            if (CollectionUtils.isEmpty(resultTagManageResponses)) {
                return;
            }
            scriptManageDeployResponse.setTagManageResponses(resultTagManageResponses);
        });
    }

    private void setFileList(List<ScriptManageDeployResponse> scriptManageDeployResponses) {
        if (scriptManageDeployResponses == null || CollectionUtils.isEmpty(scriptManageDeployResponses)) {
            return;
        }
        List<Long> scriptDeployIds = scriptManageDeployResponses.stream().map(ScriptManageDeployResponse::getId)
            .collect(Collectors.toList());
        List<ScriptFileRefResult> scriptFileRefResults = scriptFileRefDAO.selectFileIdsByScriptDeployIds(
            scriptDeployIds);
        if (CollectionUtils.isEmpty(scriptFileRefResults)) {
            return;
        }
        List<Long> fileIdList = scriptFileRefResults.stream().map(ScriptFileRefResult::getFileId).collect(
            Collectors.toList());
        List<FileManageResponse> fileManageResponses = getFileManageResponseByFileIds(fileIdList);
        if (CollectionUtils.isEmpty(fileManageResponses)) {
            return;
        }
        Map<Long, List<ScriptFileRefResult>> scriptFileRefMap = scriptFileRefResults.stream().collect(
            Collectors.groupingBy(ScriptFileRefResult::getScriptDeployId));
        Map<Long, FileManageResponse> fileManageResultMap = fileManageResponses.stream().collect(
            Collectors.toMap(FileManageResponse::getId, a -> a, (k1, k2) -> k1));
        scriptManageDeployResponses.forEach(scriptManageDeployResponse -> {
            List<ScriptFileRefResult> scriptFileRefList = scriptFileRefMap.get(scriptManageDeployResponse.getId());
            if (CollectionUtils.isEmpty(scriptFileRefList)) {
                return;
            }
            List<FileManageResponse> resultFileManageResponses = scriptFileRefList.stream().map(
                scriptFileRefResult -> fileManageResultMap.get(scriptFileRefResult.getFileId())).collect(
                Collectors.toList());
            if (CollectionUtils.isEmpty(resultFileManageResponses)) {
                return;
            }
            scriptManageDeployResponse.setFileManageResponseList(resultFileManageResponses);
        });
    }

    @Override
    public void deleteScriptManage(Long scriptDeployId) {
        ScriptManageDeployResult scriptManageDeployResult = scriptManageDAO.selectScriptManageDeployById(
            scriptDeployId);
        if (scriptManageDeployResult == null) {
            return;
        }
        ResponseResult<List<SceneManageListResp>> sceneManageList = sceneManageApi.getSceneManageList();
        if (sceneManageList == null || !sceneManageList.getSuccess()) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_DELETE_VALID_ERROR,
                "???????????????????????????" + sceneManageList.getError().getMsg());
        }

        List<ScriptManageDeployResult> existScriptManageDeployResults = scriptManageDAO
            .selectScriptManageDeployByScriptId(scriptManageDeployResult.getScriptId());
        Map<Long, ScriptManageDeployResult> existScriptManageDeployResultMap = existScriptManageDeployResults.stream()
            .collect(Collectors.toMap(ScriptManageDeployResult::getId, o -> o, (k1, k2) -> k1));
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isNotEmpty(sceneManageList.getData())) {
            for (SceneManageListResp sceneManageListResp : sceneManageList.getData()) {
                if (!StringUtil.isBlank(sceneManageListResp.getFeatures())) {
                    Map<String, Object> featuresMap = JsonHelper.json2Map(sceneManageListResp.getFeatures(),
                        String.class, Object.class);
                    if (featuresMap.get(SceneManageConstant.FEATURES_SCRIPT_ID) != null) {
                        if (existScriptManageDeployResultMap.get(
                            Long.valueOf(featuresMap.get(SceneManageConstant.FEATURES_SCRIPT_ID).toString())) != null) {
                            sb.append(sceneManageListResp.getSceneName()).append("???");
                        }
                    }
                }
            }
        }
        if (!StringUtil.isBlank(sb.toString())) {
            sb.deleteCharAt(sb.lastIndexOf("???"));
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_DELETE_VALID_ERROR,
                "????????????????????????????????????????????????????????????:" + sb.toString());
        }
        scriptManageDAO.deleteScriptManageAndDeploy(scriptManageDeployResult.getScriptId());
        List<Long> existScriptManageDeployIds = existScriptManageDeployResults.stream().map(
            ScriptManageDeployResult::getId).collect(Collectors.toList());
        List<ScriptFileRefResult> scriptFileRefResults = scriptFileRefDAO.selectFileIdsByScriptDeployIds(
            existScriptManageDeployIds);
        if (CollectionUtils.isNotEmpty(scriptFileRefResults)) {
            List<Long> scriptFileRefIds = scriptFileRefResults.stream().map(ScriptFileRefResult::getId).collect(
                Collectors.toList());
            scriptFileRefDAO.deleteByIds(scriptFileRefIds);
            List<Long> fileIds = scriptFileRefResults.stream().map(ScriptFileRefResult::getFileId).collect(
                Collectors.toList());
            List<FileManageResult> fileManageResults = fileManageDAO.selectFileManageByIds(fileIds);
            List<String> filePaths = fileManageResults.stream().map(FileManageResult::getUploadPath).collect(
                Collectors.toList());
            fileApi.deleteFile(filePaths);
            fileManageDAO.deleteByIds(fileIds);
        }
        scriptTagRefDAO.deleteByScriptId(scriptManageDeployResult.getScriptId());

    }

    @Override
    public void createScriptTagRef(ScriptTagCreateRefRequest scriptTagCreateRefRequest) {
        if (scriptTagCreateRefRequest == null || scriptTagCreateRefRequest.getScriptDeployId() == null) {
            return;
        }
        if (scriptTagCreateRefRequest.getTagNames().size() > 10) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_TAG_ADD_VALID_ERROR, "???????????????????????????????????????10");
        }
        List<String> collect = scriptTagCreateRefRequest.getTagNames().stream().filter(o -> o.length() > 10).collect(
            Collectors.toList());
        if (CollectionUtils.isNotEmpty(collect)) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_TAG_ADD_VALID_ERROR, "??????????????????????????????10");
        }
        ScriptManageDeployResult scriptManageDeployResult = scriptManageDAO.selectScriptManageDeployById(
            scriptTagCreateRefRequest.getScriptDeployId());
        if (scriptManageDeployResult != null) {
            scriptTagRefDAO.deleteByScriptId(scriptManageDeployResult.getScriptId());
            if (CollectionUtils.isNotEmpty(scriptTagCreateRefRequest.getTagNames())) {
                List<TagManageParam> tagManageParams = scriptTagCreateRefRequest.getTagNames().stream().distinct().map(
                    tagName -> {
                        TagManageParam tagManageParam = new TagManageParam();
                        tagManageParam.setTagName(tagName);
                        //?????????????????????
                        tagManageParam.setTagStatus(0);
                        //?????????????????????
                        tagManageParam.setTagType(0);
                        return tagManageParam;
                    }).collect(Collectors.toList());
                List<Long> tagIds = tagManageDAO.addScriptTags(tagManageParams,0);
                scriptTagRefDAO.addScriptTagRef(tagIds, scriptManageDeployResult.getScriptId());
            }
        }

    }

    @Override
    public List<TagManageResponse> queryScriptTagList() {
        List<TagManageResult> tagManageResults = tagManageDAO.selectAllScript();
        if (CollectionUtils.isNotEmpty(tagManageResults)) {
            return tagManageResults.stream().map(tagManageResult -> {
                TagManageResponse tagManageResponse = new TagManageResponse();
                tagManageResponse.setId(tagManageResult.getId());
                tagManageResponse.setTagName(tagManageResult.getTagName());
                return tagManageResponse;
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public ScriptManageDeployDetailResponse getScriptManageDeployDetail(Long scriptDeployId) {
        ScriptManageDeployDetailResponse result = new ScriptManageDeployDetailResponse();
        ScriptManageDeployResult scriptManageDeployResult = scriptManageDAO.selectScriptManageDeployById(
            scriptDeployId);
        if (scriptManageDeployResult == null) {
            return null;
        }
        BeanUtils.copyProperties(scriptManageDeployResult, result);
        setRefName(result, scriptManageDeployResult);
        setFileList(result);
        setFeatures(result, scriptManageDeployResult.getFeature());
        return result;
    }

    /**
     * ????????????????????????
     *
     * @param scriptManageDeployResponses
     * @param scriptManageDeployResults
     */
    private void setRefName(List<ScriptManageDeployResponse> scriptManageDeployResponses,
        PagingList<ScriptManageDeployResult> scriptManageDeployResults) {
        if (scriptManageDeployResults == null || CollectionUtils.isEmpty(scriptManageDeployResults.getList())
            || CollectionUtils.isEmpty(scriptManageDeployResponses)) {
            return;
        }
        Map<String, List<ScriptManageDeployResult>> refTypeMap = scriptManageDeployResults.getList()
            .stream()
            .filter(item -> item.getRefType() != null)
            .collect(
                Collectors.groupingBy(ScriptManageDeployResult::getRefType));
        if (refTypeMap == null || refTypeMap.size() <= 0) {
            return;
        }
        Map<Long, ScriptManageDeployResult> longScriptManageDeployResultMap = scriptManageDeployResults.getList()
            .stream().collect(Collectors.toMap(ScriptManageDeployResult::getId, a -> a, (k1, k2) -> k1));
        Map<Long, Scene> businessFlowMap = new HashMap<>();
        Map<Long, BusinessLinkManageTable> businessActivityMap = new HashMap<>();
        for (Map.Entry<String, List<ScriptManageDeployResult>> entry : refTypeMap.entrySet()) {
            //?????????????????????
            if (ScriptManageConstant.BUSINESS_PROCESS_REF_TYPE.equals(entry.getKey())) {
                List<Long> businessFlowIds = entry.getValue().stream().map(
                    scriptManageDeployResult -> Long.parseLong(scriptManageDeployResult.getRefValue())).collect(
                    Collectors.toList());
                List<Scene> scenes = tSceneMapper.selectBusinessFlowNameByIds(businessFlowIds);
                if (CollectionUtils.isNotEmpty(scenes)) {
                    businessFlowMap = scenes.stream().collect(Collectors.toMap(Scene::getId, a -> a, (k1, k2) -> k1));
                }
            }
            //?????????????????????
            if (ScriptManageConstant.BUSINESS_ACTIVITY_REF_TYPE.equals(entry.getKey())) {
                List<Long> businessActivityIds = entry.getValue().stream().map(
                    scriptManageDeployResult -> Long.parseLong(scriptManageDeployResult.getRefValue())).collect(
                    Collectors.toList());
                List<BusinessLinkManageTable> businessLinkManageTables = tBusinessLinkManageTableMapper
                    .selectBussinessLinkByIdList(businessActivityIds);
                if (CollectionUtils.isNotEmpty(businessLinkManageTables)) {
                    businessActivityMap = businessLinkManageTables.stream().collect(
                        Collectors.toMap(BusinessLinkManageTable::getLinkId, a -> a, (k1, k2) -> k1));
                }
            }
        }
        Map<Long, Scene> finalBusinessFlowMap = businessFlowMap;
        Map<Long, BusinessLinkManageTable> finalBusinessActivityMap = businessActivityMap;
        scriptManageDeployResponses.forEach(scriptManageDeployResponse -> {
            ScriptManageDeployResult scriptManageDeployResult = longScriptManageDeployResultMap.get(
                scriptManageDeployResponse.getId());
            if (ScriptManageConstant.BUSINESS_PROCESS_REF_TYPE.equals(scriptManageDeployResponse.getRefType())) {
                Scene scene = finalBusinessFlowMap.get(Long.parseLong(scriptManageDeployResult.getRefValue()));
                if (scene != null) {
                    scriptManageDeployResponse.setRefName(scene.getSceneName());
                }
            }
            if (ScriptManageConstant.BUSINESS_ACTIVITY_REF_TYPE.equals(scriptManageDeployResponse.getRefType())) {
                BusinessLinkManageTable businessLinkManageTable = finalBusinessActivityMap.get(
                    Long.parseLong(scriptManageDeployResult.getRefValue()));
                if (businessLinkManageTable != null) {
                    scriptManageDeployResponse.setRefName(businessLinkManageTable.getLinkName());
                }

            }
        });
    }

    private void setFileList(ScriptManageDeployDetailResponse result) {
        if (result == null || result.getId() == null) {
            return;
        }
        List<ScriptFileRefResult> scriptFileRefResults = scriptFileRefDAO.selectFileIdsByScriptDeployId(result.getId());
        if (CollectionUtils.isEmpty(scriptFileRefResults)) {
            log.info("????????????????????????id");
            return;
        }
        List<Long> fileIds = scriptFileRefResults.stream().map(ScriptFileRefResult::getFileId).collect(
            Collectors.toList());
        List<FileManageResponse> totalFileList = getFileManageResponseByFileIds(fileIds);
        if (CollectionUtils.isNotEmpty(totalFileList)) {
            List<FileManageResponse> fileManageResponseList = totalFileList.stream().filter(
                f -> f.getFileType().equals(FileTypeEnum.SCRIPT.getCode()) || f.getFileType()
                    .equals(FileTypeEnum.DATA.getCode())).collect(
                Collectors.toList());
            List<FileManageResponse> attachmentManageResponseList = totalFileList.stream().filter(
                f -> f.getFileType().equals(FileTypeEnum.ATTACHMENT.getCode())).collect(
                Collectors.toList());
            result.setFileManageResponseList(fileManageResponseList);
            result.setAttachmentManageResponseList(attachmentManageResponseList);
        }
    }

    private void setFeatures(ScriptManageDeployDetailResponse result, String feature) {
        if (StringUtils.isNotBlank(feature)) {
            Map<String, Object> featureMap = JSON.parseObject(feature, Map.class);
            if (featureMap.containsKey(FeaturesConstants.PLUGIN_CONFIG)) {
                String pluginConfigContent = JSON.toJSONString(featureMap.get(FeaturesConstants.PLUGIN_CONFIG));
                List<PluginConfigDetailResponse> pluginConfigDetailResponseList
                    = JSON.parseArray(pluginConfigContent, PluginConfigDetailResponse.class);
                result.setPluginConfigDetailResponseList(pluginConfigDetailResponseList);
            }
        }
    }

    private List<FileManageResponse> getFileManageResponseByFileIds(List<Long> fileIds) {
        List<FileManageResult> fileManageResults = fileManageDAO.selectFileManageByIds(fileIds);
        List<FileManageResponse> fileManageResponses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fileManageResults)) {
            for (FileManageResult fileManageResult : fileManageResults) {
                FileManageResponse fileManageResponse = new FileManageResponse();
                BeanUtils.copyProperties(fileManageResult, fileManageResponse);
                if (StringUtils.isNotEmpty(fileManageResult.getFileExtend())) {
                    Map<String, Object> stringObjectMap = JsonHelper.json2Map(fileManageResult.getFileExtend(),
                        String.class, Object.class);
                    if (stringObjectMap != null && stringObjectMap.get("dataCount") != null && !StringUtil.isBlank(
                        stringObjectMap.get("dataCount").toString())) {
                        fileManageResponse.setDataCount(Long.valueOf(stringObjectMap.get("dataCount").toString()));
                    }
                    if (stringObjectMap != null && stringObjectMap.get("isSplit") != null && !StringUtil.isBlank(
                        stringObjectMap.get("isSplit").toString())) {
                        fileManageResponse.setIsSplit(Integer.valueOf(stringObjectMap.get("isSplit").toString()));
                    }
                }
                String uploadUrl = fileManageResult.getUploadPath();
                fileManageResponse.setUploadPath(uploadUrl);
                fileManageResponses.add(fileManageResponse);
            }
            return fileManageResponses;
        }
        return null;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param result
     * @param scriptManageDeployResult
     */
    private void setRefName(ScriptManageDeployDetailResponse result,
        ScriptManageDeployResult scriptManageDeployResult) {
        if (StringUtils.isEmpty(scriptManageDeployResult.getRefValue())) {
            return;
        }
        //?????????????????????
        if (ScriptManageConstant.BUSINESS_PROCESS_REF_TYPE.equals(scriptManageDeployResult.getRefType())) {
            long businessId = Long.parseLong(scriptManageDeployResult.getRefValue());
            List<Scene> scenes = tSceneMapper.selectBusinessFlowNameByIds(Collections.singletonList(businessId));
            if (CollectionUtils.isNotEmpty(scenes)) {
                result.setRefName(scenes.get(0).getSceneName());
            }
        }
        //?????????????????????
        if (ScriptManageConstant.BUSINESS_ACTIVITY_REF_TYPE.equals(scriptManageDeployResult.getRefType())) {
            long businessId = Long.parseLong(scriptManageDeployResult.getRefValue());
            List<BusinessLinkManageTable> businessLinkManageTables = tBusinessLinkManageTableMapper
                .selectBussinessLinkByIdList(Collections.singletonList(businessId));
            if (CollectionUtils.isNotEmpty(businessLinkManageTables)) {
                result.setRefName(businessLinkManageTables.get(0).getLinkName());
            }
        }
    }

    /**
     * ????????????????????????
     *
     * @param scriptManageDeployCreateRequest
     */
    private void checkCreateScriptManageParam(ScriptManageDeployCreateRequest scriptManageDeployCreateRequest) {
        if (scriptManageDeployCreateRequest == null) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "???????????????");
        }
        if (StringUtil.isBlank(scriptManageDeployCreateRequest.getName())) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "?????????????????????");
        }
        if (StringUtil.isBlank(scriptManageDeployCreateRequest.getRefType())) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "???????????????????????????");
        }
        if (StringUtil.isBlank(scriptManageDeployCreateRequest.getRefValue())) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "????????????????????????");
        }
        if (scriptManageDeployCreateRequest.getType() == null) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "?????????????????????");
        }
        if (CollectionUtils.isEmpty(scriptManageDeployCreateRequest.getFileManageCreateRequests())) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "?????????????????????");
        }
        boolean existJmx = false;
        for (FileManageCreateRequest fileManageCreateRequest : scriptManageDeployCreateRequest
            .getFileManageCreateRequests()) {
            if (StringUtil.isBlank(fileManageCreateRequest.getFileName())) {
                throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "???????????????????????????????????????");
            }
            if (fileManageCreateRequest.getFileName().length() > 64) {
                throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "??????????????????????????????????????????64???");
            }
            if (fileManageCreateRequest.getFileType() == null) {
                throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR,
                    "??????????????????????????????????????????fileName=" + fileManageCreateRequest.getFileName());
            }
            if (fileManageCreateRequest.getFileName().contains(" ")) {
                throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "????????????????????????");
            }
            if (fileManageCreateRequest.getFileType() == 0 && fileManageCreateRequest.getIsDeleted() == 0) {
                existJmx = true;
            }
            fileManageCreateRequest.setFileName(FileUtil.replaceFileName(fileManageCreateRequest.getFileName()));
        }
        if (!existJmx) {
            throw new TroWebException(ExceptionCode.SCRIPT_MANAGE_CREATE_PARAM_VALID_ERROR, "???????????????????????????????????????");
        }
    }

    /**
     * ??????????????????
     *
     * @param scriptManageDeployUpdateRequest ????????????
     */
    private void checkUpdateScriptManageParam(ScriptManageDeployUpdateRequest scriptManageDeployUpdateRequest) {
        ScriptManageExceptionUtil.isUpdateValidError(scriptManageDeployUpdateRequest == null, "????????????!");
        ScriptManageExceptionUtil.isUpdateValidError(scriptManageDeployUpdateRequest.getId() == null, "??????id??????!");
        ScriptManageExceptionUtil.isUpdateValidError(StringUtil.isBlank(scriptManageDeployUpdateRequest.getName()), "??????????????????!");
        ScriptManageExceptionUtil.isUpdateValidError(StringUtil.isBlank(scriptManageDeployUpdateRequest.getRefType()), "????????????????????????!");
        ScriptManageExceptionUtil.isUpdateValidError(StringUtil.isBlank(scriptManageDeployUpdateRequest.getRefValue()), "?????????????????????!");
        ScriptManageExceptionUtil.isUpdateValidError(scriptManageDeployUpdateRequest.getType() == null, "??????????????????!");
        ScriptManageExceptionUtil.isUpdateValidError(CollectionUtils.isEmpty(scriptManageDeployUpdateRequest.getFileManageUpdateRequests()), "??????????????????!");

        boolean existJmx = false;
        for (FileManageUpdateRequest fileManageUpdateRequest : scriptManageDeployUpdateRequest
            .getFileManageUpdateRequests()) {

            ScriptManageExceptionUtil.isUpdateValidError(StringUtil.isBlank(fileManageUpdateRequest.getFileName()), "????????????????????????????????????!");
            ScriptManageExceptionUtil.isUpdateValidError(fileManageUpdateRequest.getFileName().length() > 64, "??????????????????????????????????????????64!");
            ScriptManageExceptionUtil.isUpdateValidError(fileManageUpdateRequest.getFileName().contains(" "), "?????????????????????!");
            ScriptManageExceptionUtil.isUpdateValidError(fileManageUpdateRequest.getFileType() == null, "??????????????????????????????????????????fileName=" + fileManageUpdateRequest.getFileName());

            if (fileManageUpdateRequest.getFileType() == 0 && fileManageUpdateRequest.getIsDeleted() == 0) {
                existJmx = true;
            }
            fileManageUpdateRequest.setFileName(FileUtil.replaceFileName(fileManageUpdateRequest.getFileName()));
        }

        ScriptManageExceptionUtil.isUpdateValidError(!existJmx, "????????????????????????????????????!");
    }

    /**
     * ?????????????????? ?????? + ??????id + ??????
     *
     * @param scriptManageDeployResult ????????????
     * @return ????????????
     */
    private String getTargetScriptPath(ScriptManageDeployResult scriptManageDeployResult) {
        return scriptFilePath + "/"+ scriptManageDeployResult.getScriptId() + "/"
            + scriptManageDeployResult.getScriptVersion() + "/";
    }

}
