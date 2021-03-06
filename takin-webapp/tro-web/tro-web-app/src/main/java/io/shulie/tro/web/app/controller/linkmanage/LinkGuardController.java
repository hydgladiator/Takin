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
package io.shulie.tro.web.app.controller.linkmanage;

import java.util.List;

import com.pamirs.tro.entity.domain.entity.LinkGuardEntity;
import com.pamirs.tro.entity.domain.query.LinkGuardQueryParam;
import com.pamirs.tro.entity.domain.vo.guardmanage.LinkGuardVo;
import io.shulie.tro.common.beans.annotation.ModuleDef;
import io.shulie.tro.web.app.annotation.AuthVerification;
import io.shulie.tro.web.app.common.Response;
import io.shulie.tro.web.app.constant.APIUrls;
import io.shulie.tro.web.app.constant.BizOpConstants;
import io.shulie.tro.web.app.context.OperationLogContextHolder;
import io.shulie.tro.web.app.exception.ExceptionCode;
import io.shulie.tro.web.app.exception.TroWebException;
import io.shulie.tro.web.app.service.linkManage.LinkGuardService;
import io.shulie.tro.web.auth.api.enums.ActionTypeEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: ??????
 * @Date: 2020-03-05 09:20
 * @Description: ????????????
 */

@RestController
@RequestMapping(APIUrls.TRO_API_URL + "console")
@Api(tags = "????????????", value = "????????????")
public class LinkGuardController {

    @Autowired
    private LinkGuardService linkGuardService;

    /**
     * ????????????
     *
     * @param vo
     * @return
     */
    @ApiOperation("??????????????????")
    @PostMapping("/link/guard/guardmanage")
    @ModuleDef(
        moduleName = BizOpConstants.Modules.APPLICATION_MANAGE,
        subModuleName = BizOpConstants.SubModules.OUTLET_BAFFLE,
        logMsgKey = BizOpConstants.Message.MESSAGE_OUTLET_BAFFLE_CREATE
    )
    @AuthVerification(
            moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
            needAuth = ActionTypeEnum.CREATE
    )
    public Response storetechLink(@RequestBody LinkGuardVo vo) {
        // ??????????????????
        if(StringUtils.isNotBlank(vo.getRemark()) && vo.getRemark().length() > 200) {
            throw new TroWebException(ExceptionCode.GUARD_PARAM_ERROR,"????????????????????????200??????");
        }
        OperationLogContextHolder.operationType(BizOpConstants.OpTypes.CREATE);
        OperationLogContextHolder.addVars(BizOpConstants.Vars.CLASS_METHOD_NAME, vo.getMethodInfo());
        return linkGuardService.addGuard(vo);

    }

    /**
     * ????????????
     *
     * @param applicationName
     * @param id
     * @param current
     * @param pageSize
     * @return
     */
    @GetMapping("/link/guard/guardmanage")
    @ApiOperation("????????????????????????")
    @AuthVerification(
            moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
            needAuth = ActionTypeEnum.QUERY
    )
    public Response<List<LinkGuardVo>> gettGuardList(
        @ApiParam(name = "applicationName", value = "????????????") @RequestParam(value = "applicationName", required = false)
            String applicationName,
        @ApiParam(name = "id", value = "??????id") @RequestParam(value = "id", required = false) Long id,
        @ApiParam(name = "applicationId", value = "??????id") @RequestParam(value = "applicationId", required = false)
            String applicationId,
        Integer current,
        Integer pageSize
    ) {
        LinkGuardQueryParam param = new LinkGuardQueryParam();
        param.setId(id);
        param.setApplicationName(applicationName);
        param.setApplicationId(applicationId);
        param.setCurrentPage(current);
        param.setPageSize(pageSize);
        return linkGuardService.selectByExample(param);
    }

    @GetMapping("/link/guard/guardmanage/all")
    @ApiOperation("???????????????????????????")
    public Response<List<LinkGuardEntity>> getAllEnableGuard(
    ) {
        return linkGuardService.selectAll();
    }

    @GetMapping("/link/guard/guardmanage/info")
    @ApiOperation("??????????????????")
    @AuthVerification(
            moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
            needAuth = ActionTypeEnum.QUERY
    )
    public Response<LinkGuardVo> gettGuardInfo(
        @ApiParam(name = "id", value = "??????id") @RequestParam(value = "id", required = true) Long id

    ) {
        if (id == null) {
            return Response.fail("0", "id ????????????", null);
        }
        return linkGuardService.getById(id);
    }

    @ApiOperation("??????????????????")
    @PutMapping("/link/guard/guardmanage")
    @ModuleDef(
        moduleName = BizOpConstants.Modules.APPLICATION_MANAGE,
        subModuleName = BizOpConstants.SubModules.OUTLET_BAFFLE,
        logMsgKey = BizOpConstants.Message.MESSAGE_OUTLET_BAFFLE_UPDATE
    )
    @AuthVerification(
            moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
            needAuth = ActionTypeEnum.UPDATE
    )
    public Response modifyGuard(@RequestBody @ApiParam(name = "vo", value = "??????????????????") LinkGuardVo vo) {
        // ??????????????????
        if(StringUtils.isNotBlank(vo.getRemark()) && vo.getRemark().length() > 200) {
            throw new TroWebException(ExceptionCode.GUARD_PARAM_ERROR,"????????????????????????200??????");
        }
        OperationLogContextHolder.operationType(BizOpConstants.OpTypes.UPDATE);
        OperationLogContextHolder.addVars(BizOpConstants.Vars.CLASS_METHOD_NAME, vo.getMethodInfo());
        return linkGuardService.updateGuard(vo);
    }

    @ApiOperation("??????????????????")
    @RequestMapping(value = "/link/guard/guardmanage", method = RequestMethod.DELETE)
    @ModuleDef(
        moduleName = BizOpConstants.Modules.APPLICATION_MANAGE,
        subModuleName = BizOpConstants.SubModules.OUTLET_BAFFLE,
        logMsgKey = BizOpConstants.Message.MESSAGE_OUTLET_BAFFLE_DELETE
    )
    @AuthVerification(
            moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
            needAuth = ActionTypeEnum.DELETE
    )
    public Response deleteGuard(@RequestBody LinkGuardVo vo) {
        OperationLogContextHolder.operationType(BizOpConstants.OpTypes.DELETE);

        LinkGuardVo linkGuardVo = linkGuardVo(vo.getId());
        if (null == linkGuardVo) {
            return Response.fail("??????????????????");
        }
        OperationLogContextHolder.addVars(BizOpConstants.Vars.CLASS_METHOD_NAME, linkGuardVo.getMethodInfo());
        return linkGuardService.deleteById(vo.getId());
    }

    @ApiOperation("???????????????????????????")
    @PutMapping(value = "/link/guard/guardmanage/switch")
    @ModuleDef(
        moduleName = BizOpConstants.Modules.APPLICATION_MANAGE,
        subModuleName = BizOpConstants.SubModules.OUTLET_BAFFLE,
        logMsgKey = BizOpConstants.Message.MESSAGE_OUTLET_BAFFLE_ENABLE_DISABLE
    )
    @AuthVerification(
            moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
            needAuth = ActionTypeEnum.ENABLE_DISABLE
    )
    public Response guard(@RequestBody LinkGuardVo vo) {
        OperationLogContextHolder.operationType(
            vo.getIsEnable() ? BizOpConstants.OpTypes.ENABLE : BizOpConstants.OpTypes.DISABLE);
        LinkGuardVo linkGuardVo = linkGuardVo(vo.getId());
        if (null == linkGuardVo) {
            return Response.fail("??????????????????");
        }
        OperationLogContextHolder.addVars(BizOpConstants.Vars.CLASS_METHOD_NAME, linkGuardVo.getMethodInfo());
        return linkGuardService.enableGuard(vo.getId(), vo.getIsEnable());
    }

    private LinkGuardVo linkGuardVo(Long id) {
        Response<LinkGuardVo> linkGuardVo = linkGuardService.getById(id);
        if (null == linkGuardVo) {
            return null;
        }
        LinkGuardVo data = linkGuardVo.getData();
        return data;
    }

}
