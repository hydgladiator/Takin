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

package io.shulie.tro.web.app.service.user;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.pamirs.tro.entity.domain.entity.user.User;
import com.pamirs.tro.entity.domain.vo.user.UserLoginParam;
import io.shulie.tro.web.app.common.Response;
import io.shulie.tro.web.app.common.RestContext;
import io.shulie.tro.web.app.exception.ExceptionCode;
import io.shulie.tro.web.app.exception.TroWebException;
import io.shulie.tro.web.app.license.TroClientValidator;
import io.shulie.tro.web.app.output.user.TroUserOutput;
import io.shulie.tro.web.app.response.user.UserLoginResponse;
import io.shulie.tro.web.app.response.user.UserLogoutResponse;
import io.shulie.tro.web.app.service.auth.TroAuthService;
import io.shulie.tro.web.app.utils.TroUserUtil;
import io.shulie.tro.web.auth.api.exception.TroAuthException;
import io.shulie.tro.web.auth.api.exception.TroLoginException;
import io.shulie.tro.web.common.constant.TroClientAuthConstant;
import io.shulie.tro.web.common.util.verificationcode.VerificationCodeUtil;
import io.shulie.tro.web.data.dao.user.LoginRecordDao;
import io.shulie.tro.web.data.mapper.mysql.TroUserMapper;
import io.shulie.tro.web.data.model.mysql.TroUserEntity;
import io.shulie.tro.web.data.param.user.LoginRecordParam;
import io.shulie.tro.web.data.param.user.UserDeptQueryParam;
import io.shulie.tro.web.data.result.user.UserDeptResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: fanxx
 * @Date: 2020/3/24 ??????8:45
 * @Description:
 */
@Component
@Slf4j
public class TroUserService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TroAuthService troAuthService;

    @Resource
    private TroUserMapper troUserMapper;

    @Autowired
    private LoginRecordDao loginRecordDao;


    public Response login(UserLoginParam userLoginParam, HttpServletRequest request, HttpServletResponse response) {
        try {
            // ??????????????????
            saveLoginRecord(userLoginParam,request);
            // ???????????????
            //checkCode(request,userLoginParam.getCode());
            User user = troAuthService.login(request, userLoginParam);
            UserDeptQueryParam userDeptQueryParam = new UserDeptQueryParam();
            userDeptQueryParam.setUserIdList(Arrays.asList(String.valueOf(user.getId())));
            List<UserDeptResult> userDeptResultList = troAuthService.selectList(userDeptQueryParam);
            List<UserDeptResult> userDeptResults = Optional.ofNullable(userDeptResultList).orElse(Lists.newArrayList());
            if (CollectionUtils.isNotEmpty(userDeptResults)) {
                user.setDeptList(userDeptResults.get(0).getDeptList());
            }
            TroUserEntity admin = getAdminUser(user.getCustomerId());
            if(admin != null) {
                user.setCustomerId(admin.getId());
                user.setCustomerKey(admin.getKey());
            }
            log.warn("Login User->Admin, userId={}, adminId={}, adminkey={}", user.getId(), user.getCustomerId(),
                user.getCustomerKey());
            syncAuth(user);
            troAuthService.loginSuccess(user);
            UserLoginResponse userLoginResponse = new UserLoginResponse();
            userLoginResponse.setId(user.getId());
            userLoginResponse.setKey(user.getKey());
            userLoginResponse.setName(user.getName());
            userLoginResponse.setUserType(user.getUserType());
            userLoginResponse.setXToken(user.getXToken());
            userLoginResponse.setExpire(TroClientAuthConstant.isExpire);
            // ??????tro-cloud????????????
            TroClientValidator.validate(user.getKey());
            return Response.success(userLoginResponse);
        } catch (TroLoginException e) {
            log.error(e.getMessage(), e);
            return Response.fail("300", e.getMessage(), null);
        }
    }

    private void saveLoginRecord(UserLoginParam userLoginParam, HttpServletRequest request) {
        String ipAddress = getIpAddr(request);
        LoginRecordParam param = new LoginRecordParam();
        param.setUserName(userLoginParam.getUsername());
        param.setIp(ipAddress);
        loginRecordDao.insert(param);
    }

    private String getIpAddr(HttpServletRequest request) {
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if (ipAddress.equals("127.0.0.1")) {
                    // ??????????????????????????????IP
                    InetAddress inet = null;
                    try {
                        inet = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    ipAddress = inet.getHostAddress();
                }
            }
            // ?????????????????????????????????????????????IP??????????????????IP,??????IP??????','??????
            if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
                // = 15
                if (ipAddress.indexOf(",") > 0) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                }
            }
        } catch (Exception e) {
            ipAddress="";
        }
        // ipAddress = this.getRequest().getRemoteAddr();

        return ipAddress;
    }

    public Response loginNoCode(UserLoginParam userLoginParam, HttpServletRequest request, HttpServletResponse response) {
        try {

            User user = troAuthService.login(request, userLoginParam);
            UserDeptQueryParam userDeptQueryParam = new UserDeptQueryParam();
            userDeptQueryParam.setUserIdList(Arrays.asList(String.valueOf(user.getId())));
            List<UserDeptResult> userDeptResultList = troAuthService.selectList(userDeptQueryParam);
            List<UserDeptResult> userDeptResults = Optional.ofNullable(userDeptResultList).orElse(Lists.newArrayList());
            if (CollectionUtils.isNotEmpty(userDeptResults)) {
                user.setDeptList(userDeptResults.get(0).getDeptList());
            }
            TroUserEntity admin = getAdminUser(user.getCustomerId());
            if (admin != null) {
                user.setCustomerId(admin.getId());
                user.setCustomerKey(admin.getKey());
            }
            log.warn("Login User->Admin, userId={}, adminId={}, adminkey={}", user.getId(), user.getCustomerId(),
                user.getCustomerKey());
            syncAuth(user);
            troAuthService.loginSuccess(user);
            UserLoginResponse userLoginResponse = new UserLoginResponse();
            userLoginResponse.setId(user.getId());
            userLoginResponse.setKey(user.getKey());
            userLoginResponse.setName(user.getName());
            userLoginResponse.setUserType(user.getUserType());
            userLoginResponse.setXToken(user.getXToken());
            userLoginResponse.setExpire(TroClientAuthConstant.isExpire);
            // ??????tro-cloud????????????
            TroClientValidator.validate(user.getKey());
            return Response.success(userLoginResponse);
        } catch (TroLoginException e) {
            log.error(e.getMessage(), e);
            return Response.fail("300", e.getMessage(), null);
        }
    }

    private void checkCode(HttpServletRequest request, String code) {
        if (StringUtils.isBlank(code)) {
            throw new TroWebException(ExceptionCode.VERIFICATION_CODE_ERROR, "????????????????????????");
        }
        String accessToken = request.getHeader("Access-Token");
        HttpSession httpSession = VerificationCodeUtil.getSession(accessToken);
        if(httpSession == null) {
            throw new TroWebException(ExceptionCode.VERIFICATION_CODE_ERROR,  "??????????????????????????????");
        }
        Object sessionObject = httpSession.getAttribute(VerificationCodeUtil.VERIFICATION);
        if (null == sessionObject) {
            throw new TroWebException(ExceptionCode.VERIFICATION_CODE_ERROR,  "???????????????");
        }
        String sessionCode = sessionObject.toString();
        if (!code.equalsIgnoreCase(sessionCode)) {
            throw new TroWebException(ExceptionCode.VERIFICATION_CODE_ERROR,  "???????????????");
        }
        // ???????????????
        VerificationCodeUtil.delSession(accessToken);
    }

    public Response logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String indexUrl = troAuthService.logout(request, response);
            UserLogoutResponse userLogoutResponse = new UserLogoutResponse();
            userLogoutResponse.setIndexUrl(indexUrl);
            return Response.success(userLogoutResponse);
        } catch (TroLoginException e) {
            log.error(e.getMessage(), e);
            return Response.fail("300", e.getMessage(), null);
        }
    }

    public void redirect(String redirectUrl, HttpServletRequest request, HttpServletResponse response) {
        troAuthService.redirect(redirectUrl, request, response);
    }

    public Response logout(HttpServletRequest request) {
        String token = request.getHeader("x-token");
        redisTemplate.opsForValue().set(token, null);
        return Response.success("????????????");
    }

    public void syncAuth(User user) {
        //????????????????????????????????????
        List<String> urlList = troAuthService.getUserMenuResource(user);
        if (!TroUserUtil.validateSuperAdmin(user) && CollectionUtils.isEmpty(urlList)) {
            String msg = "????????????????????????????????????????????????" + user.getName();
            log.error(msg);
            throw new TroAuthException(ExceptionCode.ALL_PERMISSION_DENY_ERROR, msg);
        }
        //??????user?????????????????????????????????
        RestContext.setUser(user);
        Map<String, Boolean> menuMap = troAuthService.getUserMenu();
        Map<String, Boolean> actionMap = troAuthService.getUserAction();
        Map<String, List<Integer>> dataMap = troAuthService.getUserData();
        user.setPermissionUrl(urlList);
        user.setPermissionMenu(menuMap);
        user.setPermissionAction(actionMap);
        user.setPermissionData(dataMap);
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    private TroUserEntity getAdminUser(Long customerId) {
        return troUserMapper.selectById(customerId);
    }

    public TroUserOutput queryUserByKey(String userAppKey) {
        LambdaQueryWrapper<TroUserEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(TroUserEntity::getKey, userAppKey)
            .eq(TroUserEntity::getIsDelete, 0);
        List<TroUserEntity> troUserEntities = troUserMapper.selectList(lambdaQueryWrapper);
        if (CollectionUtils.isEmpty(troUserEntities)) {
            return null;
        }
        if (troUserEntities.size() > 1) {
            log.error("{}??????????????????????????????????????????", userAppKey);
        }
        TroUserOutput troUserOutput = new TroUserOutput();
        BeanUtils.copyProperties(troUserEntities.get(0), troUserOutput);
        return troUserOutput;
    }
}
