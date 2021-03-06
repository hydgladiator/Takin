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

package io.shulie.tro.web.app.service.linkManage;

import java.util.List;

import com.pamirs.tro.entity.domain.dto.EntranceSimpleDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.BusinessActiveIdAndNameDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.BusinessFlowDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.BusinessFlowIdAndNameDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.EntranceDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.MiddleWareNameDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.SceneDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.SystemProcessIdAndNameDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.SystemProcessViewListDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.TechLinkDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.TopologicalGraphVo;
import com.pamirs.tro.entity.domain.dto.linkmanage.linkstatistics.LinkHistoryInfoDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.linkstatistics.LinkRemarkDto;
import com.pamirs.tro.entity.domain.entity.linkmanage.statistics.StatisticsQueryVo;
import com.pamirs.tro.entity.domain.vo.linkmanage.BusinessFlowVo;
import com.pamirs.tro.entity.domain.vo.linkmanage.BusinessLinkVo;
import com.pamirs.tro.entity.domain.vo.linkmanage.MiddleWareEntity;
import com.pamirs.tro.entity.domain.vo.linkmanage.TechLinkVo;
import com.pamirs.tro.entity.domain.vo.linkmanage.queryparam.BusinessQueryVo;
import com.pamirs.tro.entity.domain.vo.linkmanage.queryparam.SceneQueryVo;
import com.pamirs.tro.entity.domain.vo.linkmanage.queryparam.TechQueryVo;
import io.shulie.tro.web.app.common.Response;
import io.shulie.tro.web.app.response.application.ApplicationDetailResponse;
import io.shulie.tro.web.app.response.linkmanage.BusinessActivityNameResponse;
import io.shulie.tro.web.app.response.linkmanage.BusinessLinkResponse;
import io.shulie.tro.web.app.response.linkmanage.MiddleWareResponse;
import io.shulie.tro.web.app.response.linkmanage.TechLinkResponse;

/**
 * @Auther: vernon
 * @Date: 2019/11/29 14:43
 * @Description:
 */
public interface LinkManageService {
    /////////////????????????
    //Result ???????????????????????????
    //??????????????????????????? ?????????????????????????????????Json

    TechLinkResponse fetchLink(String applicationName, String entrance);

    TopologicalGraphVo fetchGraph(String body);

    List<EntranceDto> fetchApp();

    List<EntranceDto> fetchEntrance(String applicationName);

    //?????????????????????????????????????????????links,??????:Result,link.link = 1
    Response storetechLink(TechLinkVo links);

    //???????????? ?????????linkType = 0,????????????
    Response deleteLink(String id);

    //?????????????????? ??????:Link.linkType = 0,????????????id???
    Response modifyLink(TechLinkVo links);

    //?????????????????? ??????:?????????????????????????????????????????? vo.linkType ?????????TechLinkDto
    Response<List<SystemProcessViewListDto>> gettechLinksViwList(TechQueryVo vo);

    Response addBusinessLink(BusinessLinkVo links);

    Response modifyBussinessLink(BusinessLinkVo link);

    //?????????????????? ??????:?????????????????????????????????????????? vo.LinkType = 0,vo.linkChange ?????????TechLinkDto
    Response getBussisnessLinks(BusinessQueryVo vo);

    //??????????????????: ????????????????????????
    Response deleteScene(String sceneId);

    //??????????????????:sceneName:????????????,linkName:????????????entrace:??????,ischange??????????????????
    Response<List<SceneDto>> getScenes(SceneQueryVo vo);

    Response getMiddleWareInfo(StatisticsQueryVo vo);

    LinkRemarkDto getstatisticsInfo();

    Response deletBusinessLink(String id);

    LinkHistoryInfoDto getChart();

    List<MiddleWareEntity> getAllMiddleWareTypeList();

    List<SystemProcessIdAndNameDto> ggetAllSystemProcess(String systemProcessName);

    List<SystemProcessIdAndNameDto> getAllSystemProcessCanrelateBusiness(String systemProcessName);

    List<String> entranceFuzzSerach(String entrance);

    /**
     * ????????????????????????, ?????????????????????
     * ??????????????????????????????id
     * @param businessActiveName ??????????????????
     * @return ??????????????????????????????id
     */
    List<BusinessActiveIdAndNameDto> businessActiveNameFuzzSearch(String businessActiveName);

    TechLinkDto fetchTechLinkDetail(String id);

    BusinessLinkResponse getBussisnessLinkDetail(String id);

    // SceneDetailDto deleteSceneLinkRelatedByBusinessId(String businessId, String sceneId);

    List<MiddleWareEntity> businessProcessMiddleWares(List<String> ids);

    BusinessFlowDto getBusinessFlowDetail(String id);

    void modifyBusinessFlow(BusinessFlowVo vo) throws Exception;

    void addBusinessFlow(BusinessFlowVo vo) throws Exception;

    List<BusinessFlowIdAndNameDto> businessFlowIdFuzzSearch(String businessFlowName) throws Exception;

    List<MiddleWareNameDto> cascadeMiddleWareNameAndVersion(String middleWareType) throws Exception;

    List<MiddleWareNameDto> getDistinctMiddleWareName();

    List<EntranceSimpleDto> getEntranceByAppName(String applicationName);

    List<ApplicationDetailResponse> getApplicationDetailsByAppName(String applicationName, String entrance,
        String linkApplicationName) throws Exception;

    List<MiddleWareResponse> getMiddleWareResponses(String applicationName);

    /**
     * ??????????????????id??????????????????id?????????
     *
     * @param businessFlowId
     * @return
     */
    List<BusinessActivityNameResponse> getBusinessActiveByFlowId(Long businessFlowId);
}
