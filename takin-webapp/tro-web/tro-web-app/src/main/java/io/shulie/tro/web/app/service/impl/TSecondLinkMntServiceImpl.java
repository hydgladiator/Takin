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

package io.shulie.tro.web.app.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pamirs.tro.common.constant.LinkLevelEnum;
import com.pamirs.tro.common.constant.TROErrorEnum;
import com.pamirs.tro.common.exception.TROModuleException;
import com.pamirs.tro.common.util.PageInfo;
import com.pamirs.tro.entity.domain.entity.TApplicationMnt;
import com.pamirs.tro.entity.domain.entity.TFirstLinkMnt;
import com.pamirs.tro.entity.domain.entity.TLinkMnt;
import com.pamirs.tro.entity.domain.entity.TSecondBasic;
import com.pamirs.tro.entity.domain.entity.TSecondLinkMnt;
import com.pamirs.tro.entity.domain.vo.TLinkServiceMntVo;
import io.shulie.tro.web.app.common.CommonService;
import io.shulie.tro.web.app.service.TSecondLinkMntService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ???????????????????????????
 *
 * @author shulie
 */

@Service
public class TSecondLinkMntServiceImpl extends CommonService implements TSecondLinkMntService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSecondLink(TSecondLinkMnt secondLinkMnt) throws Exception {
        String linkName = secondLinkMnt.getLinkName();
        int linkExist = secondLinkMntDao.saveSecondLinkExist(linkName);
        if (linkExist > 0) {
            throw new TROModuleException(TROErrorEnum.CONFCENTER_SECOND_LINK_DUPICATE_EXCEPTION);
        } else {
            Long secondLinkId = snowflake.next();
            secondLinkMnt.setLinkId(secondLinkId + "");
            secondLinkMnt.setTestStatus("0");
            secondLinkMntDao.addSecondLink(secondLinkMnt);
            String baseLinks = secondLinkMnt.getBaseLinks();
            saveRelationLink(secondLinkId, baseLinks, "t_second_basic");

        }
    }

    @Override
    public PageInfo<TSecondLinkMnt> queryLinkList(String linkName, String baseLinkName,
        Integer pageNum, Integer pageSize) throws TROModuleException {
        if (pageSize == null || pageNum == null) {
            throw new TROModuleException(TROErrorEnum.PAGE_PARAM_EXCEPTION);
        }
        if (pageSize != -1) {
            PageHelper.startPage(pageNum, pageSize);
        }
        // ???????????????????????????????????????????????????????????????????????????
        List<TSecondLinkMnt> secondLinkList;
        //???????????????secondLinkName?????????????????????
        if (StringUtils.isEmpty(baseLinkName)) {
            secondLinkList = secondLinkMntDao.queryLinkList(linkName);
        } else {
            //            Map<String, Object> paramMap = new HashMap<>(10);
            //            paramMap.put("linkName", linkName);
            //            paramMap.put("baseLinkName", baseLinkName);
            //            secondLinkList = secondLinkMntDao.querySecondLinkListByView(paramMap);
            // ????????????????????????????????????
            Map<String, Object> paramMap = new HashMap<>(10);
            paramMap.put("linkName", linkName);
            paramMap.put("baseLinkName", baseLinkName);
            secondLinkList = secondLinkMntDao.querySecondLinkList(paramMap);
        }

        //????????????????????????????????????????????????
        for (TSecondLinkMnt secondLink : secondLinkList) {
            // ???????????????id???????????????????????????
            List<List<Map<String, Object>>> basicLinkList = getRelationLinkRelationShip("t_second_basic",
                secondLink.getLinkId());
            secondLink.setBaseLinks(JSON.toJSONString(basicLinkList));
            String linkTpsRule = secondLink.getLinkTpsRule();
            String linkTpsRuleName = getBaseLinkNameByStr(linkTpsRule);
            secondLink.setLinkTpsRule(linkTpsRuleName);
        }

        return new PageInfo<>(secondLinkList);
    }

    @Override
    public Map<String, Object> queryLinkMapByLinkId(String linkId) {
        TSecondLinkMnt secondLinkMnt = secondLinkMntDao.queryLinkByLinkId(linkId);
        Map<String, Object> secondLinkMap = new HashMap<>(15);
        List<Map<String, Object>> nodesList = Lists.newArrayList();
        if (secondLinkMnt != null) {
            try {
                String secondLinkName = secondLinkMnt.getLinkName();
                int linkBankCount = secondBasicDao.getBasicLinkBankCount(linkId);
                //                List<List<Map<String, Object>>> baseLinkList = getBasicLinkBySecondLinkId(linkId);
                List<List<Map<String, Object>>> baseLinkList = getRelationLinkRelationShip("t_second_basic", linkId);

                //2?????????????????????
                //                List<Map<String, Object>> linkTpsRuleList = getLinkDetailByIds(secondLinkMnt
                //                .getLinkTpsRule());
                List<Map<String, Object>> linkTpsRuleList = getLinkDetail(secondLinkMnt.getLinkTpsRule());
                secondLinkMap.put("linkId", secondLinkMnt.getLinkId());
                secondLinkMap.put("linkName", secondLinkName);
                secondLinkMap.put("aswanId", secondLinkMnt.getAswanId());
                secondLinkMap.put("baseLinks", baseLinkList);
                secondLinkMap.put("createTime", secondLinkMnt.getCreateTime());
                secondLinkMap.put("linkTps", secondLinkMnt.getLinkTps());
                secondLinkMap.put("linkTpsRule", linkTpsRuleList);
                secondLinkMap.put("remark", secondLinkMnt.getRemark());
                secondLinkMap.put("updateTime", secondLinkMnt.getUpdateTime());
                secondLinkMap.put("useYn", secondLinkMnt.getUseYn());
                secondLinkMap.put("testStatus", secondLinkMnt.getTestStatus());
                //????????????/?????????????????????
                List<Map<String, Object>> linkRelation = getLinkRelation(linkId, secondLinkName);
                secondLinkMap.put("links", linkRelation);
                //?????????????????????????????????
                Map<String, Object> secondNodeMap = new HashMap<>();
                secondNodeMap.put("bank", String.valueOf(0));
                secondNodeMap.put("name", secondLinkName);
                secondNodeMap.put("secondLinkId", linkId);
                secondNodeMap.put("x", String.valueOf(1));
                secondNodeMap.put("y", linkBankCount % 2 == 0 ? String.valueOf(linkBankCount / 2 + 0.5)
                    : String.valueOf(linkBankCount / 2 + 1));
                secondNodeMap.put("key", linkId + "0");
                nodesList.add(secondNodeMap);
                //?????????????????????????????????
                List<Map<String, Object>> basicNodesList = getBasicNodesList(linkId);
                nodesList.addAll(basicNodesList);
                secondLinkMap.put("nodes", nodesList);
            } catch (Exception e) {
                LOGGER.error("????????????????????????", e);
                e.printStackTrace();
            }
        }

        return secondLinkMap;
    }

    private List<Map<String, Object>> getBasicNodesList(String secondLinkId) {
        List<Map<String, Object>> basicNodesList = new ArrayList<>();
        List<TLinkMnt> tBasicLinkList = secondBasicDao.querySecondBasicLinkRelationBySecondLinkId(secondLinkId);
        //???????????????Map<K,V>?????????K????????????????????? V????????????????????????ID???????????????
        Map<String, List<String>> coordinateMap = Maps.newHashMap();
        if (tBasicLinkList != null) {
            for (TLinkMnt basicLink : tBasicLinkList) {
                Map<String, Object> basicNodesMap = Maps.newHashMap();
                String bLinkBank = String.valueOf(basicLink.getBLinkBank());
                String basicLinkId = String.valueOf(basicLink.getLinkId());
                //????????????????????????Map??????????????????key
                List<String> basicLinkIdList = coordinateMap.get(bLinkBank) == null ? Lists.newArrayList()
                    : coordinateMap.get(bLinkBank);
                basicLinkIdList.add(basicLinkId);
                coordinateMap.put(bLinkBank, basicLinkIdList);

                basicNodesMap.put("name", basicLink.getLinkName());
                basicNodesMap.put("bank", bLinkBank);
                basicNodesMap.put("basicLinkId", basicLinkId);
                basicNodesMap.put("key", basicLinkId + bLinkBank);
                basicNodesList.add(basicNodesMap);
            }
        }
        List<Integer> linkBankList = secondBasicDao.queryBasicLinkBank(secondLinkId);
        //??????????????????????????????,???????????????
        //????????????????????????
        for (int i = 0; i < linkBankList.size(); i++) {
            String linkBank = String.valueOf(linkBankList.get(i));
            List<String> basicLinkIdList = coordinateMap.get(linkBank);
            if (basicLinkIdList != null) {
                //?????????????????????????????????id??????
                for (int j = 0; j < basicLinkIdList.size(); j++) {
                    String linkId = basicLinkIdList.get(j);
                    for (Map<String, Object> basicNodesMap : basicNodesList) {
                        String basicLinkId = (String)basicNodesMap.get("basicLinkId");
                        String basicLinkBank = (String)basicNodesMap.get("bank");
                        //???????????????????????????Map?????????????????????????????????y??????????????????i
                        if (linkBank.equals(basicLinkBank)) {
                            basicNodesMap.put("y", String.valueOf(i + 1));
                            if (linkId.equals(basicLinkId)) {
                                basicNodesMap.put("x", String.valueOf(j + 2));
                            }
                        }
                    }
                }
            }
        }
        return basicNodesList;
    }

    /**
     * ??????????????????id??????????????????
     *
     * @param secondLinkId ????????????id
     * @return ??????/????????????????????????
     */
    private List<Map<String, Object>> getLinkRelation(String secondLinkId, String secondLinkName) {
        //??????????????????????????????
        Map<String, List<TLinkMnt>> linkParamMap = Maps.newHashMap();
        List<TLinkMnt> tBasicLinkList = secondBasicDao.querySecondBasicLinkRelationBySecondLinkId(secondLinkId);
        List<Map<String, Object>> relationList = Lists.newArrayList();
        if (tBasicLinkList != null) {
            for (TLinkMnt basicLink : tBasicLinkList) {
                List<TLinkMnt> tLinkMnts = linkParamMap.get(String.valueOf(basicLink.getBLinkBank())) == null ? Lists
                    .newArrayList() : linkParamMap.get(String.valueOf(basicLink.getBLinkBank()));
                tLinkMnts.add(basicLink);
                linkParamMap.put(String.valueOf(basicLink.getBLinkBank()), tLinkMnts);
                for (Map.Entry<String, List<TLinkMnt>> stringListEntry : linkParamMap.entrySet()) {
                    Map<String, Object> linkMap = Maps.newHashMap();

                    //???????????????????????????
                    Map<String, Object> secondLinkMap = Maps.newHashMap();
                    secondLinkMap.put("source", secondLinkName);
                    secondLinkMap.put("target", stringListEntry.getValue().get(0).getLinkName());
                    secondLinkMap.put("from", secondLinkId + "0");
                    //????????????
                    secondLinkMap.put("to", String.valueOf(stringListEntry.getValue().get(0).getLinkId()) + String
                        .valueOf(stringListEntry.getValue().get(0).getBLinkBank()));
                    if (!relationList.contains(secondLinkMap)) {
                        relationList.add(secondLinkMap);
                    }
                    List<TLinkMnt> value = stringListEntry.getValue();
                    for (int i = 0; i < value.size() - 1; i++) {
                        linkMap.put("source", value.get(i).getLinkName());
                        linkMap.put("target", value.get(i + 1).getLinkName());
                        linkMap.put("from",
                            String.valueOf(value.get(i).getLinkId()) + String.valueOf(value.get(i).getBLinkBank()));
                        linkMap.put("to", String.valueOf(value.get(i + 1).getLinkId()) + String
                            .valueOf(value.get(i + 1).getBLinkBank()));
                        if (!relationList.contains(linkMap)) {
                            relationList.add(linkMap);
                            linkMap = Maps.newHashMap();
                        }
                    }
                }
            }
        }
        return relationList;
    }

    /**
     * ??????: ???????????????????????????????????????,[[{},{}],[]...],???????????????list????????????????????????????????????,???????????????list??????????????????????????????????????????,????????????
     * ???????????????:
     * [
     * [
     * {
     * "label": 6420618780136706048,
     * "value": "basicLink-070501"
     * },
     * {
     * "label": 6421197580146839552,
     * "value": "????????????Test2018-7-7-1"
     * }
     * ],
     * [
     * {
     * "label": 6421197580146839552,
     * "value": "????????????Test2018-7-7-1"
     * },
     * {
     * "label": 6420922169202577408,
     * "value": "??????"
     * }
     * ]
     * ]
     *
     * @param secondLinkId ????????????id
     * @return ????????????????????????????????????
     * @author shulie
     * @date 2018/7/11 10:09
     */
    @Override
    @Deprecated
    public List<List<Map<String, Object>>> getBasicLinkBySecondLinkId(String secondLinkId) {
        //1?????????????????????????????????????????????
        List<Map<String, Object>> tSecondBasics = secondBasicDao.querySecondBasicLinkBySecondLinkIdModify(secondLinkId);
        ConcurrentMap<String, List<Map<String, Object>>> groupBasicLink = tSecondBasics.stream().collect(
            Collectors.groupingByConcurrent(map -> MapUtils.getString(map, "BLINK_BANK")));
        List<List<Map<String, Object>>> baseLinkList = Lists.newArrayListWithCapacity(groupBasicLink.size());
        groupBasicLink.forEach((string, mapList) -> {
            for (Iterator<Map<String, Object>> iterator = mapList.iterator(); iterator.hasNext(); ) {
                iterator.next().remove("BLINK_BANK");
            }
            baseLinkList.add(mapList);
        });
        return baseLinkList;
    }

    @Override
    public TSecondLinkMnt queryLinkByLinkId(String linkId) {
        return secondLinkMntDao.queryLinkByLinkId(linkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLinkByLinkIds(String linkIds) {
        List<String> linkIdLists = Arrays.stream(linkIds.split(",")).filter(StringUtils::isNotEmpty).distinct().collect(
            Collectors.toList());
        secondLinkMntDao.deleteLinkByLinkIds(linkIdLists);
        //??????????????????????????????t_second_basic??????????????????????????????
        secondBasicDao.deleteRefBySecondLinkId(linkIdLists);

        List<Object> logList = Lists.newArrayList();
        List<Map<String, Object>> secondLinkList = secondLinkMntDao.querySecondLinkListByIds(linkIdLists);
        List<Map<String, Object>> secondBasicLinkList = secondBasicDao.querySecondBasicLinkListByIds(linkIdLists);
        logList.add(secondLinkList);
        logList.add(secondBasicLinkList);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLinkinfo(TSecondLinkMnt secondLinkMnt) throws TROModuleException {
        String secondLinkId = secondLinkMnt.getLinkId();
        //        int linkExist = secondLinkMntDao.updateLinkExist(secondLinkId);
        TSecondLinkMnt originSecondLinkMnt = secondLinkMntDao.queryLinkByLinkId(secondLinkId);
        if (!Objects.isNull(originSecondLinkMnt)) {
            TSecondLinkMnt secondLink = queryLinkByLinkId(secondLinkId);
            secondLink.setUseYn(secondLinkMnt.getUseYn());
            secondLink.setAswanId(secondLinkMnt.getAswanId());
            secondLink.setBaseLinks(secondLinkMnt.getBaseLinks());
            secondLink.setLinkName(secondLinkMnt.getLinkName());
            secondLink.setLinkTpsRule(secondLinkMnt.getLinkTpsRule());
            secondLink.setRemark(secondLinkMnt.getRemark());
            secondLinkMntDao.updateLink(secondLink);
            //??????????????????t_second_basic???????????????????????????????????????
            secondBasicDao.deleteRefBySecondLinkId(Collections.singletonList(secondLinkId));
            saveRelationLink(Long.parseLong(secondLinkId), secondLinkMnt.getBaseLinks(), "t_second_basic");

        } else {
            LOGGER.info(TROErrorEnum.CONFCENTER_NOT_FOUND_SECOND_LINKID_EXCEPTION.getErrorMessage());

            throw new TROModuleException(TROErrorEnum.CONFCENTER_NOT_FOUND_SECOND_LINKID_EXCEPTION);
        }
    }

    @Override
    public void updateSecondLinkStatus(String secondLinkId, String testStatus) {
        TSecondLinkMnt tSecondLinkMnt = queryLinkByLinkId(secondLinkId);
        tSecondLinkMnt.setTestStatus(testStatus);
        secondLinkMntDao.updateLink(tSecondLinkMnt);
    }

    @Override
    public Map<String, List<TApplicationMnt>> queryApplicationListByLinkInfo(String linkId, String linkLevel)
        throws TROModuleException {
        Map<String, List<TApplicationMnt>> resultMap = new HashMap<>();
        //1??????????????????????????????????????????
        //2_0,????????????
        if (LinkLevelEnum.BASE_LINK_LEVEL.getName().equals(linkLevel)) {
            resultMap.put(linkId, queryApplicationByBaseLinkId(linkId));
        }
        //2_1, ????????????
        if (LinkLevelEnum.SECOND_LINK_LEVEL.getName().equals(linkLevel)) {
            //??????????????????????????????????????????
            resultMap.putAll(queryApplicationBySecondLinkId(linkId));
        }
        //2_2???????????????
        if (LinkLevelEnum.FIRST_LINK_LEVEL.getName().equals(linkLevel)) {
            resultMap.putAll(queryApplicationByFirstLinkId(linkId));
        }
        return resultMap;
    }

    //-------------------------------------private method area-------------------------------------------------

    /**
     * @param linkId ????????????id
     * @return void
     * @throws TROModuleException ??????
     * @description ??????????????????id??????????????????
     * @author shulie
     * @create 2018/6/19 19:43
     */
    private Map<String, List<TApplicationMnt>> queryApplicationByFirstLinkId(String linkId) throws TROModuleException {
        TFirstLinkMnt firstLinkMnt = firstLinkMntDao.queryLinkByLinkId(linkId);
        Map<String, List<TApplicationMnt>> resultMap = new HashMap<>();
        if (firstLinkMnt != null) {
            String secondLinks = firstLinkMnt.getSecondLinks();
            if (secondLinks == null) {
                LOGGER.error("?????????????????????NULL???");
                throw new TROModuleException(TROErrorEnum.CONFCENTER_SECOND_LINKID_LIST_IS_NULL_EXCEPTION);
            }
            String[] secondLinkIdArr = secondLinks.split(",");
            for (String secondLinkId : secondLinkIdArr) {
                resultMap.putAll(queryApplicationBySecondLinkId(secondLinkId));
            }
        }
        return resultMap;
    }

    /**
     * ??????????????????id??????????????????
     *
     * @param secondLinkId ????????????id
     * @return ????????????
     */
    private Map<String, List<TApplicationMnt>> queryApplicationBySecondLinkId(String secondLinkId) {
        Map<String, List<TApplicationMnt>> resultMap = new HashMap<>();
        List<TSecondBasic> secondBasicList = secondBasicDao.querySecondBasicLinkBySecondLinkId(secondLinkId);
        if (secondBasicList != null && secondBasicList.size() > 0) {
            for (TSecondBasic secondBasic : secondBasicList) {
                long basicLinkId = secondBasic.getBasicLinkId();
                String linkId = String.valueOf(basicLinkId);
                resultMap.put(linkId, queryApplicationByBaseLinkId(linkId));
            }
        }
        return resultMap;
    }

    /**
     * ??????????????????id??????????????????
     *
     * @param baseLinkId ????????????id
     * @return ????????????
     */
    private List<TApplicationMnt> queryApplicationByBaseLinkId(String baseLinkId) {
        return secondLinkMntDao.queryApplicationByBaseLinkId(baseLinkId);
    }

    /**
     * @param secondLinkId ???????????????id
     * @param baseLinks    ????????????
     * @throws java.io.IOException ????????????
     * @description ??????????????????????????????????????????
     * @author shulie
     * @create 2018/6/19 16:03
     */
    @Deprecated
    private void saveSecondBasicLinkRef(Long secondLinkId, String baseLinks) throws IOException {
        //??????baseLinks??????????????????t_second_basic??????
        ObjectMapper mapper = new ObjectMapper();
        List<List<String>> jsonList = mapper.readValue(baseLinks, List.class);
        for (int j = 0; j < jsonList.size(); j++) {
            List<String> baseLinkList = jsonList.get(j);
            for (int i = 0; i < baseLinkList.size(); i++) {
                String baseLink = baseLinkList.get(i);
                TSecondBasic secondBasic = new TSecondBasic();
                secondBasic.setSecondLinkId(secondLinkId);
                //????????????id??????,??????????????????,??????????????????
                secondBasic.setBasicLinkId(Long.parseLong(baseLink));
                secondBasic.setBlinkOrder(i + 1);
                secondBasic.setBlinkBank(j + 1);
                secondBasicDao.saveSecondBasicLink(secondBasic);
            }
        }
    }

    /**
     * ??????????????????????????????id??????????????????????????????
     *
     * @param linkIds ????????????id
     * @return java.lang.String
     * @description ???????????????????????????????????????????????????
     * @author shulie
     * @create 2018/6/19 19:38
     */
    private String getBaseLinkNameByStr(String linkIds) {
        List<String> linkIdList = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(linkIds);
        return Joiner.on(",").join(tLinkMnDao.transferBusinessLinkName(linkIdList));
    }

    private List<Map<String, Object>> getLinkDetail(String linkIds) {
        List<String> linkIdList = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(linkIds);
        return tLinkMnDao.transferBusinessLinkNameAndId(linkIdList);

    }

    @Deprecated
    private List<Map<String, Object>> getLinkDetailByIds(String linkIds) {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] linkIdArr = linkIds.split(",");
        for (String linkId : linkIdArr) {
            TLinkServiceMntVo linkServiceMntVo = tLinkMnDao.queryLinkByLinkId(linkId);
            if (linkServiceMntVo != null) {
                Map<String, Object> map = new HashMap<>(5);
                String linkName = linkServiceMntVo.getLinkName();
                map.put("label", linkName);
                map.put("value", linkId);
                list.add(map);
            }
        }
        return list;
    }
}
