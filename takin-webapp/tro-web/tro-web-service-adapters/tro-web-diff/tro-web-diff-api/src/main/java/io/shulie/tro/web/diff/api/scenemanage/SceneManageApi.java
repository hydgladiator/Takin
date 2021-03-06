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

package io.shulie.tro.web.diff.api.scenemanage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import io.shulie.tro.cloud.open.req.engine.EnginePluginDetailsWrapperReq;
import io.shulie.tro.cloud.open.req.engine.EnginePluginFetchWrapperReq;
import io.shulie.tro.cloud.open.req.report.ReportAllocationUserReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneAllocationUserReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneIpNumReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageDeleteReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageIdReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageQueryByIdsReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageQueryReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneManageWrapperReq;
import io.shulie.tro.cloud.open.req.scenemanage.SceneParseReq;
import io.shulie.tro.cloud.open.req.scenemanage.UpdateSceneFileRequest;
import io.shulie.tro.cloud.open.resp.engine.EnginePluginDetailResp;
import io.shulie.tro.cloud.open.resp.engine.EnginePluginSimpleInfoResp;
import io.shulie.tro.cloud.open.resp.scenemanage.SceneManageListResp;
import io.shulie.tro.cloud.open.resp.scenemanage.SceneManageWrapperResp;
import io.shulie.tro.cloud.open.resp.strategy.StrategyResp;
import io.shulie.tro.common.beans.response.ResponseResult;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author ??????
 * @Package io.shulie.tro.web.diff.api.scenemanage
 * @date 2020/10/27 4:22 ??????
 */
@Valid
public interface SceneManageApi {

    /**
     * ??????????????????id, ?????????????????????????????????????????????
     *
     * @param updateSceneFileRequest ????????????
     * @return
     */
    ResponseResult updateSceneFileByScriptId(@Validated UpdateSceneFileRequest updateSceneFileRequest);

    /**
     * ??????
     *
     * @param sceneManageWrapperReq
     * @return
     */
    ResponseResult<Long> saveScene(SceneManageWrapperReq sceneManageWrapperReq);

    /**
     * ??????
     *
     * @param sceneManageWrapperReq
     * @return
     */
    ResponseResult updateScene(SceneManageWrapperReq sceneManageWrapperReq);

    /**
     * ??????
     *
     * @param sceneManageDeleteReq
     * @return
     */
    ResponseResult deleteScene(SceneManageDeleteReq sceneManageDeleteReq);

    /**
     * ?????????????????? ???????????????
     *
     * @param sceneManageIdVO
     * @return
     */
    ResponseResult<SceneManageWrapperResp> getSceneDetail(SceneManageIdReq sceneManageIdVO);

    /**
     * ????????????????????????
     *
     * @param sceneManageQueryReq
     * @return
     */
    ResponseResult<List<SceneManageListResp>> getSceneList(SceneManageQueryReq sceneManageQueryReq);

    /**
     * ????????????
     *
     * @param sceneManageWrapperReq
     * @return
     */
    ResponseResult<BigDecimal> calcFlow(SceneManageWrapperReq sceneManageWrapperReq);

    /**
     * ????????????????????????
     *
     * @param sceneIpNumReq
     * @return
     */
    ResponseResult<StrategyResp> getIpNum(SceneIpNumReq sceneIpNumReq);

    /**
     * ????????????
     *
     * @param sceneParseReq
     * @return
     */
    ResponseResult<Map<String, Object>> parseScript(SceneParseReq sceneParseReq);

    /**
     * ????????????
     *
     * @param sceneParseReq
     * @return
     */
    ResponseResult<Map<String, Object>> parseAndUpdateScript(SceneParseReq sceneParseReq);

    /**
     * ???????????????????????????
     *
     * @return
     */
    ResponseResult<List<SceneManageListResp>> getSceneManageList();

    /**
     * ????????????-???????????????
     *
     * @param req
     * @return
     */
    ResponseResult allocationSceneUser(SceneAllocationUserReq req);

    /**
     * ????????????-???????????????
     *
     * @param req
     * @return
     */
    ResponseResult allocationReportUser(ReportAllocationUserReq req);

    ResponseResult<List<SceneManageWrapperResp>> getByIds(SceneManageQueryByIdsReq req);
    /**
     * ???????????????jmeter????????????
     *
     * @param wrapperReq
     * @return
     */
    ResponseResult<Map<String, List<EnginePluginSimpleInfoResp>>> listEnginePlugins(
        EnginePluginFetchWrapperReq wrapperReq);

    /**
     * ???????????????jmeter????????????
     *
     * @param wrapperReq
     * @return
     */
    ResponseResult<EnginePluginDetailResp> getEnginePluginDetails(EnginePluginDetailsWrapperReq wrapperReq);

}
