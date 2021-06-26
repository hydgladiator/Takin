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

package com.pamirs.tro.entity.domain.entity;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pamirs.tro.common.util.DateToStringFormatSerialize;
import com.pamirs.tro.common.util.LongToStringFormatSerialize;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @author shulie
 * @description MQ消费信息
 * @create 2018/7/30 15:44
 */
public class TMqMsg implements Serializable {
    /**
     * 　主键id
     * JsonSerialize这个注解是在反序列化时将其转换成String类型吗
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.MSG_ID
     *
     * @mbg.generated
     */
    @JsonSerialize(using = LongToStringFormatSerialize.class)
    private Long msgId;

    /**
     * 消息类型
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.MSG_TYPE
     *
     * @mbg.generated
     */
    private String msgType;

    /**
     * 字典类型
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.DICT_TYPE
     *
     * @mbg.generated
     */
    private String dictType;

    /**
     * 消息地址
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.MSG_HOST
     *
     * @mbg.generated
     */
    @Pattern(regexp = "((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))",
        message = "IP格式错误,请填写正确的IP地址")
    private String msgHost;

    /**
     * 消息集群ip
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.msgIp
     *
     * @mbg.generated
     */
    private String msgIp;

    /**
     * 消息端口
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.MSG_PORT
     *
     * @mbg.generated
     */

    //    @Pattern(regexp ="^[1-9]\\d{0,4}$",message = "消息端口为1-5位数字!")
    //    @Range(min = 1, max = 65535, message = "消息端口为1-65535之内!")
    private String msgPort;

    /**
     * 订阅主题
     */
    //    @Pattern(regexp = "(PT_\\w+\\|?)+", message = "订阅主题topic必须以PT_开头,如果多个请以|分隔!")
    private String topic;

    /**
     * 组名
     */
    private String groupName;

    /**
     * 队列通道
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.QUEUE_CHANNEL
     *
     * @mbg.generated
     */
    //@NotBlank(message = "队列通道不能为空")
    private String queueChannel;

    /**
     * 队列管理
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.QUEUE_MANGER
     *
     * @mbg.generated
     */
    //@NotBlank(message = "队列管理不能为空")
    private String queueManager;

    /**
     * 编码字符集标识符
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.CCSID
     *
     * @mbg.generated
     */
    private String ccsid;

    /**
     * 基础队列名称
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.BASE_QUEUE_NAME
     *
     * @mbg.generated
     */
    private String baseQueueName;

    /**
     * 传输类型
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.TRANSPORT_TYPE
     *
     * @mbg.generated
     */
    private String transportType;

    /**
     * esbCode编码
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.ESBCODE
     *
     * @mbg.generated2344|34||||
     */
    @Pattern(regexp = "(PT_\\w+\\|?)+", message = "esbcode必须以PT_开头,如果多个请以|分隔!")
    private String esbcode;

    /**
     * 创建时间
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.CREATE_TIME
     *
     * @mbg.generated
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = DateToStringFormatSerialize.class)
    private Date createTime;

    /**
     * 更新时间
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.UPDATE_TIME
     *
     * @mbg.generated
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = DateToStringFormatSerialize.class)
    private Date updateTime;

    /**
     * 消费开始时间
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.UPDATE_TIME
     *
     * @mbg.generated
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = DateToStringFormatSerialize.class)
    private Date consumeStartTime;

    /**
     * 消费结束时间
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.UPDATE_TIME
     *
     * @mbg.generated
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = DateToStringFormatSerialize.class)
    private Date consumeEndTime;

    /**
     * 上次消费时间
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.UPDATE_TIME
     *
     * @mbg.generated
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = DateToStringFormatSerialize.class)
    private Date lastConsumeTime;

    /**
     * 消费状态 0未消费 1正在消费 2已消费
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_mq_msg.UPDATE_TIME
     *
     * @mbg.generated
     */
    private String consumeStatus;

    public String getMsgIp() {
        return msgIp;
    }

    public void setMsgIp(String msgIp) {
        this.msgIp = msgIp;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.MSG_ID
     *
     * @return the value of t_mq_msg.MSG_ID
     * @mbg.generated
     */
    public Long getMsgId() {
        return msgId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.MSG_ID
     *
     * @param msgId the value for t_mq_msg.MSG_ID
     * @mbg.generated
     */
    public void setMsgId(Long msgId) {
        this.msgId = msgId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.MSG_TYPE
     *
     * @return the value of t_mq_msg.MSG_TYPE
     * @mbg.generated
     */
    public String getMsgType() {
        return msgType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.MSG_TYPE
     *
     * @param msgType the value for t_mq_msg.MSG_TYPE
     * @mbg.generated
     */
    public void setMsgType(String msgType) {
        this.msgType = msgType == null ? null : msgType.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.DICT_TYPE
     *
     * @return the value of t_mq_msg.DICT_TYPE
     * @mbg.generated
     */
    public String getDictType() {
        return dictType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.DICT_TYPE
     *
     * @param dictType the value for t_mq_msg.DICT_TYPE
     * @mbg.generated
     */
    public void setDictType(String dictType) {
        this.dictType = dictType == null ? null : dictType.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.MSG_HOST
     *
     * @return the value of t_mq_msg.MSG_HOST
     * @mbg.generated
     */
    public String getMsgHost() {
        return msgHost;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.MSG_HOST
     *
     * @param msgHost the value for t_mq_msg.MSG_HOST
     * @mbg.generated
     */
    public void setMsgHost(String msgHost) {
        this.msgHost = msgHost == null ? null : msgHost.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.MSG_PORT
     *
     * @return the value of t_mq_msg.MSG_PORT
     * @mbg.generated
     */
    public String getMsgPort() {
        return msgPort;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.MSG_PORT
     *
     * @param msgPort the value for t_mq_msg.MSG_PORT
     * @mbg.generated
     */
    public void setMsgPort(String msgPort) {
        this.msgPort = msgPort == null ? null : msgPort.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.QUEUE_CHANNEL
     *
     * @return the value of t_mq_msg.QUEUE_CHANNEL
     * @mbg.generated
     */
    public String getQueueChannel() {
        return queueChannel;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.QUEUE_CHANNEL
     *
     * @param queueChannel the value for t_mq_msg.QUEUE_CHANNEL
     * @mbg.generated
     */
    public void setQueueChannel(String queueChannel) {
        this.queueChannel = queueChannel == null ? null : queueChannel.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.QUEUE_MANAGER
     *
     * @return the value of t_mq_msg.QUEUE_MANAGER
     * @mbg.generated
     */
    public String getQueueManager() {
        return queueManager;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.QUEUE_MANAGER
     *
     * @param queueManager the value for t_mq_msg.QUEUE_MANAGER
     * @mbg.generated
     */
    public void setQueueManager(String queueManager) {
        this.queueManager = queueManager == null ? null : queueManager.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.CCSID
     *
     * @return the value of t_mq_msg.CCSID
     * @mbg.generated
     */
    public String getCcsid() {
        return ccsid;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.CCSID
     *
     * @param ccsid the value for t_mq_msg.CCSID
     * @mbg.generated
     */
    public void setCcsid(String ccsid) {
        this.ccsid = ccsid == null ? null : ccsid.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.BASE_QUEUE_NAME
     *
     * @return the value of t_mq_msg.BASE_QUEUE_NAME
     * @mbg.generated
     */
    public String getBaseQueueName() {
        return baseQueueName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.BASE_QUEUE_NAME
     *
     * @param baseQueueName the value for t_mq_msg.BASE_QUEUE_NAME
     * @mbg.generated
     */
    public void setBaseQueueName(String baseQueueName) {
        this.baseQueueName = baseQueueName == null ? null : baseQueueName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.TRANSPORT_TYPE
     *
     * @return the value of t_mq_msg.TRANSPORT_TYPE
     * @mbg.generated
     */
    public String getTransportType() {
        return transportType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.TRANSPORT_TYPE
     *
     * @param transportType the value for t_mq_msg.TRANSPORT_TYPE
     * @mbg.generated
     */
    public void setTransportType(String transportType) {
        this.transportType = transportType == null ? null : transportType.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.ESBCODE
     *
     * @return the value of t_mq_msg.ESBCODE
     * @mbg.generated
     */
    public String getEsbcode() {
        return esbcode;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.ESBCODE
     *
     * @param esbcode the value for t_mq_msg.ESBCODE
     * @mbg.generated
     */
    public void setEsbcode(String esbcode) {
        this.esbcode = esbcode == null ? null : esbcode.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.CREATE_TIME
     *
     * @return the value of t_mq_msg.CREATE_TIME
     * @mbg.generated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.CREATE_TIME
     *
     * @param createTime the value for t_mq_msg.CREATE_TIME
     * @mbg.generated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_mq_msg.UPDATE_TIME
     *
     * @return the value of t_mq_msg.UPDATE_TIME
     * @mbg.generated
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_mq_msg.UPDATE_TIME
     *
     * @param updateTime the value for t_mq_msg.UPDATE_TIME
     * @mbg.generated
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * @return java.util.Date
     * @description 获取消费开始时间
     * @author shulie
     * @create 2018/8/1 14:11
     */
    public Date getConsumeStartTime() {
        return consumeStartTime;
    }

    /**
     * @param consumeStartTime 消费开始时间
     * @return void
     * @description 设置消费开始时间
     * @author shulie
     * @create 2018/8/1 14:11
     */
    public void setConsumeStartTime(Date consumeStartTime) {
        this.consumeStartTime = consumeStartTime;
    }

    /**
     * @return java.util.Date
     * @description 获取消费结束时间
     * @author shulie
     * @create 2018/8/1 14:11
     */
    public Date getConsumeEndTime() {
        return consumeEndTime;
    }

    /**
     * @param consumeEndTime 消费结束时间
     * @return void
     * @description 设置消费结束时间
     * @author shulie
     * @create 2018/8/1 14:12
     */
    public void setConsumeEndTime(Date consumeEndTime) {
        this.consumeEndTime = consumeEndTime;
    }

    /**
     * @return java.util.Date
     * @description 获取上次消费时间
     * @author shulie
     * @create 2018/8/1 14:12
     */
    public Date getLastConsumeTime() {
        return lastConsumeTime;
    }

    /**
     * @param lastConsumeTime 上次消费时间
     * @return void
     * @description 设置上次消费时间
     * @author shulie
     * @create 2018/8/1 14:13
     */
    public void setLastConsumeTime(Date lastConsumeTime) {
        this.lastConsumeTime = lastConsumeTime;
    }

    /**
     * @return java.util.Date
     * @description 获取消费状态
     * @author shulie
     * @create 2018/8/1 14:13
     */
    public String getConsumeStatus() {
        return consumeStatus;
    }

    /**
     * @param consumeStatus 消费状态
     * @return void
     * @description 设置消费状态
     * @author shulie
     * @create 2018/8/1 14:14
     */
    public void setConsumeStatus(String consumeStatus) {
        this.consumeStatus = consumeStatus;
    }

    /**
     * @return java.lang.String
     * @description 重写toString()方法
     * @author shulie
     * @create 2018/8/1 14:14
     */
    @Override
    public String toString() {
        return "TMqMsg{" +
            "msgId=" + msgId +
            ", msgType='" + msgType + '\'' +
            ", dictType='" + dictType + '\'' +
            ", msgHost='" + msgHost + '\'' +
            ", msgIp='" + msgIp + '\'' +
            ", msgPort='" + msgPort + '\'' +
            ", topic='" + topic + '\'' +
            ", groupName='" + groupName + '\'' +
            ", queueChannel='" + queueChannel + '\'' +
            ", queueManager='" + queueManager + '\'' +
            ", ccsid='" + ccsid + '\'' +
            ", baseQueueName='" + baseQueueName + '\'' +
            ", transportType='" + transportType + '\'' +
            ", esbcode='" + esbcode + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            ", consumeStartTime=" + consumeStartTime +
            ", consumeEndTime=" + consumeEndTime +
            ", lastConsumeTime=" + lastConsumeTime +
            ", consumeStatus='" + consumeStatus + '\'' +
            '}';
    }
}