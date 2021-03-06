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

package io.shulie.amdb.adaptors.utils;

public class FlagUtil {

    /**
     * 修改进制位
     * @param flag
     * @param site
     * @param b
     * @return
     */
    public static Integer setFlag(int flag, int site, boolean b) {
        if ((flag & site) == site) {
            if (!b) {
                return flag ^ site;
            }
        }else{
            if(b){
                return flag|site;
            }
        }
        return flag;
    }

    public static void main(String[] args) {
        System.out.println(setFlag(0,2,true));
        System.out.println(setFlag(0,2,false));
        System.out.println(setFlag(3,2,true));
        System.out.println(setFlag(3,2,false));
    }
}
