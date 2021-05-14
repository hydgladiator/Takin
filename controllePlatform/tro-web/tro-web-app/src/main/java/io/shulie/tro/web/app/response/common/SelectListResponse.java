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

package io.shulie.tro.web.app.response.common;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName SelectListResponse
 * @Description
 * @Author qianshui
 * @Date 2020/11/11 上午10:55
 */
@Data
@ApiModel("下拉框返回值")
public class SelectListResponse implements Serializable {

    private static final long serialVersionUID = 2145551334532174259L;

    @ApiModelProperty(value = "标签")
    private String label;

    @ApiModelProperty(value = "值")
    private String value;
}