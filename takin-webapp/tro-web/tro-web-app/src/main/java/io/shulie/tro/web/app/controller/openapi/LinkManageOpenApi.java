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

package io.shulie.tro.web.app.controller.openapi;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.pamirs.tro.entity.domain.dto.linkmanage.BusinessActiveViewListDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.BusinessFlowDto;
import com.pamirs.tro.entity.domain.dto.linkmanage.SceneDto;
import com.pamirs.tro.entity.domain.vo.linkmanage.queryparam.BusinessQueryVo;
import com.pamirs.tro.entity.domain.vo.linkmanage.queryparam.SceneQueryVo;
import io.shulie.tro.web.app.common.Response;
import io.shulie.tro.web.app.constant.APIUrls;
import io.shulie.tro.web.app.controller.openapi.converter.LinkManageOpenApiConverter;
import io.shulie.tro.web.app.controller.openapi.response.linkmanage.BusinessActiveViewListOpenApiResp;
import io.shulie.tro.web.app.controller.openapi.response.linkmanage.BusinessFlowOpenApiResp;
import io.shulie.tro.web.app.controller.openapi.response.linkmanage.BusinessLinkOpenApiResp;
import io.shulie.tro.web.app.controller.openapi.response.linkmanage.SceneOpenApiResp;
import io.shulie.tro.web.app.controller.openapi.response.linkmanage.SystemProcessViewListOpenApiResp;
import io.shulie.tro.web.app.controller.openapi.response.linkmanage.TechLinkOpenApiResp;
import io.shulie.tro.web.app.response.linkmanage.BusinessLinkResponse;
import io.shulie.tro.web.app.service.linkManage.LinkManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Auther: vernon
 * @Date: 2019/11/29 14:05
 * @Description:????????????
 */
@RestController
@RequestMapping(APIUrls.TRO_OPEN_API_URL)
@Api(tags = "linkmanage", value = "????????????")
public class LinkManageOpenApi {

    @Autowired
    private LinkManageService linkManageService;
    /**
     * ??????????????????
     */
    @Value("${link.graph.enable:false}")
    private boolean graphEable;

    @GetMapping("/link/tech/linkManage")
    @ApiOperation("??????????????????????????????")
    public Response<List<SystemProcessViewListOpenApiResp>> gettechLinksViwList(
        @ApiParam(name = "linkName", value = "??????????????????") String linkName,
        @ApiParam(name = "entrance", value = "??????") String entrance,
        @ApiParam(name = "ischange", value = "????????????") String ischange,
        @ApiParam(name = "middleWareType", value = "???????????????") String middleWareType,
        @ApiParam(name = "middleWareName", value = "???????????????") String middleWareName,
        @ApiParam(name = "middleWareVersion", value = "???????????????") String middleWareVersion,
        Integer current,
        Integer pageSize
    ) {
        return Response.success();
    }

    @GetMapping("/link/tech/linkManage/detail")
    @ApiOperation("??????????????????????????????????????????????????????")
    public Response<TechLinkOpenApiResp> fetchTechLinkDetail(
        @RequestParam("id")
        @ApiParam(name = "id", value = "??????????????????") String id) {
        return Response.success();
    }

    @GetMapping("/link/business/manage")
    @ApiOperation("????????????????????????")
    public Response<List<BusinessActiveViewListOpenApiResp>> getBussisnessLinks(
        @ApiParam(name = "businessLinkName", value = "??????????????????") String businessLinkName,
        @ApiParam(name = "entrance", value = "??????") String entrance,
        @ApiParam(name = "ischange", value = "????????????") String ischange,
        @ApiParam(name = "domain", value = "?????????") String domain,
        @ApiParam(name = "middleWareType", value = "???????????????") String middleWareType,
        @ApiParam(name = "middleWareName", value = "???????????????") String middleWareName,
        @ApiParam(name = "middleWareVersion", value = "??????????????????") String middleWareVersion,
        @ApiParam(name = "systemProcessName", value = "??????????????????") String systemProcessName,
        Integer current,
        Integer pageSize
    ) {
        BusinessQueryVo vo = new BusinessQueryVo();
        vo.setBusinessLinkName(businessLinkName);
        vo.setEntrance(entrance);
        vo.setIschange(ischange);
        vo.setDomain(domain);
        vo.setMiddleWareType(middleWareType);
        vo.setMiddleWareName(middleWareName);
        vo.setTechLinkName(systemProcessName);
        vo.setVersion(middleWareVersion);
        vo.setCurrentPage(current);
        vo.setPageSize(pageSize);
        Response<List<BusinessActiveViewListDto>> bussisnessLinks = linkManageService.getBussisnessLinks(vo);
        return Response.success(
            LinkManageOpenApiConverter.INSTANCE.ofListBusinessActiveViewListOpenApiResp(bussisnessLinks.getData()));
    }

    @GetMapping("/link/business/manage/detail")
    @ApiOperation("????????????????????????")
    public Response<BusinessLinkOpenApiResp> getBussisnessLinkDetail(@ApiParam(name = "id", value = "??????????????????")
    @RequestParam("id")
        String id) {
        try {
            BusinessLinkResponse businessLinkResponse = linkManageService.getBussisnessLinkDetail(id);
            return Response.success(
                LinkManageOpenApiConverter.INSTANCE.ofBusinessLinkOpenApiResp(businessLinkResponse));
        } catch (Exception e) {
            return Response.fail("0", e.getMessage());
        }

    }

    @GetMapping("/link/scene/manage")
    @ApiOperation("????????????????????????")
    public Response<List<SceneOpenApiResp>> getScenes
        (
            @ApiParam(name = "sceneId", value = "??????id") Long sceneId,
            @ApiParam(name = "sceneName", value = "??????????????????") String sceneName,
            @ApiParam(name = "entrance", value = "??????") String entrance,
            @ApiParam(name = "ischange", value = "????????????") String ischange,
            @ApiParam(name = "businessName", value = "???????????????") String businessName,
            @ApiParam(name = "middleWareType", value = "???????????????") String middleWareType,
            @ApiParam(name = "middleWareName", value = "???????????????") String middleWareName,
            @ApiParam(name = "middleWareVersion", value = "???????????????") String middleWareVersion,
            Integer current,
            Integer pageSize
        ) {
        SceneQueryVo vo = new SceneQueryVo();
        vo.setSceneId(sceneId);
        vo.setSceneName(sceneName);
        vo.setEntrace(entrance);
        vo.setIschanged(ischange);
        vo.setBusinessName(businessName);
        vo.setMiddleWareType(middleWareType);
        vo.setMiddleWareName(middleWareName);
        vo.setMiddleWareVersion(middleWareVersion);
        vo.setCurrentPage(current);
        vo.setPageSize(pageSize);
        Response<List<SceneDto>> scenes = linkManageService.getScenes(vo);
        return Response.success(LinkManageOpenApiConverter.INSTANCE.ofListSceneOpenApiResp(scenes.getData()));
    }

    @GetMapping("/link/scene/tree/detail")
    @ApiOperation("???????????????????????????")
    public Response<BusinessFlowOpenApiResp> getBusinessFlowDetail(@NotNull String id) {
        try {
            BusinessFlowDto dto = linkManageService.getBusinessFlowDetail(id);
            return Response.success(LinkManageOpenApiConverter.INSTANCE.ofBusinessFlowOpenApiResp(dto));
        } catch (Exception e) {
            return Response.fail("0", e.getMessage(), null);
        }
    }

}
