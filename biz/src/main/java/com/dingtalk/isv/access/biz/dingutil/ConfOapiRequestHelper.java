package com.dingtalk.isv.access.biz.dingutil;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.isv.access.api.model.corp.CorpAuthInfoVO;
import com.dingtalk.isv.access.api.model.corp.StaffVO;
import com.dingtalk.isv.access.api.model.corp.CorpChannelJSAPITicketVO;
import com.dingtalk.isv.access.api.model.corp.CorpChannelTokenVO;
import com.dingtalk.isv.access.api.model.corp.CorpJSAPITicketVO;
import com.dingtalk.isv.access.api.model.corp.CorpVO;
import com.dingtalk.isv.access.api.model.suite.CorpSuiteAuthVO;
import com.dingtalk.isv.access.biz.corp.model.CorpChannelTokenDO;
import com.dingtalk.isv.common.code.ServiceResultCode;
import com.dingtalk.isv.common.log.format.LogFormatter;
import com.dingtalk.isv.common.model.ServiceResult;
import com.dingtalk.isv.common.util.HttpRequestHelper;
import com.dingtalk.open.client.api.model.isv.CorpAuthInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;
import java.util.List;
/**
 * 开放平台取conf或者token相关http接口封装
 * Created by lifeng.zlf on 2016/4/27.
 */
public class ConfOapiRequestHelper {
    private static Logger logger = LoggerFactory.getLogger(ConfOapiRequestHelper.class);
    private static final Logger bizLogger = LoggerFactory.getLogger("HTTP_INVOKE_LOGGER");
    private HttpRequestHelper httpRequestHelper;
    private String oapiDomain;
    private String sxk_url = "http://114.55.100.6:8080/sxk/";

    public String getOapiDomain() {
        return oapiDomain;
    }

    public void setOapiDomain(String oapiDomain) {
        this.oapiDomain = oapiDomain;
    }

    public HttpRequestHelper getHttpRequestHelper() {
        return httpRequestHelper;
    }

    public void setHttpRequestHelper(HttpRequestHelper httpRequestHelper) {
        this.httpRequestHelper = httpRequestHelper;
    }

    /**
     * 获取企业的jsapi ticket
     * @param suiteKey
     * @param corpId
     * @param accessToken
     * @return
     */
    public ServiceResult<CorpJSAPITicketVO> getJSTicket(String suiteKey, String corpId, String accessToken) {
        try {
            String url = getOapiDomain() + "/get_jsapi_ticket?access_token=" + accessToken;
            String sr = httpRequestHelper.doHttpGet(url);
            JSONObject jsonObject = JSON.parseObject(sr);
            Long errCode = jsonObject.getLong("errcode");
            if (Long.valueOf(0).equals(errCode)) {
                String ticket = jsonObject.getString("ticket");
                Long expires_in = jsonObject.getLong("expires_in");
                CorpJSAPITicketVO corpJSTicketVO = new CorpJSAPITicketVO();
                corpJSTicketVO.setCorpId(corpId);
                corpJSTicketVO.setSuiteKey(suiteKey);
                corpJSTicketVO.setCorpJSAPITicket(ticket);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.SECOND, expires_in.intValue());
                corpJSTicketVO.setExpiredTime(calendar.getTime());
                return ServiceResult.success(corpJSTicketVO);
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        } catch (Throwable e) {
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("accessToken", accessToken)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        }
    }


    /**
     * 获取永久授权码
     * @param suiteKey
     * @param tmpAuthCode
     * @param suiteAccessToken
     * @return
     */
    public ServiceResult<CorpSuiteAuthVO> getPermanentCode(String suiteKey,String tmpAuthCode, String suiteAccessToken) {
        try {
            String url = getOapiDomain() + "/service/get_permanent_code?suite_access_token=" + suiteAccessToken;
            Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("tmp_auth_code", tmpAuthCode);
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));
            JSONObject jsonObject = JSON.parseObject(sr);
            Long errCode = jsonObject.getLong("errcode");
            if (Long.valueOf(0).equals(errCode)) {
                JSONObject corpObject = jsonObject.getJSONObject("auth_corp_info");
                String chPcode = StringUtils.isEmpty(jsonObject.getString("ch_permanent_code"))?"":jsonObject.getString("ch_permanent_code");
                String pCode = StringUtils.isEmpty(jsonObject.getString("permanent_code"))?"":jsonObject.getString("permanent_code");
                String corpId = corpObject.getString("corpid");
                CorpSuiteAuthVO corpSuiteAuthVO = new CorpSuiteAuthVO();
                corpSuiteAuthVO.setSuiteKey(suiteKey);
                corpSuiteAuthVO.setChPermanentCode(chPcode);
                corpSuiteAuthVO.setPermanentCode(pCode);
                corpSuiteAuthVO.setCorpId(corpId);
                return ServiceResult.success(corpSuiteAuthVO);
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        } catch (Throwable e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("accessToken", suiteAccessToken)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        }
    }



    /**
     * 获取服务窗token
     * @param suiteKey
     * @param corpId
     * @param chPermanentCode
     * @param suiteAccessToken
     * @return
     */
    public ServiceResult<CorpChannelTokenVO> getCorpChannelToken(String suiteKey, String corpId, String chPermanentCode, String suiteAccessToken) {
        try {
            String url = getOapiDomain() + "/service/get_channel_corp_token?suite_access_token=" + suiteAccessToken;
            Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("auth_corpid", corpId);
            parmMap.put("ch_permanent_code", chPermanentCode);
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));
            JSONObject jsonObject = JSON.parseObject(sr);
            Long errCode = jsonObject.getLong("errcode");
            if (Long.valueOf(0).equals(errCode)) {
                String chAccessToken = jsonObject.getString("access_token");
                Long expiresIn = jsonObject.getLong("expires_in");
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.SECOND, expiresIn.intValue());
                CorpChannelTokenVO corpChannelTokenVO = new CorpChannelTokenVO();
                corpChannelTokenVO.setCorpId(corpId);
                corpChannelTokenVO.setSuiteKey(suiteKey);
                corpChannelTokenVO.setCorpChannelToken(chAccessToken);
                corpChannelTokenVO.setExpiredTime(calendar.getTime());
                return ServiceResult.success(corpChannelTokenVO);
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        } catch (Throwable e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("accessToken", suiteAccessToken)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        }
    }




    /**
     * 获取企业的jsapi ticket
     * @param suiteKey
     * @param corpId
     * @param accessToken
     * @return
     */
    public ServiceResult<CorpChannelJSAPITicketVO> getChannelJsapiTicket(String suiteKey, String corpId, String accessToken) {
        try {
            String url = getOapiDomain() + "/channel/get_channel_jsapi_ticket?access_token=" + accessToken;
            String sr = httpRequestHelper.doHttpGet(url);
            JSONObject jsonObject = JSON.parseObject(sr);
            Long errCode = jsonObject.getLong("errcode");
            if (Long.valueOf(0).equals(errCode)) {
                String ticket = jsonObject.getString("ticket");
                Long expires_in = jsonObject.getLong("expires_in");
                CorpChannelJSAPITicketVO corpJSTicketVO = new CorpChannelJSAPITicketVO();
                corpJSTicketVO.setCorpId(corpId);
                corpJSTicketVO.setSuiteKey(suiteKey);
                corpJSTicketVO.setCorpChannelJSAPITicket(ticket);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.SECOND, expires_in.intValue());
                corpJSTicketVO.setExpiredTime(calendar.getTime());
                return ServiceResult.success(corpJSTicketVO);
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        } catch (Throwable e) {
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("accessToken", accessToken)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        }
    }




    /**
     * 获取企业的授权信息
     * @param suiteKey
     * @param corpId
     * @param suiteAccessToken
     * @return
     */
    public ServiceResult<CorpAuthInfoVO> getAuthInfo(String suiteKey, String corpId, String suiteAccessToken) {
        try {
            String url = getOapiDomain() + "/service/get_auth_info?suite_access_token=" + suiteAccessToken;

            Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("suite_key", suiteKey);
            parmMap.put("auth_corpid", corpId);
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));

            JSONObject jsonObject = JSON.parseObject(sr);
            Long errCode = jsonObject.getLong("errcode");
            if (Long.valueOf(0).equals(errCode)) {
               CorpAuthInfoVO corpAuthInfoVO = JSON.parseObject(sr,CorpAuthInfoVO.class);
               return ServiceResult.success(corpAuthInfoVO);
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        } catch (Throwable e) {
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("suiteAccessToken", suiteAccessToken)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        }
    }




    /**
     * 注册事件回调
     * @param suiteKey
     * @param call_back_tag
     * @param accessToken
     * @return
     */

    public ServiceResult<String> getCallBack(String suiteKey, String call_back_tag, String accessToken,String token,String aes_key,String rtn_url){
        try {
        	
        	String url = getOapiDomain() + "/call_back/register_call_back?access_token=" + accessToken;
            Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("token", token);
            parmMap.put("aes_key", aes_key);
            parmMap.put("call_back_tag", call_back_tag);
            parmMap.put("url", rtn_url);
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));
            return ServiceResult.success(sr);
            //JSONObject jsonObject = JSON.parseObject(sr);
        	//查询已注册的回调事件   回调地址已不存在
//            String url = getOapiDomain() + "/call_back/get_call_back?access_token=" + accessToken;
//            //String url = "https://oapi.dingtalk.com/call_back/get_call_back?access_token=" + accessToken;
//            String sr = httpRequestHelper.doHttpGet(url);
//            return ServiceResult.success(sr);
            
//            JSONObject jsonObject = JSON.parseObject(sr);
//            Long errCode = jsonObject.getLong("errcode");
//            if (Long.valueOf(0).equals(errCode)) {
//                String ticket = jsonObject.getString("ticket");
//                Long expires_in = jsonObject.getLong("expires_in");
//                CorpJSAPITicketVO corpJSTicketVO = new CorpJSAPITicketVO();
//                corpJSTicketVO.setCorpId(corpId);
//                corpJSTicketVO.setSuiteKey(suiteKey);
//                corpJSTicketVO.setCorpJSAPITicket(ticket);
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(new Date());
//                calendar.add(Calendar.SECOND, expires_in.intValue());
//                corpJSTicketVO.setExpiredTime(calendar.getTime());
//                return ServiceResult.success(corpJSTicketVO);
//            }
            //return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        } catch (Throwable e) {
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("accessToken", accessToken)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        }
    }

    /**
     * 更新事件回调
     * @param suiteKey
     * @param call_back_tag
     * @param accessToken
     * @return
     */

    public ServiceResult<String> updateCallBack(String suiteKey, String call_back_tag, String accessToken,String token,String aes_key,String rtn_url){
        try {
        	
        	String url = getOapiDomain() + "/call_back/update_call_back?access_token=" + accessToken;
            Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("token", token);
            parmMap.put("aes_key", aes_key);
            parmMap.put("call_back_tag", call_back_tag);
            parmMap.put("url", rtn_url);
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));
            return ServiceResult.success(sr);
            //JSONObject jsonObject = JSON.parseObject(sr);
        	//查询已注册的回调事件   回调地址已不存在
//            String url = getOapiDomain() + "/call_back/get_call_back?access_token=" + accessToken;
//            //String url = "https://oapi.dingtalk.com/call_back/get_call_back?access_token=" + accessToken;
//            String sr = httpRequestHelper.doHttpGet(url);
//            return ServiceResult.success(sr);
            
//            JSONObject jsonObject = JSON.parseObject(sr);
//            Long errCode = jsonObject.getLong("errcode");
//            if (Long.valueOf(0).equals(errCode)) {
//                String ticket = jsonObject.getString("ticket");
//                Long expires_in = jsonObject.getLong("expires_in");
//                CorpJSAPITicketVO corpJSTicketVO = new CorpJSAPITicketVO();
//                corpJSTicketVO.setCorpId(corpId);
//                corpJSTicketVO.setSuiteKey(suiteKey);
//                corpJSTicketVO.setCorpJSAPITicket(ticket);
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(new Date());
//                calendar.add(Calendar.SECOND, expires_in.intValue());
//                corpJSTicketVO.setExpiredTime(calendar.getTime());
//                return ServiceResult.success(corpJSTicketVO);
//            }
            //return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        } catch (Throwable e) {
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("accessToken", accessToken)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        }
    }
    
    public String updateUser(String userId){
    	try
    	{
    		String url = sxk_url + "user";
    		//String sr = httpRequestHelper.doHttpGet(url);
    		Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("userId", userId);
            parmMap.put("userName", "zhangsam");
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));
    		return sr;
    	}
    	catch(Exception e){
    		return e.getMessage();
    	} 
    }
    
    public String addUser(String userId,String corpId,StaffVO staffVO)
    {
    	try
    	{
    		String url = sxk_url + "adduser";
    		Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("userId", userId);
            parmMap.put("corpId", corpId);
            parmMap.put("department", JSON.toJSONString(staffVO.getDepartment()));
            parmMap.put("name", JSON.toJSONString(staffVO.getName()));
            parmMap.put("tel", JSON.toJSONString(staffVO.getTel()));
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));
    		return sr;
    	}
    	catch(Exception e){
    		return e.getMessage();
    	} 
    }
    
    public String createCorp(CorpVO corpVO){
    	try
    	{
    		String url = sxk_url + "createcorp";
    		Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("CorpId", corpVO.getCorpId());
            parmMap.put("InviteCode", corpVO.getInviteCode());
            parmMap.put("Industry", corpVO.getIndustry());
            parmMap.put("CorpName", corpVO.getCorpName());
            parmMap.put("InviteUrl", corpVO.getInviteUrl());
            parmMap.put("CorpLogoUrl", corpVO.getCorpLogoUrl());
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));
    		return sr;
    	}
    	catch(Exception e){
    		return e.getMessage();
    	}
    }
    
    
    public String addDept(String deptId,String corpId)
    {
    	try
    	{
    		String url = sxk_url + "adddept";
    		Map<String, Object> parmMap = new HashMap<String, Object>();
            parmMap.put("DeptId", deptId);
            parmMap.put("CorpId", corpId);
            String sr = httpRequestHelper.httpPostJson(url, JSON.toJSONString(parmMap));
    		return sr;
    	}
    	catch(Exception e){
    		return e.getMessage();
    	} 
    }






}
