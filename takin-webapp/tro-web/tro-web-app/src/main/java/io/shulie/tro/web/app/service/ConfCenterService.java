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

package io.shulie.tro.web.app.service;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import com.pamirs.tro.common.ResponseOk;
import com.pamirs.tro.common.constant.MQConstant;
import com.pamirs.tro.common.constant.TRODictTypeEnum;
import com.pamirs.tro.common.constant.TROErrorEnum;
import com.pamirs.tro.common.constant.WListRuleEnum;
import com.pamirs.tro.common.constant.WListTypeEnum;
import com.pamirs.tro.common.exception.TROModuleException;
import com.pamirs.tro.common.redis.RedisKey;
import com.pamirs.tro.common.redis.WhiteBlackListRedisKey;
import com.pamirs.tro.common.util.PageInfo;
import com.pamirs.tro.common.util.RequestPradarUtil;
import com.pamirs.tro.common.util.TroFileUtil;
import com.pamirs.tro.entity.dao.confcenter.TApplicationMntDao;
import com.pamirs.tro.entity.dao.confcenter.TLinkMntDao;
import com.pamirs.tro.entity.dao.confcenter.TWListMntDao;
import com.pamirs.tro.entity.domain.entity.TAlarm;
import com.pamirs.tro.entity.domain.entity.TApplicationIp;
import com.pamirs.tro.entity.domain.entity.TApplicationMnt;
import com.pamirs.tro.entity.domain.entity.TBList;
import com.pamirs.tro.entity.domain.entity.TLinkServiceMnt;
import com.pamirs.tro.entity.domain.entity.TPradaHttpData;
import com.pamirs.tro.entity.domain.entity.TSecondLinkMnt;
import com.pamirs.tro.entity.domain.entity.TWList;
import com.pamirs.tro.entity.domain.entity.user.User;
import com.pamirs.tro.entity.domain.query.BListQueryParam;
import com.pamirs.tro.entity.domain.query.Result;
import com.pamirs.tro.entity.domain.query.TWListVo;
import com.pamirs.tro.entity.domain.vo.TApplicationInterface;
import com.pamirs.tro.entity.domain.vo.TLinkApplicationInterface;
import com.pamirs.tro.entity.domain.vo.TLinkBasicVO;
import com.pamirs.tro.entity.domain.vo.TLinkMntDictoryVo;
import com.pamirs.tro.entity.domain.vo.TLinkNodesVo;
import com.pamirs.tro.entity.domain.vo.TLinkServiceMntVo;
import com.pamirs.tro.entity.domain.vo.TLinkTopologyInfoVo;
import com.pamirs.tro.entity.domain.vo.TUploadInterfaceDataVo;
import io.shulie.tro.web.app.cache.webimpl.AllUserCache;
import io.shulie.tro.web.app.common.CommonService;
import io.shulie.tro.web.app.common.Response;
import io.shulie.tro.web.app.common.RestContext;
import io.shulie.tro.web.app.context.OperationLogContextHolder;
import io.shulie.tro.web.app.service.auth.TroAuthService;
import io.shulie.tro.web.app.service.linkManage.impl.WhiteListFileService;
import io.shulie.tro.web.app.service.user.TroUserService;
import io.shulie.tro.web.app.service.user.TroWebUserService;
import io.shulie.tro.web.data.dao.application.ApplicationDAO;
import io.shulie.tro.web.data.dao.blacklist.BlackListDAO;
import io.shulie.tro.web.data.param.application.ApplicationCreateParam;
import io.shulie.tro.web.data.param.blacklist.BlackListCreateParam;
import io.shulie.tro.web.data.result.user.UserCacheResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * ??????: ????????????service
 *
 * @author shulie
 * @date 2019/4/4 16:36
 * @see
 */
@Service
@Slf4j
public class ConfCenterService extends CommonService {
    /**
     * session ??????
     */
    public static final String CONST_CAS_ASSERTION = "_const_cas_assertion_";
    @Autowired
    @Qualifier("modifyMonitorThreadPool")
    protected ThreadPoolExecutor modifyMonitorExecutor;
    @Autowired
    private TroAuthService troAuthService;
    @Autowired
    private TroWebUserService troWebUserService;
    @Autowired
    private TroUserService troUserService;
    @Autowired
    private WhiteListFileService whiteListFileService;
    @Value("${spring.config.whiteListPath}")
    private String whiteListPath;
    @Autowired
    private BlackListDAO blackListDAO;
    @Autowired
    private ApplicationDAO applicationDAO;
    @Autowired
    private AllUserCache allUserCache;

    /**
     * ??????: ????????????????????????????????????,????????????????????????,???????????????????????????
     *
     * @throws TROModuleException ??????????????????
     * @author shulie
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveApplication(TApplicationMnt tApplicationMnt) throws TROModuleException {
        User user = RestContext.getUser();
        if (user == null) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_APPLICATION_DUPICATE_EXCEPTION);
        }
        int applicationExist = tApplicationMntDao.applicationExistByCustomerIdAndAppName(
            user.getCustomerId(), tApplicationMnt.getApplicationName());
        if (applicationExist > 0) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_APPLICATION_DUPICATE_EXCEPTION);
        }
        addApplication(tApplicationMnt);
        addApplicationToDataBuild(tApplicationMnt);
        addApplicationToLinkDetection(tApplicationMnt);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAgentRegisteApplication(TApplicationMnt tApplicationMnt) throws TROModuleException {
        if (StringUtils.isBlank(RestContext.getTenantUserKey())) {
            OperationLogContextHolder.ignoreLog();
            log.error("tenantUserKey???????????????????????????");
        } else {
            UserCacheResult user = allUserCache.getCachedUserByKey(RestContext.getTenantUserKey());
            int applicationExist = tApplicationMntDao.applicationExistByCustomerIdAndAppName(
                user.getCustomerId(), tApplicationMnt.getApplicationName());
            if (applicationExist > 0) {
                OperationLogContextHolder.ignoreLog();
                return;
            }
            tApplicationMnt.setCustomerId(user.getCustomerId());
            tApplicationMnt.setUserId(user.getId());
            addApplication(tApplicationMnt);
            addApplicationToDataBuild(tApplicationMnt);
            addApplicationToLinkDetection(tApplicationMnt);
        }
    }

    /**
     * ??????: ??????????????????????????????????????????
     *
     * @author shulie
     */
    private void addApplicationToLinkDetection(TApplicationMnt tApplicationMnt) {
        Map<String, Object> map = new HashMap<String, Object>(10) {
            private static final long serialVersionUID = 1L;

            {
                put("linkDetectionId", snowflake.next());
                put("applicationId", tApplicationMnt.getApplicationId());
            }
        };
        TLinkDetectionDao.insertLinkDetection(map);
    }

    /**
     * ??????: ??????????????????????????????????????????
     *
     * @param tApplicationMnt ??????????????????
     * @author shulie
     */
    private void addApplicationToDataBuild(TApplicationMnt tApplicationMnt) {
        Map<String, Object> map = new HashMap<String, Object>(10) {
            private static final long serialVersionUID = 1L;

            {
                put("dataBuildId", snowflake.next());
                put("applicationId", tApplicationMnt.getApplicationId());
                if ("0".equals(tApplicationMnt.getCacheExpTime())) {
                    put("cacheBuildStatus", 2);
                }
            }
        };
        TDataBuildDao.insertDataBuild(map);
    }

    @PostConstruct
    public void initWhiteList() {
        writeWhiteListFile();
    }

    /**
     * ??????: ??????????????????
     *
     * @param tApplicationMnt ?????????????????????
     * @author shulie
     */
    private void addApplication(TApplicationMnt tApplicationMnt) throws TROModuleException {
        tApplicationMnt.setApplicationId(snowflake.next());
        tApplicationMnt.setCacheExpTime(
            StringUtils.isEmpty(tApplicationMnt.getCacheExpTime()) ? "0" : tApplicationMnt.getCacheExpTime());
        ApplicationCreateParam createParam = new ApplicationCreateParam();
        BeanUtils.copyProperties(tApplicationMnt, createParam);
        applicationDAO.insert(createParam);

        TroFileUtil.createFile(getBasePath() + tApplicationMnt.getApplicationName());
    }

    /**
     * ??????: ?????????????????????
     *
     * @param paramMap ????????????
     * @return ???????????????
     * @author shulie
     */
    public PageInfo<TApplicationInterface> queryWList(Map<String, Object> paramMap) {
        String applicationName = MapUtils.getString(paramMap, "applicationName");
        String principalNo = MapUtils.getString(paramMap, "principalNo");
        String type = MapUtils.getString(paramMap, "type");
        String whiteListUrl = MapUtils.getString(paramMap, "whiteListUrl");
        Long applicationId = MapUtils.getLong(paramMap, "applicationId");
        PageHelper.startPage(PageInfo.getPageNum(paramMap), PageInfo.getPageSize(paramMap));
        List<TApplicationInterface> queryWListInfo = tWListMntDao.queryOnlyWList(applicationName, principalNo, type,
            whiteListUrl, null, applicationId);

        return new PageInfo<>(queryWListInfo.isEmpty() ? Lists.newArrayList() : queryWListInfo);
    }

    public List<TApplicationInterface> queryWListDownLoad(Map<String, Object> paramMap) {
        String applicationName = MapUtils.getString(paramMap, "applicationName");
        String principalNo = MapUtils.getString(paramMap, "principalNo");
        String type = MapUtils.getString(paramMap, "type");
        String whiteListUrl = MapUtils.getString(paramMap, "whiteListUrl");
        List<String> applicationIds = null;
        Object wlistIds = paramMap.get("wlistIds");
        if (null != wlistIds) {
            applicationIds = (List)wlistIds;
        }
        return tWListMntDao.queryOnlyWList(applicationName, principalNo, type, whiteListUrl, applicationIds, null);
    }

    /**
     * ??????????????????
     *
     * @param applicationId
     */
    public void projectPressureSwitch(Long applicationId) {
        // tWListMntDao.updateWListById();
    }

    /**
     * ??????: ????????????????????????
     *
     * @param paramMap ????????????
     * @return ????????????
     * @author shulie
     */
    public PageInfo<TApplicationMnt> queryApplicationList(Map<String, Object> paramMap) {
        String applicationName = MapUtils.getString(paramMap, "applicationName");
        List<String> applicationIds = (List<String>)MapUtils.getObject(paramMap, "applicationIds");
        if (!StringUtils.equals("-1", MapUtils.getString(paramMap, "pageSize"))) {
            PageHelper.startPage(PageInfo.getPageNum(paramMap), PageInfo.getPageSize(paramMap));
        }
        List<TApplicationMnt> queryApplicationList = tApplicationMntDao.queryApplicationList(applicationName,
            applicationIds);

        return new PageInfo<>(queryApplicationList.isEmpty() ? Lists.newArrayList() : queryApplicationList);
    }

    /**
     * ??????: ????????????id????????????????????????
     *
     * @param applicationId ??????id
     * @return ????????????
     * @author shulie
     */
    public TApplicationMnt queryApplicationinfoById(long applicationId) {
        TApplicationMnt tApplicationMnt = tApplicationMntDao.queryApplicationinfoById(applicationId);

        return tApplicationMnt;
    }

    /**
     * ??????: ????????????id?????????????????????????????????
     *
     * @param applicationId ??????id
     * @return ????????????
     * @author JasonYan
     */
    public TApplicationMnt queryApplicationinfoByIdAndRole(long applicationId) {
        TApplicationMnt tApplicationMnt = tApplicationMntDao.queryApplicationinfoById(applicationId);
        return tApplicationMnt;
    }

    /**
     * ??????: ????????????id????????????????????????(????????????????????????)
     *
     * @param applicationIds ??????ids
     * @author shulie
     */
    @Transactional(rollbackFor = Exception.class)
    public String deleteApplicationinfoByIds(String applicationIds) {
        GetDeleteIds deleteIds = new GetDeleteIds(applicationIds, "applicationName").invoke(tApplicationMntDao);
        List<String> ableDeleteApplicationList = deleteIds.getAbleDeleteList();
        if (!ableDeleteApplicationList.isEmpty()) {
            tWListMntDao.deleteApplicationinfoRelatedInterfaceByIds(ableDeleteApplicationList);
            ableDeleteApplicationList.forEach(applicationId -> {
                TApplicationMnt tApplicationMnt = tApplicationMntDao.queryApplicationinfoById(
                    Long.parseLong(applicationId));
                redisManager.removeKey(
                    WhiteBlackListRedisKey.TRO_WHITE_LIST_KEY + tApplicationMnt.getApplicationName());
                redisManager.removeKey(
                    WhiteBlackListRedisKey.TRO_WHITE_LIST_KEY_METRIC + tApplicationMnt.getApplicationName());
            });
            //?????????????????????????????????
            redisManager.removeKey(WhiteBlackListRedisKey.TRO_WHITE_LIST_KEY);
            redisManager.removeKey(WhiteBlackListRedisKey.TRO_WHITE_LIST_KEY_METRIC);
            tApplicationMntDao.queryApplicationName(ableDeleteApplicationList).forEach(
                applicationName -> TroFileUtil.recursiveDeleteFile(new File(getBasePath() + applicationName)));
            tApplicationMntDao.deleteApplicationinfoByIds(ableDeleteApplicationList);
            TDataBuildDao.deleteApplicationToDataBuild(ableDeleteApplicationList);
            TLinkDetectionDao.deleteApplicationToLinkDetection(ableDeleteApplicationList);
            tShadowTableDataSourceDao.deleteByApplicationIdList(ableDeleteApplicationList);

            Map<String, Object> logMap = Maps.newHashMap();
            List<Map<String, Object>> ableDeleteApplicationWlistData = tApplicationMntDao.queryApplicationListByIds(
                ableDeleteApplicationList);
            List<Map<String, Object>> ableDeleteDataBuild = TDataBuildDao.queryDataBuildListByIds(
                ableDeleteApplicationList);
            List<Map<String, Object>> ableDeleteLinkDetection = TLinkDetectionDao.queryLinkDetectionListByIds(
                ableDeleteApplicationList);
            logMap.put("ableDeleteApplicationWlistData", ableDeleteApplicationWlistData);
            logMap.put("ableDeleteDataBuild", ableDeleteDataBuild);
            logMap.put("ableDeleteLinkDetection", ableDeleteLinkDetection);
        }
        return deleteIds.getResult();
    }

    // =================================== ???????????????=====================================================

    /**
     * ??????: ?????????????????????????????????
     *
     * @return ????????????
     * @author shulie
     */
    public List<Map<String, Object>> queryApplicationdata() {
        List<Map<String, Object>> list = transferElementToString(tApplicationMntDao.queryApplicationdata());

        return list;
    }

    //    private static final String whiteListPath = "/opt/tro/conf/tro-remote/api/confcenter/wbmnt/query/";

    /**
     * ??????: ????????????id??????????????????
     *
     * @param tApplicationMnt ???????????????
     * @author shulie
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateApplicationinfo(TApplicationMnt tApplicationMnt) throws TROModuleException {
        TApplicationMnt originApplicationMnt = tApplicationMntDao.queryApplicationinfoById(
            tApplicationMnt.getApplicationId());
        String originApplicationName = originApplicationMnt.getApplicationName();
        //?????????????????????  applicationName  ???????????????appName????????????
        if (!StringUtils.equals(originApplicationName, tApplicationMnt.getApplicationName())) {
            int applicationExist = tApplicationMntDao.applicationExist(tApplicationMnt.getApplicationName());
            if (applicationExist > 0) {

                throw new TROModuleException(TROErrorEnum.CONFCENTER_UPDATE_APPLICATION_DUPICATE_EXCEPTION);
            }
            TroFileUtil.recursiveDeleteFile(new File(getBasePath() + originApplicationName));
            TroFileUtil.createFile(getBasePath() + tApplicationMnt.getApplicationName());
        }
        tApplicationMntDao.updateApplicationinfo(tApplicationMnt);

    }

    /**
     * ??????: ?????????????????????
     *
     * @param twListVo ??????????????????
     * @throws TROModuleException ??????
     * @author shulie
     */
    public List<String> saveWList(TWListVo twListVo) throws TROModuleException {
        List<String> duplicateUrlList = Lists.newArrayList();
        //MQ????????????, ???????????????????????????????????????interfaceName,??????queueName
        if (WListTypeEnum.MQ.getValue().equals(twListVo.getType())) {
            int wListExist = tWListMntDao.queryWListCountByMqInfo(twListVo);
            if (wListExist > 0) {
                throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_WLIST_DUPICATE_EXCEPTION);
            }
            TWList tWList = new TWList();
            BeanUtils.copyProperties(twListVo, tWList);
            addWList(tWList);
            //?????????????????????????????????
        } else {
            String appId = twListVo.getApplicationId();
            List<String> list = twListVo.getList();
            List<TWList> twLists = Lists.newArrayList();
            list.forEach(url -> {
                int wListExist = tWListMntDao.wListExist(appId, url, twListVo.getUseYn());
                if (wListExist > 0) {
                    duplicateUrlList.add(url);
                } else {
                    TWList tWList = new TWList();
                    BeanUtils.copyProperties(twListVo, tWList);
                    tWList.setInterfaceName(url);
                    twLists.add(tWList);
                }
            });
            //????????????
            if (twLists.size() == 0 && duplicateUrlList.size() == 0) {
                throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_WLIST_INTERFACE_LOST_EXCEPTION);
            }
            if (twLists.size() == 0 && duplicateUrlList.size() != 0) {
                throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_WLIST_DUPICATE_EXCEPTION);
                //for update
            }

            batchAddWList(twLists);
        }
        writeWhiteListFile();
        return duplicateUrlList;
    }

    private void writeWhiteListFile() {
        try {
            Map<String, List<Map<String, Object>>> result = queryBWList("");
            if (null != result && result.size() > 0) {
                File file = new File(whiteListPath);
                if (!file.exists()) {
                    file.mkdirs();
                }

                if (file.exists()) {
                    file = new File(whiteListPath + "bwlist");
                    if (!file.isFile()) {
                        file.createNewFile();
                    }
                }

                ResponseOk.ResponseResult response = ResponseOk.result(result);
                String content = JSONObject.toJSONString(response);
                FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), false);
                BufferedWriter bufferWritter = new BufferedWriter(fileWriter);
                bufferWritter.write(content);
                bufferWritter.close();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * ??????: ?????????????????????????????????
     *
     * @param twLists ????????????????????????
     * @author shulie
     */
    private void batchAddWList(List<TWList> twLists) {
        tWListMntDao.batchAddWList(twLists);
    }

    /**
     * ??????: ???????????????????????????
     *
     * @param tWList ??????????????????
     * @author shulie
     */
    private void addWList(TWList tWList) {
        tWListMntDao.addWList(tWList);
    }

    /**
     * ??????: ?????????????????????,???????????????????????????????????????????????????,???????????????List<TLinkApplicationInterface>
     *
     * @param applicationName ????????????
     * @param principalNo     ???????????????
     * @param type            ???????????????
     * @return ???????????????????????????
     * @author shulie
     */

    public List<TApplicationInterface> queryOnlyWList(String applicationName, String principalNo, String type, String x,
        Long appId) {
        List<TApplicationInterface> queryWListInfo = tWListMntDao.queryOnlyWList(applicationName, principalNo, type, x,
            null, null);
        return queryWListInfo.isEmpty() ? Lists.newArrayList() : queryWListInfo;
    }

    /**
     * ??????: ????????????????????????????????????????????????????????????????????????,?????????????????????????????????????????????
     *
     * @param applicationName ????????????
     * @param principalNo     ?????????
     * @param type            ???????????????
     * @return ??????????????????
     * @author shulie
     */
    public List<TApplicationInterface> queryWList(String applicationName, String principalNo, String type) {
        return tWListMntDao.queryWList(applicationName, principalNo, type);
    }

    /**
     * ??????: ??????id?????????????????????
     *
     * @param wlistId ?????????id
     * @return ?????????????????????
     * @throws TROModuleException ??????
     * @author shulie
     */
    public TWList querySingleWListById(String wlistId) throws TROModuleException {
        TWList tWlist = tWListMntDao.querySingleWListById(wlistId);
        if (tWlist == null) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_QUERY_WLISTBYID_NOTEXIST);
        }

        return tWlist;
    }

    /**
     * ??????:  ??????id?????????????????????
     *
     * @param param ??????????????????
     * @author shulie
     */
    public void updateWListById(TWList param) throws TROModuleException {
        TWList dbData = tWListMntDao.querySingleWListById(String.valueOf(param.getWlistId()));
        if (dbData == null) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_QUERY_WLISTBYID_NOTEXIST);
        }
        int applicationExist = tWListMntDao.wListExist(param.getApplicationId(), param.getInterfaceName(),
            param.getUseYn());
        //            ?????????,???????????????????????????,??????????????????1????????????
        if (applicationExist > 1) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_UPDATE_WLIST_DUPICATE_EXCEPTION);
        }
        //?????????????????????????????????
        tWListMntDao.updateSelective(param);
        //???????????? nginx??????
        writeWhiteListFile();
    }

    /**
     * ??????: ?????????????????????
     *
     * @param wlistIds ?????????id(??????id???????????????)
     * @author shulie
     */
    public String deleteWListByIds(String wlistIds) {
        GetDeleteIds deleteIds = new GetDeleteIds(wlistIds, "interfaceName").invoke(tWListMntDao);
        List<String> ableDeleteWList = deleteIds.getAbleDeleteList();
        if (!ableDeleteWList.isEmpty()) {
            List<TWList> ableDeleteWLists = tWListMntDao.queryWListByIds(ableDeleteWList);
            tWListMntDao.deleteWListByIds(ableDeleteWList);
            //?????????????????????????????????
            ableDeleteWLists.stream().map(tWList -> tWList.getApplicationId()).distinct().forEach(applicationId -> {
                TApplicationMnt tApplicationMnt = tApplicationMntDao.queryApplicationinfoById(
                    Long.parseLong(applicationId));
            });

        }
        writeWhiteListFile();
        return deleteIds.getResult();
    }

    /**
     * ??????: ??????id?????????????????????
     *
     * @author shulie
     */
    public void deleteWListByIds(List<Long> ids) {
        if (!ids.isEmpty()) {
            List<TWList> ableDeleteWLists = tWListMntDao.getWListByIds(ids);
            tWListMntDao.deleteByIds(ids);

        }
        writeWhiteListFile();
    }

    // =================================== ???????????????=====================================================

    /**
     * ??????: ?????????????????????
     *
     * @param tBList ??????????????????
     * @throws TROModuleException ??????
     * @author shulie
     */
    public void saveBList(TBList tBList) throws TROModuleException {
        if (StringUtils.isBlank(tBList.getUseYn())) {
            tBList.setUseYn("1");
        }
        int applicationExist = tBListMntDao.bListExist(tBList.getRedisKey());
        if (applicationExist > 0) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_BLIST_DUPICATE_EXCEPTION);
        }
        addBList(tBList);
        writeWhiteListFile();
    }

    /**
     * ??????: ???????????????????????????
     *
     * @param tBList ??????????????????
     * @author shulie
     */
    private void addBList(TBList tBList) {
        BlackListCreateParam param = new BlackListCreateParam();
        param.setRedisKey(tBList.getRedisKey());
        param.setUseYn(Integer.parseInt(tBList.getUseYn()));
        param.setCreateTime(new Date());
        param.setUpdateTime(new Date());
        blackListDAO.insert(param);
        User user = RestContext.getUser();
        configSyncService.syncBlockList(user.getKey());
        whiteListFileService.writeWhiteListFile(user.getCustomerId(), user.getCustomerKey());
    }

    /**
     * ??????: ??????id?????????????????????
     *
     * @return ?????????????????????
     * @author shulie
     */
    public TBList querySingleBListById(String blistId) {
        TBList tbList = tBListMntDao.querySingleBListById(blistId);

        return tbList;
    }

    /**
     * ??????: ??????id?????????????????????
     *
     * @param tBList ??????????????????
     * @author shulie
     */
    public void updateBListById(TBList tBList) {
        TBList originBList = tBListMntDao.querySingleBListById(String.valueOf(tBList.getBlistId()));
        tBListMntDao.updateBListById(tBList);
        User user = RestContext.getUser();
        if (user == null) {
            user = troWebUserService.queryUserByKey(RestContext.getTenantUserKey());
        }
        configSyncService.syncBlockList(user.getKey());
        whiteListFileService.writeWhiteListFile(user.getCustomerId(), user.getCustomerKey());
    }

    /**
     * ??????: ???????????????????????????
     *
     * @param blistIds ?????????id(??????id???????????????)
     * @author shulie
     */
    @Deprecated
    public void deleteBListByIds(String blistIds) {
        List<String> blistIdList = Arrays.stream(blistIds.split(",")).filter(StringUtils::isNotEmpty).distinct()
            .collect(Collectors.toList());
        tBListMntDao.deleteBListByIds(blistIdList);
        User user = RestContext.getUser();
        if (user == null) {
            user = troWebUserService.queryUserByKey(RestContext.getTenantUserKey());
        }
        configSyncService.syncBlockList(user.getKey());
        List<TBList> deleteBLists = tBListMntDao.queryBListByIds(blistIdList);
        whiteListFileService.writeWhiteListFile(user.getCustomerId(), user.getCustomerKey());
    }

    public List<TBList> queryBListByIds(List<Long> blistIds) {
        if (CollectionUtils.isEmpty(blistIds)) {
            return Lists.newArrayList();
        }
        List<String> blistIdList = blistIds.stream().map(String::valueOf).collect(Collectors.toList());
        return tBListMntDao.queryBListByIds(blistIdList);
    }

    /**
     * ??????: ?????????????????????
     *
     * @param bListQueryParam redis???key??????????????????
     * @return ?????????????????????
     * @author shulie
     */
    public Response queryBList(BListQueryParam bListQueryParam) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("pageNum", bListQueryParam.getCurrentPage());
        paramMap.put("pageSize", bListQueryParam.getPageSize());
        String redisKey = bListQueryParam.getRedisKey();
        PageHelper.startPage(PageInfo.getPageNum(paramMap), PageInfo.getPageSize(paramMap));
        List<TBList> queryBList = tBListMntDao.queryBList(redisKey, "");
        if (CollectionUtils.isNotEmpty(queryBList)) {
            for (TBList tbList : queryBList) {
                List<Long> allowUpdateUserIdList = RestContext.getUpdateAllowUserIdList();
                if (CollectionUtils.isNotEmpty(allowUpdateUserIdList)) {
                    tbList.setCanEdit(allowUpdateUserIdList.contains(tbList.getUserId()));
                }
                List<Long> allowDeleteUserIdList = RestContext.getDeleteAllowUserIdList();
                if (CollectionUtils.isNotEmpty(allowDeleteUserIdList)) {
                    tbList.setCanRemove(allowDeleteUserIdList.contains(tbList.getUserId()));
                }
                List<Long> allowEnableDisableUserIdList = RestContext.getEnableDisableAllowUserIdList();
                if (CollectionUtils.isNotEmpty(allowEnableDisableUserIdList)) {
                    tbList.setCanEnableDisable(allowEnableDisableUserIdList.contains(tbList.getUserId()));
                }
            }
        }
        PageInfo<TBList> pageInfo = new PageInfo<>(queryBList.isEmpty() ? Lists.newArrayList() : queryBList);
        Response response = Response.success(pageInfo.getList(),
            CollectionUtils.isEmpty(pageInfo.getList()) ? 0 : pageInfo.getTotal());
        return response;
    }

    /**
     * ??????: ??????????????????????????????agent????????????????????????USE_YN=1????????????
     *
     * @return ??????????????????
     * @author shulie
     */
    public Map<String, List<Map<String, Object>>> queryBWList(String appName) throws TROModuleException {

        //??????????????????  ???????????????????????????  ???????????????????????? ???????????????  ???????????????  ??????????????? ???????????????
        String appNameKey = "";
        if (StringUtils.isNotEmpty(appName)) {
            appNameKey = appName;
        }
        List<Map<String, Object>> wLists = tWListMntDao.queryWListList(appName);
        List<Map<String, Object>> bLists = tBListMntDao.queryBListList();
        Map<String, List<Map<String, Object>>> resultMap = Maps.newHashMapWithExpectedSize(30);
        List<Map<String, Object>> wListsResult = new ArrayList();
        if (wLists != null) {
            for (Map<String, Object> whiteItem : wLists) {
                String type = (String)whiteItem.get("TYPE");
                String interfaceName = (String)whiteItem.get("INTERFACE_NAME");
                if ("dubbo".equals(type)) {
                    if (StringUtils.contains(interfaceName, "#")) {
                        interfaceName = StringUtils.substringBefore(interfaceName, "#");
                    }
                }
                Map<String, Object> whiteItemNew = new HashMap<String, Object>();
                whiteItemNew.put("TYPE", type);
                whiteItemNew.put("INTERFACE_NAME", interfaceName);
                wListsResult.add(whiteItemNew);
            }
        }

        resultMap.put("wLists", wListsResult);
        resultMap.put("bLists", bLists);

        return resultMap;
    }

    /**
     * ??????: ????????????????????????
     *
     * @return ??????????????????
     * @author shulie
     */
    public Map<String, List<Map<String, Object>>> queryBWMetricList(String appName) throws TROModuleException {
        List<Map<String, Object>> wLists = null;
        List<Map<String, Object>> bLists = null;
        //??????????????????  ???????????????????????????  ???????????????????????? ???????????????  ???????????????  ??????????????? ???????????????
        String appNameKey = "";
        if (StringUtils.isNotEmpty(appName)) {
            appNameKey = appName;
        }
        Optional<Object> wListValue = redisManager.valueGet(
            WhiteBlackListRedisKey.TRO_WHITE_LIST_KEY_METRIC + appNameKey);
        if (wListValue.isPresent()) {
            wLists = (List<Map<String, Object>>)wListValue.get();
        } else {
            RedisKey wListRedis = new RedisKey(WhiteBlackListRedisKey.TRO_WHITE_LIST_KEY_METRIC + appNameKey,
                WhiteBlackListRedisKey.TIMEOUT);
            wLists = tWListMntDao.queryWListList(appName);
            // ????????????????????????????????????????????????????????????????????????
            //???????????????type=mq?????????????????????mqType????????????ibmmq??????rocketmq
            //????????????????????????????????????
            if (wLists == null || wLists.isEmpty()) {
                throw new TROModuleException(TROErrorEnum.CONFCENTER_QUERY_NOT_WLIST_FOR_APPNAME_EXCEPTION);
            }
            wLists.forEach(map -> {
                String type = (String)map.get("TYPE");
                if ("mq".equals(type)) {
                    String mqType = (String)map.get("MQ_TYPE");
                    //?????????ESB/IBM?????????type?????????ibmmq
                    if (MQConstant.ESB.equals(mqType) || MQConstant.IBM.equals(mqType)) {
                        map.put("TYPE", "ibmmq");
                    } else if (MQConstant.ROCKETMQ.equals(mqType) || MQConstant.DPBOOT_ROCKETMQ.equals(mqType)) {
                        map.put("TYPE", "rocketmq");
                    }
                }
            });
            redisManager.valuePut(wListRedis, wLists);
        }
        Optional<Object> bListValue = redisManager.valueGet(WhiteBlackListRedisKey.TRO_BLACK_LIST_KEY);
        if (bListValue.isPresent()) {
            bLists = (List<Map<String, Object>>)bListValue.get();
        } else {
            RedisKey bListRedis = new RedisKey(WhiteBlackListRedisKey.TRO_BLACK_LIST_KEY,
                WhiteBlackListRedisKey.TIMEOUT);
            bLists = tBListMntDao.queryBListList();
            redisManager.valuePut(bListRedis, bLists);
        }
        Map<String, List<Map<String, Object>>> resultMap = Maps.newHashMapWithExpectedSize(30);

        resultMap.put("wLists", wLists);
        resultMap.put("bLists", bLists);

        return resultMap;
    }

    // =================================== ????????????=====================================================

    /**
     * ??????: ??????????????????
     *
     * @param tLinkServiceMntVo ?????????????????????
     * @throws TROModuleException ??????
     * @author shulie
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveBasicLink(TLinkServiceMntVo tLinkServiceMntVo) throws TROModuleException {
        Map<String, Object> selectLinkId = tLinkMnDao.selectLinkId(tLinkServiceMntVo.getLinkName());
        Long linkId = MapUtils.getLong(selectLinkId, "LINK_ID");
        String valueOf = String.valueOf(linkId == null ? "" : linkId);
        List<TLinkServiceMnt> tLinkServiceMntList = tLinkServiceMntVo.gettLinkServiceMntList();
        List<TLinkServiceMnt> tLinkServiceMntLists = Lists.newArrayList();

        if (StringUtils.isNotEmpty(valueOf)) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_LINK_EXIST);
        }

        //???????????????????????????????????????
        tLinkServiceMntVo.setLinkId(snowflake.next());
        tLinkMnDao.addBasicLink(tLinkServiceMntVo);

        saveRelationLink(tLinkServiceMntVo.getLinkId(), tLinkServiceMntVo.getTechLinks(), "t_bs_tch_link");

        if (tLinkServiceMntList != null && !tLinkServiceMntList.isEmpty()) {
            tLinkServiceMntList.forEach(tLinkServiceMnt -> {
                tLinkServiceMnt.setLinkServiceId(snowflake.next());
                tLinkServiceMnt.setLinkId(tLinkServiceMntVo.getLinkId());
                tLinkServiceMntLists.add(tLinkServiceMnt);
            });

            tLinkMnDao.addLinkInterface(tLinkServiceMntLists);
        }

    }

    /**
     * ??????: ??????????????????
     * ????????????????????????????????????????????????
     *
     * @param tLinkServiceMntVo ?????????????????????
     * @throws TROModuleException ??????
     * @author shulie
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLink(TLinkServiceMntVo tLinkServiceMntVo) throws TROModuleException {
        Map<String, Object> selectLinkId = tLinkMnDao.selectLinkId(tLinkServiceMntVo.getLinkName());
        Long linkId = MapUtils.getLong(selectLinkId, "LINK_ID");
        String valueOf = String.valueOf(linkId == null ? "" : linkId);
        List<TLinkServiceMnt> tLinkServiceMntList = tLinkServiceMntVo.gettLinkServiceMntList();
        List<TLinkServiceMnt> tLinkServiceMntLists = Lists.newArrayList();

        if (StringUtils.isNotEmpty(valueOf)) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_LINK_EXIST);
        }

        //???????????????????????????????????????
        tLinkServiceMntVo.setLinkId(snowflake.next());
        tLinkMnDao.addBasicLink(tLinkServiceMntVo);

        //++ ?????????????????????????????????
        tLinkMnDao.addSecondLinkRef(tLinkServiceMntVo.getSecondLinkId(), String.valueOf(tLinkServiceMntVo.getLinkId()));

        if (tLinkServiceMntList != null && !tLinkServiceMntList.isEmpty()) {
            tLinkServiceMntList.forEach(tLinkServiceMnt -> {
                tLinkServiceMnt.setLinkServiceId(snowflake.next());
                tLinkServiceMnt.setLinkId(tLinkServiceMntVo.getLinkId());
                tLinkServiceMntLists.add(tLinkServiceMnt);
            });
            tLinkMnDao.addLinkInterface(tLinkServiceMntLists);
        }

    }

    /**
     * ??????: ????????????????????????????????????
     *
     * @param tLinkServiceMntVo    ???????????????????????????
     * @param linkId               ????????????id
     * @param tLinkServiceMntList  ????????????????????????
     * @param tLinkServiceMntLists ??????????????????????????????
     * @param linkExist            ??????????????????????????????
     * @throws TROModuleException
     * @author shulie
     */
    private void addParam(TLinkServiceMntVo tLinkServiceMntVo,
        String linkId,
        List<TLinkServiceMnt> tLinkServiceMntList,
        List<TLinkServiceMnt> tLinkServiceMntLists,
        String linkExist) throws TROModuleException {
        if (tLinkServiceMntList.isEmpty()) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_LINK_PARAM_EXCEPTION);
        }

        tLinkServiceMntList.forEach(tLinkServiceMnt -> {
            if (StringUtils.isNotEmpty(linkId)) {
                int linkInterfaceExist = tLinkMnDao.saveLinkInterfaceExist(linkId, tLinkServiceMnt.getInterfaceName());
                if (linkInterfaceExist == 0) {
                    tLinkServiceMnt.setLinkServiceId(snowflake.next());
                    tLinkServiceMnt.setLinkId(Long.valueOf(linkId));
                    tLinkServiceMntLists.add(tLinkServiceMnt);
                }
            } else {
                tLinkServiceMnt.setLinkServiceId(snowflake.next());
                tLinkServiceMnt.setLinkId(tLinkServiceMntVo.getLinkId());
                tLinkServiceMntLists.add(tLinkServiceMnt);
            }
        });

        tLinkServiceMntList.clear();
        tLinkServiceMntVo.settLinkServiceMntList(tLinkServiceMntLists);
    }

    /**
     * ??????: ????????????????????????
     *
     * @param paramMap ??????????????????,????????????,????????????,???????????????,????????????
     *                 pageSize???-1???????????????
     * @return ????????????
     * @author shulie
     */
    public PageInfo<TLinkApplicationInterface> queryBasicLinkList(Map<String, Object> paramMap) {

        //FIXME ???????????????????????????
        boolean paginationFlag = StringUtils.equals("-1", MapUtils.getString(paramMap, "pageSize")) ? false : true;
        Page<TLinkApplicationInterface> newPage = new Page<>();
        if (paginationFlag) {
            newPage = PageHelper.startPage(PageInfo.getPageNum(paramMap), PageInfo.getPageSize(paramMap));
        }
        List<TLinkApplicationInterface> queryBasicLinkList = tLinkMnDao.queryBasicLinkList(paramMap);
        if (CollectionUtils.isEmpty(queryBasicLinkList)) {
            return new PageInfo<>(Lists.newArrayList());
        }
        queryBasicLinkList = queryBasicLinkList.stream()
            //                .filter(tLinkApplicationInterface -> !StringUtils.isAnyEmpty(tLinkApplicationInterface
            //                .getApplicationName(), tLinkApplicationInterface.getInterfaceName()))
            .peek(tLinkApplicationInterface -> {
                List<List<Map<String, Object>>> basicLinkList = getRelationLinkRelationShip("t_bs_tch_link",
                    String.valueOf(tLinkApplicationInterface.getLinkId()));
                tLinkApplicationInterface.setTechLinks(JSON.toJSONString(basicLinkList));
            })
            .collect(Collectors.toList());
        newPage.clear();
        newPage.addAll(queryBasicLinkList);

        return new PageInfo<TLinkApplicationInterface>(newPage);
    }

    public List<TLinkServiceMntVo> queryBasicLinkListDownload(Map<String, Object> paramMap) {

        List<TLinkServiceMntVo> tLinkServiceMntVos = tLinkMnDao.queryBasicLinkListDownload(paramMap);
        for (TLinkServiceMntVo tLinkServiceMntVo : tLinkServiceMntVos) {
            List<TLinkServiceMnt> tLinkServiceMnts = tLinkMntDao.queryLinkInterface(
                String.valueOf(tLinkServiceMntVo.getLinkId()));
            tLinkServiceMntVo.settLinkServiceMntList(tLinkServiceMnts);
        }

        return tLinkServiceMntVos;
    }

    /**
     * linkName??????APPID
     *
     * @param obj
     * @return
     */
    public String queryAppIdByAppName(Object obj) {

        if (obj == null) {
            return "1";

        }
        String str = String.valueOf(obj);
        String linkName = secondLinkDao.queryAppIdByAppName(str);
        return String.valueOf(linkName);

    }

    /**
     * ??????: ????????????id????????????????????????
     *
     * @param linkId ??????id
     * @return ????????????????????????
     * @author shulie
     */
    public TLinkServiceMntVo queryLinkByLinkId(String linkId) {
        TLinkServiceMntVo tLinkServiceMntVo = tLinkMnDao.queryLinkByLinkId(linkId);
        //????????????
        setTechLinks(tLinkServiceMntVo);
        //????????????
        setNodes(tLinkServiceMntVo);

        return tLinkServiceMntVo;
    }

    private void setTechLinks(TLinkServiceMntVo tLinkServiceMntVo) {
        if (StringUtils.isNotBlank(tLinkServiceMntVo.getTechLinks())) {
            List<List<String>> techLinks = JSON.parseObject(tLinkServiceMntVo.getTechLinks(),new TypeReference<List<List<String>>>(){}); // Json ???List

            List<List<Map<String, Object>>> showTechLinks = Lists.newArrayListWithExpectedSize(techLinks.size());
            techLinks.forEach(lstString -> {
                List<Map<String, Object>> tempObj = tLinkMntDao.transferBusinessLinkNameAndId(lstString);
                ;
                showTechLinks.add(tempObj);
            });
            tLinkServiceMntVo.setTechLinksList(showTechLinks);
        }
    }

    private void setNodes(TLinkServiceMntVo tLinkServiceMntVo) {
        List<TLinkNodesVo> tLinkNodesVoList = tLinkMntDao.getNodesByBlinkId(
            String.valueOf(tLinkServiceMntVo.getLinkId()));
        if (CollectionUtils.isNotEmpty(tLinkNodesVoList)) {
            Map<String, Object> linkNode = Maps.newHashMapWithExpectedSize(tLinkNodesVoList.size());
            Integer maxBlank = tLinkNodesVoList.stream().mapToInt(tempNode -> tempNode.gettLinkBank()).max().getAsInt();
            //??????
            List<Map<String, Object>> nodeList = Lists.newArrayListWithExpectedSize(tLinkNodesVoList.size() + 1);
            //?????????
            List<Map<String, Object>> linksList = Lists.newArrayListWithExpectedSize(tLinkNodesVoList.size() + 1);
            Integer parentKey = 10001;
            Map<String, Object> parentNode = Maps.newHashMapWithExpectedSize(6);
            parentNode.put("bank", "0");
            parentNode.put("x", "1");
            parentNode.put("y",
                maxBlank % 2 == 0 ? String.valueOf(maxBlank / 2 + 0.5) : String.valueOf(maxBlank / 2 + 1));
            parentNode.put("text", tLinkServiceMntVo.getLinkName());
            parentNode.put("baseLinkId", String.valueOf(tLinkServiceMntVo.getLinkId()));
            parentNode.put("key", String.valueOf(parentKey));

            nodeList.add(parentNode);
            tLinkNodesVoList.stream().forEach(nodeVo -> {
                Map<String, Object> tempLink = Maps.newHashMapWithExpectedSize(4);
                if (1 == nodeVo.gettLinkOrder().intValue()) {
                    tempLink.put("from", String.valueOf(parentKey));
                    tempLink.put("source", tLinkServiceMntVo.getLinkName());
                } else {
                    tempLink.put("from",
                        String.valueOf(parentKey + nodeVo.gettLinkBank() * 10000 + nodeVo.gettLinkOrder() - 1));
                    tempLink.put("source", tLinkNodesVoList.stream().filter(
                        s -> s.gettLinkBank().equals(nodeVo.gettLinkBank()) && s.gettLinkOrder()
                            .equals(nodeVo.gettLinkOrder() - 1)).findFirst().get().getLinkName());
                }
                tempLink.put("to", String.valueOf(parentKey + nodeVo.gettLinkBank() * 10000 + nodeVo.gettLinkOrder()));
                tempLink.put("target", nodeVo.getLinkName());
                linksList.add(tempLink);
                Map<String, Object> childNode = Maps.newHashMapWithExpectedSize(6);
                childNode.put("bank", String.valueOf(nodeVo.gettLinkBank()));
                childNode.put("x", String.valueOf(1 + nodeVo.gettLinkOrder()));
                childNode.put("y", String.valueOf(nodeVo.gettLinkBank()));
                childNode.put("text", nodeVo.getLinkName());
                childNode.put("techLinkId", String.valueOf(nodeVo.gettLinkId()));
                childNode.put("key",
                    String.valueOf(parentKey + nodeVo.gettLinkBank() * 10000 + nodeVo.gettLinkOrder()));
                nodeList.add(childNode);
            });
            tLinkServiceMntVo.setLinks(linksList);
            tLinkServiceMntVo.setNodes(nodeList);
        }
    }

    /**
     * ??????: ????????????????????????
     *
     * @author shulie
     */
    @Transactional(rollbackFor = Exception.class)
    public String deleteLinkByLinkIds(String basicLinkIds) {
        GetDeleteIds deleteIds = new GetDeleteIds(basicLinkIds, "basicLinkName").invoke(tLinkMnDao);
        List<String> ableDeleteBasicLink = deleteIds.getAbleDeleteList();
        if (!ableDeleteBasicLink.isEmpty()) {
            tLinkMnDao.deleteLinkByLinkIds(ableDeleteBasicLink);
            // ?????????????????????????????????????????????
            tLinkMnDao.deleteBTLinkRelationShip(ableDeleteBasicLink);
            tLinkMnDao.deleteLinkInterfaceByLinkIds(ableDeleteBasicLink);
            //???????????????????????????/??????????????????
            tLinkMnDao.deleteSecondBasicLinkRef(Splitter.on(",").trimResults().omitEmptyStrings()
                .splitToList(basicLinkIds));
            List<TLinkServiceMntVo> tLinkServiceMntVos = tLinkMnDao.queryLinksByLinkIds(ableDeleteBasicLink);

        }
        return deleteIds.getResult();
    }

    /**
     * ??????: ??????????????????????????????
     * query/bwlistmetric
     *
     * @param linkServiceIds ?????????????????????id
     * @author shulie
     */
    public void deleteLinkInterfaceByLinkServiceId(String linkServiceIds) {
        List<String> linkServiceIdsList = Arrays.stream(linkServiceIds.split(",")).filter(
            e -> StringUtils.isNotEmpty(e)).distinct().collect(Collectors.toList());
        List<TLinkServiceMntVo> tLinkServiceMntVos = tLinkMnDao.queryLinksByLinkIds(linkServiceIdsList);

        tLinkMnDao.deleteLinkInterfaceByLinkServiceId(linkServiceIdsList);
    }

    /**
     * ??????: ??????id????????????????????????
     *
     * @param tLinkServiceMntVo ???????????????????????????
     * @throws TROModuleException
     * @author shulie
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLinkinfo(TLinkServiceMntVo tLinkServiceMntVo) throws TROModuleException {

        if (StringUtils.isNotEmpty(tLinkServiceMntVo.getLinkServiceIds())) {
            deleteLinkInterfaceByLinkServiceId(tLinkServiceMntVo.getLinkServiceIds());
        }

        long linkId = tLinkServiceMntVo.getLinkId();
        int linkExist = tLinkMnDao.updateLinkExist(String.valueOf(linkId));
        // ???????????????????????????
        List<TLinkServiceMnt> tLinkServiceMntList = tLinkServiceMntVo.gettLinkServiceMntList();
        List<TLinkServiceMnt> saveServiceMntLists = Lists.newArrayList();
        List<TLinkServiceMnt> updateServiceMntLists = Lists.newArrayList();
        if (linkExist > 0) {
            //??????
            if (tLinkServiceMntList != null && !tLinkServiceMntList.isEmpty()) {
                tLinkServiceMntList.forEach(tLinkServiceMnt -> {
                    int linkInterfaceExist = tLinkMnDao.updateLinkInterfaceExist(String.valueOf(linkId),
                        String.valueOf(tLinkServiceMnt.getLinkServiceId()));
                    if (linkInterfaceExist == 0) {
                        tLinkServiceMnt.setLinkServiceId(snowflake.next());
                        tLinkServiceMnt.setLinkId(linkId);
                        saveServiceMntLists.add(tLinkServiceMnt);

                    } else {
                        //??????
                        updateServiceMntLists.add(tLinkServiceMnt);
                    }
                });
            }
            tLinkMnDao.updateLink(tLinkServiceMntVo);

            // ==============================Start ??????????????????????????????????????? Start==============================
            // ???????????????????????????????????????
            // ?????????????????????????????????
            tLinkMnDao.deleteReLationShipByTLinkId(Collections.singletonList(String.valueOf(linkId)));
            saveRelationLink(linkId, tLinkServiceMntVo.getTechLinks(), "t_bs_tch_link");
            // ==============================End ??????????????????????????????????????? End==============================

            if (!saveServiceMntLists.isEmpty()) {
                //?????????????????????????????????
                tLinkMnDao.addLinkInterface(saveServiceMntLists);

            }

            if (!updateServiceMntLists.isEmpty()) {

                for (TLinkServiceMnt mnt : updateServiceMntLists) {
                    List<TLinkServiceMnt> updateServiceMntLists2 = Lists.newArrayList();
                    updateServiceMntLists2.add(mnt);
                    LOGGER.info("update object:" + org.apache.commons.lang3.builder.ToStringBuilder
                        .reflectionToString(mnt, org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE));

                    tLinkMnDao.updateLinkInterface(updateServiceMntLists2);
                }

            }//??????????????????????????????
        } else {
            //?????????????????????????????????????????????
            tLinkServiceMntVo.setLinkId(snowflake.next());
            tLinkMnDao.addBasicLink(tLinkServiceMntVo);

            addParam(tLinkServiceMntVo, String.valueOf(linkId), tLinkServiceMntList, saveServiceMntLists,
                String.valueOf(linkExist));
            tLinkMnDao.addLinkInterface(saveServiceMntLists);

        }
    }

    /**
     * ??????:
     * ??????:??????id????????????????????????
     *
     * @param tLinkServiceMntVo ???????????????????????????
     * @throws TROModuleException
     * @author shulie
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLinkInfo(TLinkServiceMntVo tLinkServiceMntVo) throws TROModuleException {
        //???????????????????????????,?????????????????????
        if (StringUtils.isNotEmpty(tLinkServiceMntVo.getLinkServiceIds())) {
            deleteLinkInterfaceByLinkServiceId(tLinkServiceMntVo.getLinkServiceIds());
        }

        long linkId = tLinkServiceMntVo.getLinkId();
        int linkExist = tLinkMnDao.updateLinkExist(String.valueOf(linkId));
        // ???????????????????????????
        List<TLinkServiceMnt> tLinkServiceMntList = tLinkServiceMntVo.gettLinkServiceMntList();
        List<TLinkServiceMnt> saveServiceMntLists = Lists.newArrayList();
        List<TLinkServiceMnt> updateServiceMntLists = Lists.newArrayList();
        if (linkExist > 0) {
            //??????
            if (tLinkServiceMntList != null && !tLinkServiceMntList.isEmpty()) {
                tLinkServiceMntList.forEach(tLinkServiceMnt -> {
                    int linkInterfaceExist = tLinkMnDao.updateLinkInterfaceExist(String.valueOf(linkId),
                        String.valueOf(tLinkServiceMnt.getLinkServiceId()));
                    if (linkInterfaceExist == 0) {
                        tLinkServiceMnt.setLinkServiceId(snowflake.next());
                        tLinkServiceMnt.setLinkId(linkId);
                        saveServiceMntLists.add(tLinkServiceMnt);

                    } else {
                        //??????
                        updateServiceMntLists.add(tLinkServiceMnt);
                    }
                });
            }
            tLinkMnDao.updateLink(tLinkServiceMntVo);
            //++ ????????????????????????????????????
            //???????????????????????????(????????????????????????)?????????,
            int existSecondLinkRef = tLinkMnDao.existSecondLinkRef(tLinkServiceMntVo.getSecondLinkId(),
                String.valueOf(tLinkServiceMntVo.getLinkId()));
            if (existSecondLinkRef > 0) {
                tLinkMnDao.updateSecondLinkRef(tLinkServiceMntVo.getSecondLinkId(),
                    String.valueOf(tLinkServiceMntVo.getLinkId()));
            } else {
                tLinkMnDao.addSecondLinkRef(tLinkServiceMntVo.getSecondLinkId(),
                    String.valueOf(tLinkServiceMntVo.getLinkId()));
            }

            if (!saveServiceMntLists.isEmpty()) {
                //?????????????????????????????????
                tLinkMnDao.addLinkInterface(saveServiceMntLists);

            }

            if (!updateServiceMntLists.isEmpty()) {

                for (TLinkServiceMnt mnt : updateServiceMntLists) {
                    List<TLinkServiceMnt> updateServiceMntLists2 = Lists.newArrayList();
                    updateServiceMntLists2.add(mnt);
                    LOGGER.info("update object:" + org.apache.commons.lang3.builder.ToStringBuilder
                        .reflectionToString(mnt, org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE));

                    tLinkMnDao.updateLinkInterface(updateServiceMntLists2);
                }

            }//??????????????????????????????
        } else {
            //?????????????????????????????????????????????
            Map<String, Object> selectLinkId = tLinkMnDao.selectLinkId(tLinkServiceMntVo.getLinkName());
            Long saveLinkId = MapUtils.getLong(selectLinkId, "LINK_ID");
            String valueOf = String.valueOf(saveLinkId == null ? "" : linkId);
            if (StringUtils.isNotEmpty(valueOf)) {
                throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_LINK_EXIST);
            }
            tLinkServiceMntVo.setLinkId(snowflake.next());
            saveLink(tLinkServiceMntVo);
            tLinkMnDao.addBasicLink(tLinkServiceMntVo);

            addParam(tLinkServiceMntVo, String.valueOf(linkId), tLinkServiceMntList, saveServiceMntLists,
                String.valueOf(linkExist));
            tLinkMnDao.addLinkInterface(saveServiceMntLists);

        }
    }

    /**
     * ??????: query application - ip by application name and type
     *
     * @param applicationName ????????????
     * @return ????????????
     * @author shulie
     */
    public List<TApplicationIp> queryApplicationIpByNameTypeList(String applicationName, String type) {
        return tApplicationIpDao.queryApplicationIpByNameTypeList(applicationName, type);
    }

    /**
     * ??????: query application - ip by application name and type
     *
     * @param applicationName ????????????
     * @return ????????????
     * @author shulie
     */
    public List<TApplicationIp> queryApplicationIpByNameList(String applicationName) {
        List<TApplicationIp> tApplicationIps = tApplicationIpDao.queryApplicationIpByNameList(applicationName);
        return tApplicationIps;
    }

    /**
     * ??????: ??????????????????
     *
     * @param tAlarm ???????????????
     * @author shulie
     */
    public void addTAlarm(TAlarm tAlarm) throws TROModuleException {

        List<TApplicationIp> tApplicationIps = tApplicationIpDao.queryApplicationIpByIpList(tAlarm.getIp());
        if (CollectionUtils.isEmpty(tApplicationIps)) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_IP_NOTEXISTAPP_EXCEPTION);
        }

        for (TApplicationIp tApplicationIp : tApplicationIps) {
            tAlarm.setId(null);
            tAlarm.setWarPackages(tApplicationIp.getApplicationName());
            Result<Void> add = tAlarmService.add(tAlarm);

            if (!add.isSuccess()) {
                TROModuleException troModuleException = new TROModuleException(add.getMessage());

                throw troModuleException;
            }
        }
    }

    public List<TLinkServiceMntVo> queryTLinkMntsByIds(List<String> ids) {
        return tLinkMnDao.queryLinksByLinkIds(ids);
    }

    /**
     * ??????:  ??????appname??????????????????????????????
     *
     * @author shulie
     * @date 2019/3/1 14:48
     */
    public PageInfo<?> queryWListByAppName(Map<String, Object> paramMap) {
        String type = MapUtils.getString(paramMap, "type");
        String interfaceName = MapUtils.getString(paramMap, "interfaceName");
        String applicationName = MapUtils.getString(paramMap, "applicationName");
        String pageNum = MapUtils.getString(paramMap, "pageNum");
        String pageSize = MapUtils.getString(paramMap, "pageSize");
        // ?????????????????????
        if ("1".equals(type) && StringUtils.isEmpty(interfaceName)) {

            RedisTemplate redisTemplate = redisManager.getRedisTemplate();
            String pradaSynchronizedToRedisStatus = (String)redisTemplate.opsForValue().get("pradaSynchronizedToRedis");
            if ("Success".equals(pradaSynchronizedToRedisStatus)) {
                Long total = redisTemplate.opsForList().size(applicationName);
                int pageNumN = Integer.parseInt(pageNum);
                int pageSizeN = Integer.parseInt(pageSize);
                int start = (pageNumN - 1) * pageSizeN;
                int end = pageNumN * pageSizeN;

                List<String> list = null;
                if (StringUtils.isEmpty(applicationName)) {
                    list = redisTemplate.opsForList().range("allUrlList", start, end - 1);
                } else {
                    list = redisTemplate.opsForList().range(applicationName, start, end - 1);
                }

                List<Object> returnList = Lists.newArrayListWithCapacity(200);
                // ????????????
                list.forEach(url -> {
                    Map<String, String> objectObjectHashMap = Maps.newHashMapWithExpectedSize(1);
                    objectObjectHashMap.put("interfaceName", url);
                    returnList.add(objectObjectHashMap);
                });

                PageInfo<Object> pageInfo = new PageInfo<>(returnList.isEmpty() ? Lists.newArrayList() : returnList);
                pageInfo.setPageNum(pageNumN);
                pageInfo.setPageSize(pageSizeN);
                pageInfo.setTotal(total);
                return pageInfo;
            }
        }

        // ????????????????????????
        //??????
        PageHelper.startPage(PageInfo.getPageNum(paramMap), PageInfo.getPageSize(paramMap));
        switch (type) {
            case "1":
                List<TPradaHttpData> tPradaHttpData = tWListMntDao.queryInterfaceByAppNameByTPHD(applicationName, type,
                    interfaceName);
                return new PageInfo<>(tPradaHttpData.isEmpty() ? Lists.newArrayList() : tPradaHttpData);
            case "2":
                List<TUploadInterfaceDataVo> dubboData = tWListMntDao.queryInterfaceByAppNameFromTUID(applicationName,
                    type, interfaceName);
                return new PageInfo<>(dubboData.isEmpty() ? Lists.newArrayList() : dubboData);
            case "4":
                List<TUploadInterfaceDataVo> jobData = tWListMntDao.queryInterfaceByAppNameFromTUID(applicationName,
                    type, interfaceName);
                return new PageInfo<>(jobData.isEmpty() ? Lists.newArrayList() : jobData);
            default:
                break;
        }
        return new PageInfo(Lists.newArrayList());
    }

    /**
     * ??????: ?????????????????????
     *
     * @param list ??????????????????
     * @return ?????????????????????????????????
     * @author shulie
     * @date 2019/3/1 15:11
     */
    public List<String> filterWList(String appName, List<String> list) {

        //??????ip  /vas-cas-web/login;JSESSIONID=    NVAS-vas-cas-web
        //http://vip.pamirs.com:8080/vas-cas-web/login;JSESSIONID= NVAS-vas-cas-web
        //http://cubc.pamirs.com/cubc/codaccountmanage/queryModifyParamsById ???????????????  cubc
        //http://dpjjwms.pamirs.com/dpjjwms/ ??????.cache.html wms-dpjjwms
        List<String> filterList = Lists.newArrayListWithCapacity(4);
        if (!WListRuleEnum.getAppNameList().contains(appName)) {
            return list;
        }

        List<String> numberDomainList = Lists.newArrayListWithCapacity(200);
        List<String> domainList = Lists.newArrayListWithCapacity(200);
        List<String> cubcList = Lists.newArrayListWithCapacity(200);
        List<String> wms_dpjjwms_List = Lists.newArrayListWithCapacity(200);
        for (String infas : list) {
            if (infas.contains(WListRuleEnum.CUBC.getRule()) && NumberUtils.isDigits(
                StringUtils.substringAfterLast(infas, "/"))) {
                cubcList.add(infas);
                continue;
            }
            if (infas.contains(WListRuleEnum.WMS_DPJJWMS.getRule()) && infas.endsWith(".cache.html")) {
                wms_dpjjwms_List.add(infas);
                continue;
            }
            String domain = StringUtils.substringBetween(infas, "//", "/");
            String domainNumber = domain.replaceAll("\\.", "")
                .replaceAll(":", "");
            boolean numberDomain = NumberUtils.isDigits(domainNumber);
            if (numberDomain && infas.contains(WListRuleEnum.NVAS_VAS_CAS_WEB.getRule())) {
                numberDomainList.add(infas);
            }

            if (!numberDomain && infas.contains(WListRuleEnum.VIP_NVAS_VAS_CAS_WEB.getRule())) {
                domainList.add(infas);
            }
        }

        if (CollectionUtils.isNotEmpty(numberDomainList)) {
            filterList.add(numberDomainList.get(0));
        }
        if (CollectionUtils.isNotEmpty(domainList)) {
            filterList.add(domainList.get(0));
        }
        if (CollectionUtils.isNotEmpty(cubcList)) {
            filterList.add(cubcList.get(0));
        }
        if (CollectionUtils.isNotEmpty(wms_dpjjwms_List)) {
            filterList.add(wms_dpjjwms_List.get(0));
        }
        return filterList;
    }

    /**
     * ??????: ????????????????????????(prada??????????????????)
     *
     * @return ????????????
     * @author shulie
     * @date 2019/3/7 9:20
     */
    public List<?> queryAppNameList() {

        List<Object> returnList = Lists.newArrayListWithCapacity(200);
        List<String> list = tApplicationMntDao.queryAppNameList();
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }
        list.sort(String::compareTo);
        list.forEach(appName -> {
                Map<String, Object> map = Maps.newHashMapWithExpectedSize(1);
                map.put("key", appName);
                returnList.add(map);
            }
        );
        return returnList;
    }

    /**
     * ??????: ????????????????????????(???pradar????????????)
     *
     * @author shulie
     * @date 2019/3/7 20:57
     */
    public List<String> queryAppNameFromPradar() throws TROModuleException {
        Map<String, Object> pradarInitParams = RequestPradarUtil.initPrada();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<Map<String, Object>>(pradarInitParams, headers);
        Map<String, Object> responseMap = restTemplate.postForObject(
            getPradarUrl() + RequestPradarUtil.PRADA_APPNAME_URL, httpEntity, Map.class);
        boolean resultFlag = MapUtils.getBoolean(responseMap, "resultFlag");
        if (!resultFlag) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_QUERY_APPNAMEFROMPRADAR_EXCEPTION);
        }
        Object data = responseMap.get("data");
        List<String> appNameList = Lists.newArrayListWithCapacity(300);
        if (data instanceof List) {
            appNameList = (List<String>)data;
        }

        appNameList.sort(String::compareToIgnoreCase);
        return appNameList;
    }

    /**
     * ?????????????????????
     *
     * @param applicationId ??????ID
     * @return
     */
    public List<Map<String, Object>> queryWListByAppId(String applicationId) {
        return tWListMntDao.queryWListByAppId(applicationId);

    }

    /**
     * ????????????????????????
     * ??????????????????@link LinkManageExcelVo
     *
     * @param files
     * @return
     * @throws IOException
     * @throws TROModuleException
     */
    public void batchUploadLinkList(MultipartFile[] files) throws IOException, TROModuleException {
        for (MultipartFile file : files) {
            //??????????????????Sheet????????????????????????
            Sheet sheet = new Sheet(1, 1);
            InputStream stream = new BufferedInputStream(file.getInputStream());
            List<Object> lists = EasyExcelFactory.read(stream, sheet);

            List<TLinkServiceMntVo> addList = new ArrayList<>();
            List<TLinkServiceMntVo> updateList = new ArrayList<>();

            int index = 0;
            for (Object list : lists) {
                ArrayList inVo = (ArrayList)list;
                TLinkServiceMntVo vo = new TLinkServiceMntVo();
                vo.setAswanId(String.valueOf(inVo.get(0)));
                List<JSONObject> mnts = JSONObject.parseObject((String)inVo.get(1), List.class);
                List<TLinkServiceMnt> TLinkServiceMnts = new ArrayList<>(mnts.size());
                StringBuffer sb = new StringBuffer();
                mnts.stream().forEach(mnt -> {
                    TLinkServiceMnt nt = new TLinkServiceMnt();
                    nt.setInterfaceName(String.valueOf(mnt.get("????????????")));
                    nt.setInterfaceDesc(String.valueOf(mnt.get("????????????")));
                    String linkServiceId = mnt.getString("????????????id");
                    nt.setLinkServiceId(Long.parseLong(linkServiceId));
                    TLinkServiceMnts.add(nt);
                    sb.append(linkServiceId).append(",");

                });

                vo.setLinkServiceIds(sb.toString().substring(0, sb.length() - 1));
                vo.settLinkServiceMntList(TLinkServiceMnts);

                vo.setLinkDesc(String.valueOf(inVo.get(2)));
                vo.setLinkEntrence(String.valueOf(inVo.get(3)));
                vo.setLinkModule("1");
                vo.setLinkName(String.valueOf(inVo.get(4)));
                vo.setLinkRank("1");
                vo.setLinkType("1");
                vo.setPrincipalNo("000000");
                vo.setRt(String.valueOf(inVo.get(6)));
                vo.setRtSa(String.valueOf(inVo.get(7)));
                String secondLinkName = String.valueOf(inVo.get(8));
                if ("null".equalsIgnoreCase(secondLinkName)) {
                    throw new TROModuleException(TROErrorEnum.CONFCENTER_NOT_ALLOW_EMPTY + "??????" + index++ + "?????????");
                }

                String secondlinkId = secondLinkDao.queryAppIdByAppName(secondLinkName);
                vo.setSecondLinkId(secondlinkId);
                vo.setTargetSuccessRate(String.valueOf(inVo.get(9)));
                vo.setTargetTps(String.valueOf(inVo.get(10)));
                vo.setUseYn("??????".equalsIgnoreCase(String.valueOf(inVo.get(11))) ? 1 : 0);
                vo.setVolumeCalcStatus("1");
                //????????????id
                String linkid = ObjectUtils.toString(inVo.get(12));
                //??????tps
                vo.setTps(String.valueOf(inVo.get(13)));

                if (StringUtils.isBlank(linkid)) {
                    addList.add(vo);
                } else {
                    vo.setLinkId(Long.parseLong(linkid));
                    updateList.add(vo);
                }
            }

            if (addList.size() > 0) {
                for (TLinkServiceMntVo vo : addList) {
                    this.saveLink(vo);
                }
            }
            if (updateList.size() > 0) {
                for (TLinkServiceMntVo vo : updateList) {
                    this.updateLinkInfo(vo);
                }
            }
        }
    }

    private String getKey(TRODictTypeEnum enums, Object key) {
        String key1 = String.valueOf(key);
        Map data = this.queryDicList(enums);
        Map dataList = (Map)data.get("dicList");
        Iterator iterator = dataList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            if (StringUtils.equalsIgnoreCase(ObjectUtils.toString(entry.getValue()), key1)) {
                return String.valueOf(entry.getKey());
            }

        }
        return null;

    }

    private String getValue(TRODictTypeEnum enums, Object value) {
        String key = String.valueOf(value);
        Map data = this.queryDicList(enums);
        Map dataList = (Map)data.get("dicList");
        Iterator iterator = dataList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            if (StringUtils.equalsIgnoreCase(ObjectUtils.toString(entry.getValue()), key)) {
                return String.valueOf(entry.getKey());
            }

        }
        return null;

    }

    /**
     * ?????????????????????
     *
     * @param files
     */
    public void batchUploadWList(MultipartFile[] files) throws IOException, TROModuleException {
        //?????????????????????
        List<Map<String, Object>> applicationDataList = this.queryApplicationdata();
        Set applicationSet = new HashSet();
        for (MultipartFile file : files) { //???????????????

            applicationDataList.stream().forEach(map -> {
                applicationSet.add(map.get("applicationName"));
            });

            //??????????????????Sheet????????????????????????
            Sheet sheet = new Sheet(1, 1);
            InputStream stream = new BufferedInputStream(file.getInputStream());

            List<Object> lists = EasyExcelFactory.read(stream, sheet);
            for (Object list : lists) {

                TWListVo vo = new TWListVo();
                ArrayList excelVo = (ArrayList)list;
                String applicationName = String.valueOf(excelVo.get(1));
                if (!applicationSet.contains(applicationName)) {
                    throw new TROModuleException(TROErrorEnum.CONFCENTER_ADD_APPLICATION_NOTEXIST_EXCEPTION);

                }
                vo.setPrincipalNo("000000");
                Long id = tApplicationMntDao.queryIdByApplicationName(String.valueOf(excelVo.get(1)));
                vo.setApplicationId(String.valueOf(id));
                //???????????????????????????
                WListTypeEnum[] wListTypeEnums = WListTypeEnum.values();
                Arrays.asList(wListTypeEnums).forEach(
                    wListTypeEnum -> {
                        if (StringUtils.equalsIgnoreCase(wListTypeEnum.getName()
                            , StringUtils.trim(String.valueOf(excelVo.get(2))))) {
                            vo.setType(wListTypeEnum.getValue());
                        }
                    }
                );
                String httpType = isEmPty(excelVo.get(3)) ? null : String.valueOf(excelVo.get(3));
                vo.setHttpType(httpType);
                String jobInterval = isEmPty(excelVo.get(4)) ? null : String.valueOf(excelVo.get(4));
                vo.setJobInterval(jobInterval);
                String mqType = isEmPty(excelVo.get(5)) ? null : String.valueOf(excelVo.get(5));
                vo.setMqType(mqType);
                String useYn = String.valueOf(excelVo.get(6)).equalsIgnoreCase("??????") ? "1" : "0";
                vo.setUseYn(useYn);
                String interfaceName = isEmPty(excelVo.get(7)) ? null : String.valueOf(excelVo.get(7));
                if (StringUtils.isNotBlank(interfaceName)) {
                    String[] strings = interfaceName.split(",");
                    vo.setList(Arrays.asList(strings));
                }
                vo.setDictType("ca888ed801664c81815d8c4f5b8dff0c");
                //http????????????
                String pageLevel = isEmPty(excelVo.get(8)) ? null : String.valueOf(excelVo.get(8));
                vo.setPageLevel(pageLevel(pageLevel));
                //????????????
                String queueName = isEmPty(excelVo.get(9)) ? null : String.valueOf(excelVo.get(9));
                vo.setQueueName(queueName);

                //?????????id
                String twlist = isEmPty(excelVo.get(10)) ? null : String.valueOf(excelVo.get(10));
                if (twlist != null) {
                    vo.setWlistId(Long.parseLong(twlist));
                }
                if (StringUtils.isBlank(twlist)) {
                    //??????
                    this.saveWList(vo);
                } else {
                    //??????
                    TWList tWList = new TWList();
                    BeanUtils.copyProperties(vo, tWList);
                    tWList.setInterfaceName(interfaceName);
                    tWList.setQueueName(queueName);
                    tWList.setPageLevel(pageLevel);
                    tWList.setUseYn(useYn);
                    tWList.setMqType(mqType);
                    tWList.setHttpType(httpType);
                    tWList.setJobInterval(jobInterval);
                    this.updateWListById(tWList);
                }

            }
        }
    }

    private boolean isEmPty(Object t) {
        return null == t
            || StringUtils.isBlank(ObjectUtils.toString(t))
            || "null".equalsIgnoreCase(ObjectUtils.toString(t));
    }

    private String pageLevel(String pageLevel) {
        if (pageLevel == null) {
            return null;
        }
        pageLevel = pageLevel.trim();
        switch (pageLevel) {
            case "??????????????????":
                return "1";
            case "??????????????????/????????????":
                return "2";
            case "??????????????????":
                return "3";
            default:
                return null;
        }
    }

    public List<TLinkBasicVO> queryLinkByLinkType(Map<String, Object> queryMap) {
        List<Integer> linkTypeList = new ArrayList<>();
        if (queryMap != null && queryMap.get("linkType") != null) {
            String linkTypeString = (String)queryMap.get("linkType");
            if (StringUtils.isNotEmpty(linkTypeString)) {
                linkTypeList = Arrays.asList(linkTypeString.split(",")).stream().map(s -> Integer.parseInt(s.trim()))
                    .collect(Collectors.toList());
            }
        }
        return tLinkMntDao.queryLinksByLinkType(linkTypeList);
    }

    /**
     * ???????????? agentVersion ??????
     *
     * @param appName       ?????????
     * @param agentVersion  ??????agent??????
     * @param pradarVersion Pradar Agent??????
     * @throws TROModuleException
     */
    @Transactional(rollbackFor = Throwable.class)
    public void updateAppAgentVersion(String appName, String agentVersion, String pradarVersion) throws
        TROModuleException {
        if (StringUtils.isEmpty(appName)) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_UPDATE_APPLICATION_AGENT_VERSION_EXCEPTION);
        }
        TApplicationMnt applicationMnt = tApplicationMntDao.queryApplicationInfoByNameAndTenant(appName,
            RestContext.getUser().getCustomerId());
        if (applicationMnt == null) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_QUERY_APPLICATION_EXCEPTION);
        }
        tApplicationMntDao.updateApplicaionAgentVersion(applicationMnt.getApplicationId(), agentVersion, pradarVersion);
    }

    /**
     * ???????????????
     *
     * @return
     */
    public List<Map<String, String>> queryLinkIdName() {
        return tLinkMnDao.queryLinkIdName();
    }

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    public List<Map<String, String>> getWhiteListForLink() {
        return tWListMntDao.getWhiteListForLink();
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    @Deprecated
    public List<Map<String, Object>> queryLinkHeaderList() {
        /**
         * 1,??????????????????????????????????????????
         * 2,????????????????????????
         */
        List<Map<String, Object>> resultList = Lists.newArrayList();
        List<TLinkMntDictoryVo> linkHeaderInfo = tLinkMnDao.queryLinkHeaderInfo();
        Map<String, List<TLinkMntDictoryVo>> collect = new HashMap<>(10);
        /**
         * ?????????????????????;????????????????????????
         */
        for (TLinkMntDictoryVo tLinkMntDictoryVo : linkHeaderInfo) {
            collect.computeIfAbsent(tLinkMntDictoryVo.getName() + ";" + tLinkMntDictoryVo.getOrder(),
                k -> new ArrayList<>()).add(tLinkMntDictoryVo);
        }
        collect.forEach((nameOrder, tLinkMntDictoryVoList) -> {
            Map<String, Object> resultMap = Maps.newHashMap();
            String[] nameOrderArr = nameOrder.split(";");
            resultMap.put("name", nameOrderArr[0]);
            resultMap.put("order", nameOrderArr[1]);
            /**
             * ????????????????????????????????????????????????????????????1???????????????tLinkMntDictoryVoList???size==1,
             *  ????????????????????????
             *  ?????????????????????tLinkMntDictoryVoList.size()???????????????????????????????????????
             */
            String linkName = tLinkMntDictoryVoList.get(0).getLinkName();
            if (StringUtils.isEmpty(linkName)) {
                resultMap.put("count", 0);
            } else {
                resultMap.put("count", tLinkMntDictoryVoList.size());
            }
            //??????calcVolumeLinkList????????????
            List<Map<String, String>> list = tLinkMnDao.queryCalcVolumeLinkList(nameOrderArr[1]);
            resultMap.put("calcVolumeLinkList", list);
            resultList.add(resultMap);
        });
        Collections.sort(resultList, Comparator.comparingInt(o -> Integer.valueOf((String)o.get("order"))));
        return resultList;
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    public List<Map<String, Object>> queryLinkHeaderInfoList() {
        /**
         * 1,??????????????????????????????????????????
         * 2,????????????????????????
         */
        List<Map<String, Object>> resultList = Lists.newArrayList();
        List<TLinkTopologyInfoVo> linkHeaderInfo = tLinkMnDao.queryLinkHeaderInfoList();
        Map<String, List<TLinkTopologyInfoVo>> collect = new HashMap<>(16);
        /**
         * ?????????????????????;????????????????????????
         */
        for (TLinkTopologyInfoVo tLinkTopologyInfoVo : linkHeaderInfo) {
            collect.computeIfAbsent(tLinkTopologyInfoVo.getName() + ";" + tLinkTopologyInfoVo.getOrder(),
                k -> new ArrayList<>()).add(tLinkTopologyInfoVo);
        }
        collect.forEach((nameOrder, tLinkTopologyInfoVoList) -> {
            Map<String, Object> resultMap = Maps.newHashMap();
            String[] nameOrderArr = nameOrder.split(";");
            resultMap.put("name", nameOrderArr[0]);
            resultMap.put("order", nameOrderArr[1]);
            /**
             * ????????????????????????????????????????????????????????????1???????????????tLinkMntDictoryVoList???size==1,
             *  ????????????????????????
             *  ?????????????????????tLinkMntDictoryVoList.size()???????????????????????????????????????
             */
            String linkName = tLinkTopologyInfoVoList.get(0).getLinkName();
            if (StringUtils.isEmpty(linkName)) {
                resultMap.put("count", 0);
            } else {
                resultMap.put("count", tLinkTopologyInfoVoList.size());
            }
            //????????????????????????
            List<Map<String, String>> secondLinkList = Lists.newArrayList();
            Map<String, List<TLinkTopologyInfoVo>> collect1 = new HashMap<>(32);
            for (TLinkTopologyInfoVo tLinkTopologyInfoVo : tLinkTopologyInfoVoList) {
                if (StringUtils.isNotEmpty(tLinkTopologyInfoVo.getSecondLinkId())) {
                    collect1.computeIfAbsent(
                        tLinkTopologyInfoVo.getSecondLinkId() + ";" + tLinkTopologyInfoVo.getSecondLinkName(),
                        k -> new ArrayList<>()).add(tLinkTopologyInfoVo);
                }
            }
            collect1.forEach((secondLinkIdName, secondLinkIdNameList) -> {
                Map<String, String> secondLinkMap = Maps.newHashMap();
                secondLinkMap.put("secondLinkId", secondLinkIdNameList.get(0).getSecondLinkId());
                secondLinkMap.put("secondLinkName", secondLinkIdNameList.get(0).getSecondLinkName());
                secondLinkList.add(secondLinkMap);
            });
            resultMap.put("secondLinkList", secondLinkList);
            //??????calcVolumeLinkList????????????
            List<Map<String, String>> list = tLinkMnDao.queryCalcVolumeLinkListByModule(nameOrderArr[1]);
            resultMap.put("calcVolumeLinkList", list);
            resultList.add(resultMap);
        });
        Collections.sort(resultList, Comparator.comparingInt(o -> Integer.valueOf((String)o.get("order"))));
        return resultList;
    }

    /**
     * ??????: ??????????????????????????????????????????
     *
     * @param linkModule ????????????
     * @return java.util.List<TSecondLinkMnt>
     * @author shulie
     * @create 2019/4/15 17:59
     */
    public List<TSecondLinkMnt> querySecondLinkByModule(String linkModule) {
        return tLinkMnDao.querySecondLinkMapByModule(linkModule);
    }

    /**
     * ??????: ??????????????????id?????????????????????????????????
     *
     * @author shulie
     * @date 2018/7/10 15:35
     * @return ????????????id???????????????????????????????????????
     */
    private class GetDeleteIds<T> {
        private String ids;
        private String disableName;
        private List<String> ableDeleteList;
        private String result;

        public GetDeleteIds(String ids, String disableName) {
            this.ids = ids;
            this.disableName = disableName;
        }

        public List<String> getAbleDeleteList() {
            return ableDeleteList;
        }

        public String getResult() {
            return result;
        }

        public GetDeleteIds invoke(T t) {
            ableDeleteList = Lists.newArrayList();
            StringBuffer sb = new StringBuffer();
            Splitter.on(",").omitEmptyStrings().trimResults().splitToList(ids).stream().distinct().forEach(id -> {
                Map<String, Object> map = null;
                if (t instanceof TApplicationMntDao) {
                    map = tApplicationMntDao.queryApplicationRelationBasicLinkByApplicationId(id);
                } else if (t instanceof TWListMntDao) {
                    map = tWListMntDao.queryWListRelationBasicLinkByWListId(id);
                } else if (t instanceof TLinkMntDao) {
                    map = tLinkMnDao.querySecondLinkRelationBasicLinkByBasicLinkId(id);
                }
                if (Objects.isNull(map)) {
                    ableDeleteList.add(id);
                } else {
                    sb.append(MapUtils.getString(map, disableName)).append(",");
                }
            });
            result = StringUtils.substringBeforeLast(sb.toString(), ",");
            return this;
        }
    }
}
