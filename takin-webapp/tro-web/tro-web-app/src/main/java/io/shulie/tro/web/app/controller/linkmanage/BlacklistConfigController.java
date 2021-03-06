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

import javax.validation.Valid;

import io.shulie.tro.common.beans.annotation.ModuleDef;
import io.shulie.tro.common.beans.page.PagingList;
import io.shulie.tro.web.app.annotation.AuthVerification;
import io.shulie.tro.web.app.constant.APIUrls;
import io.shulie.tro.web.app.constant.BizOpConstants;
import io.shulie.tro.web.app.constant.BizOpConstants.Message;
import io.shulie.tro.web.app.context.OperationLogContextHolder;
import io.shulie.tro.web.app.exception.ExceptionCode;
import io.shulie.tro.web.app.exception.TroWebException;
import io.shulie.tro.web.app.input.blacklist.BlacklistCreateInput;
import io.shulie.tro.web.app.input.blacklist.BlacklistSearchInput;
import io.shulie.tro.web.app.input.blacklist.BlacklistUpdateInput;
import io.shulie.tro.web.app.output.blacklist.BlacklistOutput;
import io.shulie.tro.web.app.request.blacklist.BlacklistBatchDeleteRequest;
import io.shulie.tro.web.app.request.blacklist.BlacklistBatchEnableRequest;
import io.shulie.tro.web.app.request.blacklist.BlacklistCreateRequest;
import io.shulie.tro.web.app.request.blacklist.BlacklistDeleteRequest;
import io.shulie.tro.web.app.request.blacklist.BlacklistEnableRequest;
import io.shulie.tro.web.app.request.blacklist.BlacklistSearchRequest;
import io.shulie.tro.web.app.request.blacklist.BlacklistUpdateRequest;
import io.shulie.tro.web.app.response.blacklist.BlacklistDetailResponse;
import io.shulie.tro.web.app.response.blacklist.BlacklistStringResponse;
import io.shulie.tro.web.app.service.blacklist.BlacklistService;
import io.shulie.tro.web.auth.api.enums.ActionTypeEnum;
import io.shulie.tro.web.common.enums.blacklist.BlacklistEnableEnum;
import io.shulie.tro.web.common.enums.blacklist.BlacklistTypeEnum;
import io.shulie.tro.web.common.vo.blacklist.BlacklistVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ???????????????
 * @author ??????
 * @Package io.shulie.tro.web.app.controller.confcenter
 * @date 2021/4/6 1:50 ??????
 */
@Api(tags = "?????????????????????", value = "?????????")
@RestController
@RequestMapping(APIUrls.TRO_API_URL + "application/blacklist")
public class BlacklistConfigController {

   @Autowired
   private BlacklistService blacklistService;

    /**
     * ??????
     * @param request
     * @return
     */
    @ApiOperation("?????????????????????")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ModuleDef(
        moduleName = BizOpConstants.Modules.CONFIG_CENTER,
        subModuleName = BizOpConstants.SubModules.BLACKLIST,
        logMsgKey = BizOpConstants.Message.MESSAGE_BLACKLIST_CREATE
    )
    @AuthVerification(
        moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
        needAuth = ActionTypeEnum.CREATE
    )
    public BlacklistStringResponse add(@RequestBody @Valid BlacklistCreateRequest request) {
        if(StringUtils.isBlank(request.getRedisKey()) || StringUtils.isBlank(request.getRedisKey().trim())) {
            throw new TroWebException(ExceptionCode.BLACKLIST_ADD_ERROR,"???????????????????????????");
        }
        OperationLogContextHolder.operationType(BizOpConstants.OpTypes.CREATE);
        // ????????? redis
        OperationLogContextHolder.addVars(BizOpConstants.Vars.BLACKLIST_TYPE, BlacklistTypeEnum.getDescByType(0));
        OperationLogContextHolder.addVars(BizOpConstants.Vars.BLACKLIST_VALUE, request.getRedisKey());
        BlacklistCreateInput input = new BlacklistCreateInput();
        BeanUtils.copyProperties(request,input);
        blacklistService.insert(input);
        return new BlacklistStringResponse("????????????");
    }

    /**
     * ??????
     * @param request
     * @return
     */
    @ApiOperation("???????????????????????????")
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @AuthVerification(
        moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
        needAuth = ActionTypeEnum.QUERY
    )
    public PagingList<BlacklistVO> pageList(BlacklistSearchRequest request) {
        BlacklistSearchInput input = new BlacklistSearchInput();
        BeanUtils.copyProperties(request,input);
        return blacklistService.pageList(input);
    }

    /**
     * ??????id??????
     * @param id
     * @return
     */
    @ApiOperation("??????id???????????????????????????")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BlacklistDetailResponse querySingleById(@RequestParam("id") Long id) {
        BlacklistDetailResponse response = new BlacklistDetailResponse();
        BlacklistOutput output = blacklistService.selectById(id);
        if(output == null) {
            throw new TroWebException(ExceptionCode.BLACKLIST_SEARCH_ERROR,"????????????id");
        }
        BeanUtils.copyProperties(output,response);
        return response;
    }

    /**
     * ????????????id
     * @param request
     * @return
     */
    @ApiOperation("??????id?????????????????????")
    @PutMapping(value = "", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ModuleDef(
        moduleName = BizOpConstants.Modules.CONFIG_CENTER,
        subModuleName = BizOpConstants.SubModules.BLACKLIST,
        logMsgKey = BizOpConstants.Message.MESSAGE_BLACKLIST_UPDATE
    )
    @AuthVerification(
        moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
        needAuth = ActionTypeEnum.UPDATE
    )
    public BlacklistStringResponse update(@RequestBody BlacklistUpdateRequest request) {
        if(StringUtils.isBlank(request.getRedisKey()) || StringUtils.isBlank(request.getRedisKey().trim())) {
            throw new TroWebException(ExceptionCode.BLACKLIST_UPDATE_ERROR,"???????????????????????????");
        }
        OperationLogContextHolder.operationType(BizOpConstants.OpTypes.UPDATE);
        // ????????? redis
        OperationLogContextHolder.addVars(BizOpConstants.Vars.BLACKLIST_TYPE, BlacklistTypeEnum.getDescByType(0));
        OperationLogContextHolder.addVars(BizOpConstants.Vars.BLACKLIST_VALUE, request.getRedisKey().trim());
        BlacklistUpdateInput input = new BlacklistUpdateInput();
        BeanUtils.copyProperties(request,input);
        // ????????????
        input.setRedisKey(input.getRedisKey().trim());
        blacklistService.update(input);
        return new BlacklistStringResponse("????????????");
    }

    /**
     * ????????????????????????
     * @param request
     * @return
     */
    @ApiOperation("??????id???????????????????????????")
    @PutMapping(value = "/enable", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ModuleDef(
        moduleName = BizOpConstants.Modules.CONFIG_CENTER,
        subModuleName = BizOpConstants.SubModules.BLACKLIST,
        logMsgKey = BizOpConstants.Message.MESSAGE_BLACKLIST_ACTION
    )
    @AuthVerification(
        moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
        needAuth = ActionTypeEnum.ENABLE_DISABLE
    )
    public BlacklistStringResponse enable(@RequestBody @Valid BlacklistEnableRequest request) {
        String operationType = BlacklistEnableEnum.ENABLE.getStatus().equals(request.getUseYn())
            ? BizOpConstants.OpTypes.ENABLE : BizOpConstants.OpTypes.DISABLE;
        OperationLogContextHolder.operationType(operationType);
        BlacklistUpdateInput input = new BlacklistUpdateInput();
        BeanUtils.copyProperties(request,input);
        blacklistService.enable(input);
        return new BlacklistStringResponse(operationType+"??????");
    }

    /**
     * ??????????????????
     * @param request
     * @return
     */
    @ApiOperation("?????????????????????????????????")
    @PutMapping(value = "list/enable", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ModuleDef(
        moduleName = BizOpConstants.Modules.CONFIG_CENTER,
        subModuleName = BizOpConstants.SubModules.BLACKLIST,
        logMsgKey = Message.MESSAGE_BLACKLIST_BATCH_ACTION
    )
    @AuthVerification(
        moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
        needAuth = ActionTypeEnum.ENABLE_DISABLE
    )
    public BlacklistStringResponse listEnable(@RequestBody BlacklistBatchEnableRequest request) {
        String operationType = BlacklistEnableEnum.ENABLE.getStatus().equals(request.getUseYn())
            ? BizOpConstants.OpTypes.ENABLE : BizOpConstants.OpTypes.DISABLE;
        blacklistService.batchEnable(request.getIds(),request.getUseYn());
        return new BlacklistStringResponse("??????"+ operationType+"??????");
    }

    /**
     * ???????????????????????????
     * @param request
     * @return
     */
    @ApiOperation("???????????????????????????")
    @DeleteMapping(value = "/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ModuleDef(
        moduleName = BizOpConstants.Modules.CONFIG_CENTER,
        subModuleName = BizOpConstants.SubModules.BLACKLIST,
        logMsgKey = Message.MESSAGE_BLACKLIST_BATCH_DELETE
    )
    @AuthVerification(
        moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
        needAuth = ActionTypeEnum.DELETE
    )
    public BlacklistStringResponse batchDelete(@RequestBody @Valid BlacklistBatchDeleteRequest request) {
        if(request.getIds() == null || request.getIds().size() == 0) {
            throw new TroWebException(ExceptionCode.BLACKLIST_DELETE_ERROR,"id??????????????????");
        }
        OperationLogContextHolder.operationType(BizOpConstants.OpTypes.DELETE);
        blacklistService.batchDelete(request.getIds());
        return new BlacklistStringResponse("??????????????????");

    }

    /**
     * ???????????????????????????
     * @param request
     * @return
     */
    @ApiOperation( "???????????????????????????")
    @DeleteMapping(value = "", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ModuleDef(
        moduleName = BizOpConstants.Modules.CONFIG_CENTER,
        subModuleName = BizOpConstants.SubModules.BLACKLIST,
        logMsgKey = BizOpConstants.Message.MESSAGE_BLACKLIST_DELETE
    )
    @AuthVerification(
        moduleCode = BizOpConstants.ModuleCode.APPLICATION_MANAGE,
        needAuth = ActionTypeEnum.DELETE
    )
    public BlacklistStringResponse delete(@RequestBody @Valid BlacklistDeleteRequest request) {
        OperationLogContextHolder.operationType(BizOpConstants.OpTypes.DELETE);
        blacklistService.delete(request.getId());
        return new BlacklistStringResponse("????????????");
    }




}
