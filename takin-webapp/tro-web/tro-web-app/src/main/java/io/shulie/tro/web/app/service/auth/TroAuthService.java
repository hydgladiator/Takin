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

package io.shulie.tro.web.app.service.auth;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.pagehelper.PageInfo;
import com.pamirs.tro.entity.domain.entity.auth.TreeDeptModel;
import com.pamirs.tro.entity.domain.entity.user.DeptUser;
import com.pamirs.tro.entity.domain.entity.user.DeptUserQueryParam;
import com.pamirs.tro.entity.domain.entity.user.User;
import com.pamirs.tro.entity.domain.vo.user.UserLoginParam;
import io.shulie.tro.common.beans.page.PagingList;
import io.shulie.tro.web.app.common.Response;
import io.shulie.tro.web.app.input.user.UserQueryInput;
import io.shulie.tro.web.app.output.user.UserQueryOutput;
import io.shulie.tro.web.app.request.user.ResourceActionDeleteRequest;
import io.shulie.tro.web.app.request.user.ResourceActionQueryRequest;
import io.shulie.tro.web.app.request.user.ResourceActionUpdateRequest;
import io.shulie.tro.web.app.request.user.ResourceScopeQueryRequest;
import io.shulie.tro.web.app.request.user.ResourceScopeUpdateRequest;
import io.shulie.tro.web.app.response.user.ResourceActionResponse;
import io.shulie.tro.web.app.response.user.ResourceScopeResponse;
import io.shulie.tro.web.auth.api.exception.TroAuthException;
import io.shulie.tro.web.auth.api.exception.TroLoginException;
import io.shulie.tro.web.data.param.user.UserDeptConditionQueryParam;
import io.shulie.tro.web.data.param.user.UserDeptQueryParam;
import io.shulie.tro.web.data.result.user.UserDeptConditionResult;
import io.shulie.tro.web.data.result.user.UserDeptResult;

/**
 * @Author: fanxx
 * @Date: 2020/9/3 ??????2:35
 * @Description:
 */
public interface TroAuthService {

    /**
     * ??????????????????
     *
     * @param input
     * @return
     */
    PagingList<UserQueryOutput> selectUserByCondition(UserQueryInput input);

    /**
     * ????????????????????????
     *
     * @param deptName
     * @return
     */
    List<TreeDeptModel> getDeptTree(String deptName);

    /**
     * ??????????????????
     *
     * @param param
     * @return
     */
    PageInfo<DeptUser> getDeptUser(DeptUserQueryParam param);

    /**
     * ??????????????????????????????
     *
     * @return
     */
    Map<String, Boolean> getUserMenu();

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    Map<String, Boolean> getUserAction();

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    Map<String, List<Integer>> getUserData();

    /**
     * ??????????????????
     *
     * @param queryRequest
     * @return
     */
    List<ResourceActionResponse> getResourceAction(ResourceActionQueryRequest queryRequest);

    /**
     * ????????????????????????
     *
     * @param updateRequest
     * @return
     */
    Response updateResourceAction(ResourceActionUpdateRequest updateRequest);

    /**
     * ??????????????????
     *
     * @param queryRequest
     * @return
     */
    List<ResourceScopeResponse> getResourceScope(ResourceScopeQueryRequest queryRequest);

    /**
     * ??????????????????
     *
     * @param updateRequest
     * @return
     */
    Response updateResourceScope(ResourceScopeUpdateRequest updateRequest);

    /**
     * ??????????????????
     *
     * @param deleteRequestList
     */
    void deleteResourceAction(List<ResourceActionDeleteRequest> deleteRequestList);

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param user
     * @return
     */
    List<String> getUserMenuResource(User user);

    /**
     * ????????????id????????????????????????
     *
     * @param queryParam
     * @return
     */
    List<UserDeptResult> selectList(UserDeptQueryParam queryParam);

    /**
     * ????????????id??????????????????
     *
     * @param queryParam
     * @return
     */
    List<UserDeptConditionResult> selectUserByDeptIds(UserDeptConditionQueryParam queryParam);

    /**
     * ????????????
     *
     * @param request
     * @param user
     * @return
     * @throws TroAuthException
     */
    User login(HttpServletRequest request, UserLoginParam user) throws TroLoginException;

    /**
     * ????????????
     *
     * @param request
     * @return
     * @throws TroAuthException
     */
    String logout(HttpServletRequest request, HttpServletResponse response) throws TroLoginException;

    /**
     * ?????????
     *
     * @param request
     * @return
     */
    void redirect(String redirectUrl, HttpServletRequest request, HttpServletResponse response);

    /**
     * ??????????????????
     *
     * @param user
     */
    void loginSuccess(User user);

    /**
     * ????????????????????????
     *
     * @param request
     * @param response
     * @return
     */
    User check(HttpServletRequest request, HttpServletResponse response);

    /**
     * ??????????????????
     *
     * @param response
     */
    void checkFail(HttpServletResponse response);

    /**
     * ????????????????????????????????????
     *
     * @param user
     * @param url
     * @return
     */
    boolean hasPermissionUrl(User user, String url);
}
