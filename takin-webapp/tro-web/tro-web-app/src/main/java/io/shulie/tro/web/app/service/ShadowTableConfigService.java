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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import com.github.pagehelper.PageHelper;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import com.pamirs.tro.common.constant.Constants;
import com.pamirs.tro.common.constant.TROErrorEnum;
import com.pamirs.tro.common.exception.TROModuleException;
import com.pamirs.tro.common.util.PageInfo;
import com.pamirs.tro.common.util.RequestPradarUtil;
import com.pamirs.tro.entity.domain.entity.TApplicationMnt;
import com.pamirs.tro.entity.domain.entity.TShadowTableConfig;
import com.pamirs.tro.entity.domain.entity.TShadowTableDataSource;
import com.pamirs.tro.entity.domain.vo.TPradarShadowTable;
import com.pamirs.tro.entity.domain.vo.TPradarShadowTableConfig;
import com.pamirs.tro.entity.domain.vo.TShadowTableConfigVo;
import com.pamirs.tro.entity.domain.vo.TShadowTableDatasourceVo;
import io.shulie.tro.web.app.common.CommonService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class ShadowTableConfigService extends CommonService {

    public static final int BATCH_SIZE = 200;

    /**
     * ????????? pardar ??????  ??????  ???????????????
     *
     * @param appName
     * @return
     */
    public Map<String, Set<String>> agentGetShadowTable(String appName) {
        Map<String, Set<String>> appShadowTableMap = Maps.newHashMapWithExpectedSize(10);
        if (StringUtils.isEmpty(appName)) {
            appShadowTableMap.put("N|N|N", null);
            return appShadowTableMap;
        }
        appName = appName.trim();
        //??? pardar ????????? Y|ip:port|schema  ??????????????? ????????????????????? ?????? ???????????? ????????????????????? ?????????  ??????????????????  N ???????????????
        TApplicationMnt applicationMnt = tApplicationMntDao.queryApplicationinfoByName(appName);
        if (applicationMnt == null) {
            appShadowTableMap.put("N|N|N", null);
            return appShadowTableMap;
        }
        //?????????????????????
        List<TShadowTableDataSource> shadowTableDataSourceList = tShadowTableDataSourceDao
            .queryShadowDataSourceByApplicationId(applicationMnt.getApplicationId());
        if (CollectionUtils.isEmpty(shadowTableDataSourceList)) {
            appShadowTableMap.put("N|N|N", null);
            return appShadowTableMap;
        }
        ;
        //????????????????????????
        List<TShadowTableConfig> shadowTableConfigList = tShadowTableConfigDao
            .queryShawdowTableConfigByShadowDatasourceIdList(
                shadowTableDataSourceList.stream().map(TShadowTableDataSource::getShadowDatasourceId)
                    .collect(Collectors.toList()));
        shadowTableDataSourceList.forEach(shadowTableDataSource -> {
            boolean needShadowTable = false;
            StringBuilder sb = new StringBuilder();
            //??????????????? ????????????????????????????????????
            if (Constants.INTEGER_USE.equals(shadowTableDataSource.getUseShadowTable())) {
                sb.append("Y");
                needShadowTable = true;
            } else {
                sb.append("N");
            }
            sb.append(Constants.SPLIT).append(shadowTableDataSource.getDatabaseIpport())
                .append(Constants.SPLIT).append(shadowTableDataSource.getDatabaseName());
            //?????????????????????  ??????????????????????????? ??????set ?????????????????????
            String shadowTableMapKey = sb.toString();
            appShadowTableMap.put(sb.toString(), Sets.newHashSetWithExpectedSize(1));
            if (needShadowTable) {
                shadowTableConfigList.forEach(shadowTable -> {
                    //?????????????????????
                    if (shadowTable.getShadowDatasourceId().equals(shadowTableDataSource.getShadowDatasourceId())) {
                        if (appShadowTableMap.get(shadowTableMapKey) != null) {
                            appShadowTableMap.get(shadowTableMapKey).add(shadowTable.getShadowTableName());
                        } else {
                            Set<String> shadowTableNameSet = Sets.newHashSetWithExpectedSize(50);
                            shadowTableNameSet.add(shadowTable.getShadowTableName());
                            appShadowTableMap.put(shadowTableMapKey, shadowTableNameSet);
                        }
                    }
                });
            }
        });
        return appShadowTableMap;
    }

    /**
     * ?????????pradar?????? ???????????????
     *
     * @param applicationIdList
     * @throws TROModuleException
     */

    public void getShadowTableFromPradarList(String applicationIdList) throws TROModuleException {
        String[] applicationIds = applicationIdList.split(",");
        for (String applicationId : applicationIds) {
            getShadowTableFromPradar(Long.parseLong(applicationId));
        }
    }

    /**
     * ???pradar ???????????????????????????????????? ?????????
     *
     * @param applicationId
     * @throws TROModuleException
     */
    @Transactional(rollbackFor = Exception.class)
    protected void getShadowTableFromPradar(Long applicationId) throws TROModuleException {
        TApplicationMnt applicationMnt = tApplicationMntDao.queryApplicationinfoById(applicationId);
        if (applicationMnt == null) {

            throw new TROModuleException(TROErrorEnum.CONFCENTER_QUERY_APPLICATION_EXCEPTION);
        }
        //??????  [{"name":"RPS","tables":[{"opType":["insert","delete"],"tableName":"t_bas_courier_schedule_pda"},
        // {"opType":["update","delete"],"tableName":"t_rps_auth_usercity"}],"url":"127.0.0.1:3306"}]
        Map<String, Object> pradarInitParams = RequestPradarUtil.initPrada();
        pradarInitParams.put("appName", applicationMnt.getApplicationName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        pradarInitParams.forEach((s, o) -> {
            params.add(s, o + "");
        });
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);
        Map<String, Object> responseMap = null;
        try {
            responseMap = restTemplate.postForObject(getPradarUrl() + RequestPradarUtil.PRADAR_GET_SHADOWCONFIG_URL,
                httpEntity, Map.class);
        } catch (Exception e) {
            LOGGER.error("??????pradar??????????????????{}", e);
            return;
        }
        String resultFlag = MapUtils.getString(responseMap, "resultFlag");
        String resultMsg = MapUtils.getString(responseMap, "resultMsg");
        if ("false".equals(resultFlag)) {
            LOGGER.error("ConfCenterService.getShadowTableFromPradar  " + resultMsg);
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_GET_FROM_PRADAR_EXCEPTION);
        }

        String json = JSON.toJSONString(responseMap.get("data"));
        List<TPradarShadowTableConfig> shadowTableConfigList = JSON.parseObject(json, new TypeReference<List<TPradarShadowTableConfig>>(){});

        if (CollectionUtils.isEmpty(shadowTableConfigList)) {
            return;
        }
        List<TShadowTableDataSource> shadowDataSourceList = tShadowTableDataSourceDao
            .queryShadowDataSourceByApplicationId(applicationId);
        Map<String, TShadowTableDataSource> shadowDatasourceMap = Maps.newHashMapWithExpectedSize(
            shadowTableConfigList.size());
        List<TShadowTableDataSource> needSaveList = Lists.newArrayListWithExpectedSize(shadowTableConfigList.size());

        //?????? ???????????????????????? ?????????  ??????map????????? ????????? ???????????????????????????
        getNeedSaveShadowDatasource(applicationId, shadowTableConfigList, shadowDataSourceList, shadowDatasourceMap,
            needSaveList);
        if (CollectionUtils.isNotEmpty(needSaveList)) {
            tShadowTableDataSourceDao.insertList(needSaveList);
        }

        for (TPradarShadowTableConfig tableConfig : shadowTableConfigList) {
            //???????????? ?????????ip???????????????  ???????????????
            if (StringUtils.isEmpty(tableConfig.getName()) || StringUtils.isEmpty(tableConfig.getName())) {
                continue;
            }
            if (CollectionUtils.isEmpty(tableConfig.getTables())) {
                continue;
            }
            TShadowTableDataSource shadowTableDataSource = shadowDatasourceMap.get(
                tableConfig.getUrl() + tableConfig.getName().toUpperCase());
            //????????????
            if (tableConfig.getTables().size() > BATCH_SIZE) {
                int i = 1;
                boolean loop = true;
                do {
                    List<TPradarShadowTable> subList = null;
                    //????????????  ??????SQL?????? SQL ?????????
                    if (tableConfig.getTables().size() > i * BATCH_SIZE) {
                        subList = tableConfig.getTables().subList((i - 1) * BATCH_SIZE, i * BATCH_SIZE);
                    } else {
                        subList = tableConfig.getTables().subList((i - 1) * BATCH_SIZE, tableConfig.getTables().size());
                        loop = false;
                    }
                    i++;
                    saveShadowTableConfig(shadowTableDataSource, subList);
                } while (loop);
            } else {
                saveShadowTableConfig(shadowTableDataSource, tableConfig.getTables());
            }
        }

    }

    private void getNeedSaveShadowDatasource(Long applicationId, List<TPradarShadowTableConfig> shadowTableConfigList,
        List<TShadowTableDataSource> shadowDataSourceList, Map<String, TShadowTableDataSource> shadowDatasourceMap,
        List<TShadowTableDataSource> needSaveList) {
        if (CollectionUtils.isNotEmpty(shadowDataSourceList)) {
            for (TPradarShadowTableConfig tableConfig : shadowTableConfigList) {
                boolean pradarDatasourceExist = false;
                for (TShadowTableDataSource dataSource : shadowDataSourceList) {
                    if (dataSource.getDatabaseIpport().equalsIgnoreCase(tableConfig.getUrl()) && dataSource
                        .getDatabaseName().equalsIgnoreCase(tableConfig.getName())) {
                        pradarDatasourceExist = true;
                        shadowDatasourceMap.put(dataSource.getDatabaseIpport() + dataSource.getDatabaseName(),
                            dataSource);
                        break;
                    }
                }
                if (!pradarDatasourceExist) {
                    TShadowTableDataSource tShadowTableDataSource = new TShadowTableDataSource();
                    tShadowTableDataSource.setShadowDatasourceId(snowflake.next());
                    tShadowTableDataSource.setDatabaseIpport(tableConfig.getUrl());
                    tShadowTableDataSource.setDatabaseName(tableConfig.getName().toUpperCase());
                    tShadowTableDataSource.setApplicationId(applicationId);
                    //?????????????????? ????????????????????????
                    tShadowTableDataSource.setUseShadowTable(Constants.INTEGER_NOT_USE);
                    shadowDatasourceMap.put(
                        tShadowTableDataSource.getDatabaseIpport() + tShadowTableDataSource.getDatabaseName()
                            .toUpperCase(), tShadowTableDataSource);
                    needSaveList.add(tShadowTableDataSource);
                }
            }
        } else {
            for (TPradarShadowTableConfig tableConfig : shadowTableConfigList) {
                TShadowTableDataSource tShadowTableDataSource = new TShadowTableDataSource();
                tShadowTableDataSource.setShadowDatasourceId(snowflake.next());
                tShadowTableDataSource.setDatabaseIpport(tableConfig.getUrl());
                tShadowTableDataSource.setDatabaseName(tableConfig.getName().toUpperCase());
                tShadowTableDataSource.setApplicationId(applicationId);
                //?????????????????? ????????????????????????
                tShadowTableDataSource.setUseShadowTable(Constants.INTEGER_NOT_USE);
                shadowDatasourceMap.put(
                    tShadowTableDataSource.getDatabaseIpport() + tShadowTableDataSource.getDatabaseName().toUpperCase(),
                    tShadowTableDataSource);
                needSaveList.add(tShadowTableDataSource);
            }
        }
    }

    private void saveShadowTableConfig(TShadowTableDataSource shadowTableDataSource,
        List<TPradarShadowTable> shadowTables) {
        List<String> tableNameList = Lists.newArrayListWithExpectedSize(shadowTables.size());
        shadowTables.forEach(table -> {
                tableNameList.add(table.getTableName().toUpperCase());
            }
        );
        //??????????????????
        List<TShadowTableConfig> existTableConfigs = tShadowTableConfigDao.queryShadowTableByDatasourceId(
            shadowTableDataSource.getShadowDatasourceId(), tableNameList);
        List<TShadowTableConfig> saveList = new ArrayList<>();
        //???????????? ????????????
        shadowTables.forEach(table -> {
            boolean exist = false;
            for (TShadowTableConfig existTable : existTableConfigs) {
                if (existTable.getShadowTableName().equalsIgnoreCase(table.getTableName())) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                TShadowTableConfig saveTable = new TShadowTableConfig(snowflake.next(),
                    shadowTableDataSource.getShadowDatasourceId(), table.getTableName().toUpperCase(),
                    Constants.INTEGER_NOT_USE);
                //?????? ??? ?????????  ?????? ??????????????????
                for (String opType : table.getOpType()) {
                    if (Constants.SQL_DDL_OPERTYPE.contains(opType)) {
                        saveTable.setEnableStatus(Constants.INTEGER_USE);
                        break;
                    }
                }
                saveList.add(saveTable);
            }
        });
        //??????????????? ?????? ???????????????
        if (CollectionUtils.isNotEmpty(saveList)) {
            tShadowTableConfigDao.insertList(saveList);
        }
    }

    public PageInfo<TShadowTableConfigVo> queryShadowTableConfigPage(Map<String, Object> paramMap) {
        PageHelper.startPage(PageInfo.getPageNum(paramMap), PageInfo.getPageSize(paramMap));
        List<TShadowTableConfigVo> tableConfigVoList = tShadowTableConfigDao.queryShadowTableConfigPage(paramMap);
        return new PageInfo<>(CollectionUtils.isEmpty(tableConfigVoList) ? Lists.newArrayList() : tableConfigVoList);
    }

    /**
     * ?????? ???????????????
     *
     * @param shadowTableConfigVo
     * @throws TROModuleException
     */
    public void updateShadowTableConfig(TShadowTableConfigVo shadowTableConfigVo) throws TROModuleException {
        if (StringUtils.isEmpty(shadowTableConfigVo.getId()) || StringUtils.isEmpty(
            shadowTableConfigVo.getShadowTableName())
            || shadowTableConfigVo.getEnableStatus() == null || StringUtils.isEmpty(
            shadowTableConfigVo.getShadowDatasourceId())) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_UPDATE_PARAM_EXCEPTION);
        }
        if (shadowTableConfigVo.getShadowTableName().length() > 127) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_TABLENAME_LONG_EXCEPTION);
        }
        TShadowTableConfig queryShadowTableConfig = tShadowTableConfigDao.selectByPrimaryKey(
            Long.parseLong(shadowTableConfigVo.getId()));
        if (queryShadowTableConfig == null) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_QUERY_ID_EXCEPTION);
        }
        TShadowTableDataSource queryShadowDatasource = tShadowTableDataSourceDao.selectByPrimaryKey(
            Long.parseLong(shadowTableConfigVo.getShadowDatasourceId()));
        if (queryShadowDatasource == null) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_DATASOURCE_ID_EXCEPTION);
        }
        List<TShadowTableConfig> existTableConfigs = tShadowTableConfigDao.queryShadowTableByDatasourceId(
            queryShadowDatasource.getShadowDatasourceId(),
            Arrays.asList(shadowTableConfigVo.getShadowTableName().toUpperCase()));
        //????????????????????? ??? ??????id ?????????  ?????????????????????
        if (CollectionUtils.isNotEmpty(existTableConfigs) && !existTableConfigs.get(0).getId().equals(
            Long.parseLong(shadowTableConfigVo.getId()))) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_UPDATE_SAME_EXCEPTION);
        }
        TShadowTableConfig shadowTableConfig = new TShadowTableConfig();
        BeanUtils.copyProperties(shadowTableConfigVo, shadowTableConfig);
        //????????????????????????
        shadowTableConfig.setId(Long.parseLong(shadowTableConfigVo.getId()));
        shadowTableConfig.setShadowDatasourceId(Long.parseLong(shadowTableConfigVo.getShadowDatasourceId()));
        //???????????????????????????
        shadowTableConfig.setShadowTableName(shadowTableConfigVo.getShadowTableName().toUpperCase());
        tShadowTableConfigDao.updateByPrimaryKeySelective(shadowTableConfig);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param idList
     * @throws TROModuleException
     */
    public void deleteShadowTableByIdList(String idList) throws TROModuleException {
        if (StringUtils.isEmpty(idList)) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_DELETE_EXCEPTION);
        }
        tShadowTableConfigDao.deleteByIdList(Splitter.on(",")
            .omitEmptyStrings()
            .trimResults()
            .splitToList(idList));
    }

    /**
     * ??????????????????  ???????????????????????? ip?????? ??? ??????
     *
     * @param applicationId
     * @return
     */
    public List<TShadowTableConfigVo> queryApplicationDatabaseIpPortAndName(String applicationId) {
        return tShadowTableDataSourceDao.queryDatabaseIpPortAndName(applicationId);
    }

    /**
     * ?????????????????????  ??????
     *
     * @param shadowTableConfigVo
     * @throws TROModuleException
     */
    public void saveShadowTableConfig(TShadowTableConfigVo shadowTableConfigVo) throws TROModuleException {
        if (StringUtils.isEmpty(shadowTableConfigVo.getShadowDatasourceId()) ||
            StringUtils.isEmpty(shadowTableConfigVo.getShadowTableName())
            || shadowTableConfigVo.getEnableStatus() == null) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_ADD_PARAM_EXCEPTION);
        }
        TShadowTableDataSource queryShadowDatasource = tShadowTableDataSourceDao.selectByPrimaryKey(
            Long.parseLong(shadowTableConfigVo.getShadowDatasourceId()));
        if (queryShadowDatasource == null) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_DATASOURCE_ID_EXCEPTION);
        }
        String[] talbeNames = shadowTableConfigVo.getShadowTableName().split(",");
        List<TShadowTableConfig> tableConfigList = new ArrayList<>(talbeNames.length);
        List<String> tableNameList = new ArrayList<>(talbeNames.length);
        for (String tableName : talbeNames) {
            if (tableName.length() > 127) {
                throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOWCONFIG_TABLENAME_LONG_EXCEPTION);
            }
            tableNameList.add(tableName.toUpperCase());
        }
        List<TShadowTableConfig> existTableConfigList = tShadowTableConfigDao.queryShadowTableByDatasourceId(
            queryShadowDatasource.getShadowDatasourceId(), tableNameList);
        for (String tableName : talbeNames) {
            boolean existTableName = false;
            for (TShadowTableConfig existTable : existTableConfigList) {
                if (existTable.getShadowTableName().equalsIgnoreCase(tableName)) {
                    existTableName = true;
                    break;
                }
            }
            if (!existTableName) {
                TShadowTableConfig tableConfig = new TShadowTableConfig(snowflake.next(),
                    queryShadowDatasource.getShadowDatasourceId(), tableName.toUpperCase(),
                    shadowTableConfigVo.getEnableStatus());
                tableConfigList.add(tableConfig);
            }
        }
        if (CollectionUtils.isNotEmpty(tableConfigList)) {
            tShadowTableConfigDao.insertList(tableConfigList);
        }
    }

    /**
     * ?????????????????????????????????????????????IP??????
     *
     * @param applicationId ??????ID
     * @param dbName        ???????????????
     * @return java.util.List<java.lang.String>
     * @author shulie
     * @create 2019/3/14 10:43
     */
    public List<String> queryDatabaseIpPortList(String applicationId, String dbName) {
        return tShadowTableDataSourceDao.queryDatabaseIpPortList(applicationId, dbName);
    }

    /**
     * ?????????????????????????????????????????????IP??????
     *
     * @param applicationId ??????ID
     * @param ipPort        ????????????IP?????????
     * @return java.util.List<java.lang.String>
     * @author shulie
     * @create 2019/3/14 10:43
     */
    public List<Map<String, Object>> queryDatabaseNameList(String applicationId, String ipPort) {
        List<Map<String, Object>> maps = tShadowTableDataSourceDao.queryDatabaseNameList(applicationId, ipPort);
        return maps;
    }

    /**
     * ???????????????????????? ????????????
     *
     * @param paramMap
     * @return
     */
    public PageInfo<TShadowTableConfigVo> queryShadowDBApplicationList(Map<String, Object> paramMap) {
        if (!StringUtils.equals("-1", MapUtils.getString(paramMap, "pageSize"))) {
            PageHelper.startPage(PageInfo.getPageNum(paramMap), PageInfo.getPageSize(paramMap));
        }
        List<TShadowTableConfigVo> shadowTableConfigVoList = tShadowTableDataSourceDao.queryShadowDBApplicationList(
            paramMap);
        return new PageInfo<>(shadowTableConfigVoList);
    }

    public void saveShadowDatasource(TShadowTableDatasourceVo shadowDatasourceVo) throws TROModuleException {
        if (StringUtils.isEmpty(shadowDatasourceVo.getApplicationId()) || StringUtils.isEmpty(
            shadowDatasourceVo.getDatabaseIpport()) ||
            StringUtils.isEmpty(shadowDatasourceVo.getDatabaseName())
            || shadowDatasourceVo.getUseShadowTable() == null) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOW_DATASOURCE_SAVE_EXCEPTION);
        }
        //????????????????????????
        shadowDatasourceVo.setDatabaseName(shadowDatasourceVo.getDatabaseName().toUpperCase());
        Long datasourceId = tShadowTableDataSourceDao.queryShadowTableDatasourceId(
            Long.parseLong(shadowDatasourceVo.getApplicationId()), shadowDatasourceVo.getDatabaseName(),
            shadowDatasourceVo.getDatabaseIpport());
        if (datasourceId != null) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOW_DATASOURCE_EXIST_EXCEPTION);
        }
        TShadowTableDataSource shadowTableDataSource = new TShadowTableDataSource();
        BeanUtils.copyProperties(shadowDatasourceVo, shadowTableDataSource);
        shadowTableDataSource.setShadowDatasourceId(snowflake.next());
        shadowTableDataSource.setApplicationId(Long.parseLong(shadowDatasourceVo.getApplicationId()));
        tShadowTableDataSourceDao.insert(shadowTableDataSource);
    }

    public void updateShadowDatasource(TShadowTableDatasourceVo shadowDatasourceVo) throws TROModuleException {
        if (StringUtils.isEmpty(shadowDatasourceVo.getShadowDatasourceId())) {
            throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOW_DATASOURCE_EXIST_EXCEPTION);
        }
        //????????????????????? ?????????????????????????????????
        if (StringUtils.isNotEmpty(shadowDatasourceVo.getApplicationId()) && StringUtils.isNotEmpty(
            shadowDatasourceVo.getDatabaseName())
            && StringUtils.isNotEmpty(shadowDatasourceVo.getDatabaseIpport())) {
            Long datasourceId = tShadowTableDataSourceDao.queryShadowTableDatasourceId(
                Long.parseLong(shadowDatasourceVo.getApplicationId()),
                shadowDatasourceVo.getDatabaseName().toUpperCase(), shadowDatasourceVo.getDatabaseIpport());
            //id ?????? ????????? ?????????????????????
            if (datasourceId != null && !datasourceId.toString().equals(shadowDatasourceVo.getShadowDatasourceId())) {
                throw new TROModuleException(TROErrorEnum.API_TRO_CONFCENTER_SHADOW_DATASOURCE_EXIST_EXCEPTION);
            }
        }
        TShadowTableDataSource shadowTableDataSource = new TShadowTableDataSource();
        BeanUtils.copyProperties(shadowDatasourceVo, shadowTableDataSource);
        shadowTableDataSource.setShadowDatasourceId(Long.parseLong(shadowDatasourceVo.getShadowDatasourceId()));
        //?????????????????????
        if (StringUtils.isNotEmpty(shadowDatasourceVo.getDatabaseName())) {
            shadowTableDataSource.setDatabaseName(shadowDatasourceVo.getDatabaseName().toUpperCase());
        }
        //????????????????????????
        if (StringUtils.isNotEmpty(shadowDatasourceVo.getApplicationId())) {
            shadowTableDataSource.setApplicationId(Long.parseLong(shadowDatasourceVo.getApplicationId()));
        }
        tShadowTableDataSourceDao.updateByPrimaryKeySelective(shadowTableDataSource);
    }

    /**
     * ???????????????????????? ????????? ??????
     *
     * @return
     */
    public List<Map<String, Object>> getDatasourceApplication(String useShadowTable) {
        return transferElementToString(tShadowTableDataSourceDao.queryDatasourceApplicationdata(useShadowTable));
    }

}
