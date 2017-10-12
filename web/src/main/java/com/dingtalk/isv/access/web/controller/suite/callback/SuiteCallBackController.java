package com.dingtalk.isv.access.web.controller.suite.callback;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.isv.access.api.constant.AccessSystemConfig;
import com.dingtalk.isv.access.api.enums.suite.SuitePushType;
import com.dingtalk.isv.access.api.model.corp.CorpVO;
import com.dingtalk.isv.access.api.model.corp.StaffVO;
import com.dingtalk.isv.access.api.model.event.mq.SuiteCallBackMessage;
import com.dingtalk.isv.access.api.model.suite.CorpSuiteAuthVO;
import com.dingtalk.isv.access.api.model.suite.SuiteTicketVO;
import com.dingtalk.isv.access.api.model.suite.SuiteVO;
import com.dingtalk.isv.access.api.service.corp.CorpManageService;
import com.dingtalk.isv.access.api.service.corp.StaffManageService;
import com.dingtalk.isv.access.biz.dingutil.*;
import com.dingtalk.isv.access.api.service.suite.CorpSuiteAuthService;
import com.dingtalk.isv.access.api.service.suite.SuiteManageService;
import com.dingtalk.isv.common.log.format.LogFormatter;
import com.dingtalk.isv.common.model.ServiceResult;
import com.dingtalk.oapi.lib.aes.DingTalkEncryptException;
import com.dingtalk.oapi.lib.aes.DingTalkEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.jms.Queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 套件事件回调监听
 *
 */
@Controller
public class SuiteCallBackController{
    private static final Logger     bizLogger = LoggerFactory.getLogger("SUITE_CALLBACK_LOGGER");
    private static final Logger    mainLogger = LoggerFactory.getLogger(SuiteCallBackController.class);

    @Autowired
    private SuiteManageService suiteManageService;
    @Autowired
    private ConfOapiRequestHelper  confOapiRequestHelper;
    @Autowired
    private CorpSuiteAuthService corpSuiteAuthService;
    @Autowired
    private CorpManageService corpManageService;
    @Resource
    private StaffManageService staffManageService;
    @Autowired
    private AccessSystemConfig accessSystemConfig;
    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    @Qualifier("suiteCallBackQueue")
    private Queue suiteCallBackQueue;

    /**
     * 创建套件的时候,回调地址就填写这个
     * @param signature
     * @param timestamp
     * @param nonce
     * @param json
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/suite/create", method = {RequestMethod.POST})
    public  Map<String, String> suiteCreate(
                                                @RequestParam(value = "signature", required = false) String signature,
                                                @RequestParam(value = "timestamp", required = false) String timestamp,
                                                @RequestParam(value = "nonce", required = false) String nonce,
                                                @RequestBody(required = false) JSONObject json
    ) {
        try{
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
            ));
            /**
             * token,aseKey这两个参数在注册套件的时候,填写的。如果不想改代码,那么注册套件的时候也可以填写下面的两个数值
             * 要注意信息安全哦.
             */
            String token = "jin1234567890987123123";
            String aseKey = "ognx09sr2btmicx1a0c1j5yy5yagb11iv92ij9b29ec";
            DingTalkEncryptor dingTalkEncryptor = new DingTalkEncryptor(token, aseKey, "suite4xxxxxxxxxxxxxxx");
            String encryptMsg = json.getString("encrypt");
            String plainText = dingTalkEncryptor.getDecryptMsg(signature, timestamp, nonce, encryptMsg);
            JSONObject callbackMsgJson = JSONObject.parseObject(plainText);
            String random = callbackMsgJson.getString("Random");
            String responseEncryMsg = random;
            Map<String, String> encryptedMap = dingTalkEncryptor.getEncryptedMap(responseEncryMsg, System.currentTimeMillis(), com.dingtalk.oapi.lib.aes.Utils.getRandomStr(8));
            return encryptedMap;
        }catch (Exception e){
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
            ),e);
            return null;
        }
    }


    /**
     * 当套件创建完毕之后,需要手动修改一下套件的回调地址。在修改套件的回调地址之前,需要在BD中插入一条记录
     *
     *  insert into isv_suite(id, gmt_create, gmt_modified, suite_name, suite_key, suite_secret, encoding_aes_key, token, event_receive_url)
     *  values(1, '2016-03-14 18:08:09', '2016-03-14 18:08:09', '服务报警应用', 'suitexdhgv7mnxxxxxxxx', 'xxxxxxxxxxKBJLLPtmFmwRtKfsuiEHHpBPx8jGlCSp-iznz9gFSpkG0T0KMU9jyB',
     *  'dd18qxxxxxx357g8r7itm5pyu5hg8ibe1blhqawhuaz', 'xxxxqaz2WSX', '');
     *
     * @param suiteKey
     * @param signature
     * @param timestamp
     * @param nonce
     * @param json
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/suite/callback/{suiteKey}", method = {RequestMethod.POST})
    public  Map<String, String> receiveCallBack(@PathVariable("suiteKey") String suiteKey,
                                  @RequestParam(value = "signature", required = false) String signature,
                                  @RequestParam(value = "timestamp", required = false) String timestamp,
                                  @RequestParam(value = "nonce", required = false) String nonce,
                                  @RequestBody(required = false) JSONObject json
    ) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                LogFormatter.KeyValue.getNew("signature", signature),
                LogFormatter.KeyValue.getNew("timestamp", timestamp),
                LogFormatter.KeyValue.getNew("nonce", nonce),
                LogFormatter.KeyValue.getNew("json", json)
        ));
        ServiceResult<SuiteVO> suiteVOSr = suiteManageService.getSuiteByKey(suiteKey);
        SuiteVO suiteVO = suiteVOSr.getResult();
        try{
            DingTalkEncryptor dingTalkEncryptor = new DingTalkEncryptor(suiteVO.getToken(), suiteVO.getEncodingAesKey(), suiteVO.getSuiteKey());
            String encryptMsg = json.getString("encrypt");
            String plainText = dingTalkEncryptor.getDecryptMsg(signature,timestamp,nonce,encryptMsg);
            bizLogger.info(LogFormatter.getKVLogData(null,
                    "解密之后明文消息",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json),
                    LogFormatter.KeyValue.getNew("plainText", plainText)
            ));
            //具体业务处理
            String returnStr = isvCallbackEvent(plainText,suiteKey) ;
            Map<String, String> encryptedMap = dingTalkEncryptor.getEncryptedMap(returnStr, System.currentTimeMillis(), com.dingtalk.oapi.lib.aes.Utils.getRandomStr(8));
            return encryptedMap;
        }catch (DingTalkEncryptException e){
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "解密失败程序异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
            ),e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "解密失败程序异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
            ),e);
        }catch (Exception e){
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "未知异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
            ),e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "未知异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
            ),e);
        }catch (Throwable throwable){
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "未知异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
            ),throwable);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "未知异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
            ),throwable);
        }
        return null;
    }

    /**
     * 处理各种回调时间的TAG。这个维度的回调是和套件相关的
     * @param callbackMsg
     * @param suiteKey
     * @return
     */
    private String isvCallbackEvent(String callbackMsg,String suiteKey) {

        JSONObject callbackMsgJson = JSONObject.parseObject(callbackMsg);
        String eventType = callbackMsgJson.getString("EventType");
        /*
         * 处理回调 开始
         */
        System.err.println("回调类型：" + eventType);
        System.err.println("callbackMsg：" + callbackMsg);
        
        SuiteCallBackMessage.Tag tag = null;
        if(SuiteCallBackMessage.Tag.USER_ADD_ORG.getKey().equals(eventType)){
            tag =  SuiteCallBackMessage.Tag.USER_ADD_ORG;
            String userId=callbackMsgJson.getString("UserId");
            String corpId=callbackMsgJson.getString("CorpId");
            try{
                ServiceResult<StaffVO> sr = staffManageService.getStaff(userId,corpId,suiteKey);
                System.err.println(JSON.toJSON(sr));
                StaffVO staffVO = sr.getResult();
            	
            	
            	
            	System.err.println("传入的userId：" + userId);
            	String rtn_str = confOapiRequestHelper.addUser(userId,corpId,staffVO);
            	System.err.println("更新user成功：" + rtn_str);
            }
            catch(Exception e){
            	System.err.println(e);
            }           
            
            
            
        }else if(SuiteCallBackMessage.Tag.USER_MODIFY_ORG.getKey().equals(eventType)){
        	tag =  SuiteCallBackMessage.Tag.USER_MODIFY_ORG;
        	String userId=callbackMsgJson.getString("UserId");  //数组形式
            String corpId=callbackMsgJson.getString("CorpId");
            try{
            	
            	List<String> listUsers = JSON.parseObject(userId,ArrayList.class);
                ServiceResult<StaffVO> sr = staffManageService.getStaff(listUsers.get(0),corpId,suiteKey);
                System.err.println(JSON.toJSON(sr));
                StaffVO staffVO = sr.getResult();
            	
            	
            	
            	System.err.println("传入的userId：" + userId);
            	String rtn_str = confOapiRequestHelper.addUser(userId,corpId,staffVO);
            	System.err.println("更新user成功：" + rtn_str);
            }
            catch(Exception e){
            	System.err.println(e);
            }   
        	
        	
//        	String userId=callbackMsgJson.getString("UserId");
//        	//更新小助手用户信息 开始
//        		//step 1.从钉钉读取用户信息
//            try{
//            	System.err.println("传入的userId：" + userId);
//            	String rtn_str = confOapiRequestHelper.updateUser(userId);
//            	System.err.println("更新user成功：" + rtn_str);
//            }
//            catch(Exception e){
//            	System.err.println(e);
//            }
        		//step 2.更新小助手数据库
        		
        	//更新小助手用户信息　结束
        	
        }
        else if(SuiteCallBackMessage.Tag.USER_LEAVE_ORG.getKey().equals(eventType)){
            tag =  SuiteCallBackMessage.Tag.USER_LEAVE_ORG;
        }else if(SuiteCallBackMessage.Tag.CRM_CUSTOMER_UPDATE.getKey().equals(eventType)){
            tag =  SuiteCallBackMessage.Tag.CRM_CUSTOMER_UPDATE;
        }else if(SuiteCallBackMessage.Tag.CRM_CONTACT_CALL.getKey().equals(eventType)){
            tag =  SuiteCallBackMessage.Tag.CRM_CONTACT_CALL;
        }else if(SuiteCallBackMessage.Tag.REPORT_ADD_CRM_REPORT.getKey().equals(eventType)){
            tag =  SuiteCallBackMessage.Tag.REPORT_ADD_CRM_REPORT;
        }else if(SuiteCallBackMessage.Tag.ORG_DEPT_CREATE.getKey().equals(eventType)){
        	tag =  SuiteCallBackMessage.Tag.ORG_DEPT_CREATE;
            String deptId=callbackMsgJson.getString("DeptId");
            String corpId=callbackMsgJson.getString("CorpId");
            try{
            	System.err.println("传入的deptId：" + deptId);
            	String rtn_str = confOapiRequestHelper.addDept(deptId,corpId);
            	System.err.println("更新dept成功：" + rtn_str);
            }
            catch(Exception e){
            	System.err.println(e);
            }
        	
        }else if(SuiteCallBackMessage.Tag.ORG_DEPT_MODIFY.getKey().equals(eventType)){
        	tag =  SuiteCallBackMessage.Tag.ORG_DEPT_MODIFY;
        }
        
        /*
         * 处理回调 结束
         */
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("the eventType:", eventType)
        ));
        
        
        String responseEncryMsg = "success";
        if(SuitePushType.SUITE_TICKET.getKey().equals(eventType)){
            String receiveSuiteTicket = callbackMsgJson.getString("SuiteTicket");
            String receiveSuiteKey = callbackMsgJson.getString("SuiteKey");
            SuiteTicketVO suiteTicketVO = new SuiteTicketVO();
            suiteTicketVO.setSuiteTicket(receiveSuiteTicket);
            suiteTicketVO.setSuiteKey(suiteKey);
            ServiceResult<Void> sr = suiteManageService.saveOrUpdateSuiteTicket(suiteTicketVO);
            if(!sr.isSuccess()){
                responseEncryMsg = "faile";
            }
        }else if(SuitePushType.TMP_AUTH_CODE.getKey().equals(eventType)){
            String tmpAuthCode = callbackMsgJson.getString("AuthCode");
            ServiceResult<CorpSuiteAuthVO>  sr = corpSuiteAuthService.saveOrUpdateCorpSuiteAuth(suiteKey, tmpAuthCode);
            CorpSuiteAuthVO corpSuiteAuthVO = sr.getResult();
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("the AuthCode", tmpAuthCode),
                    LogFormatter.KeyValue.getNew("the ServiceResult<CorpSuiteAuthVO> sr", corpSuiteAuthVO),
                    LogFormatter.KeyValue.getNew("sr.isSuccess()", sr.isSuccess())
            ));
            //根据临时授权码，获取永久授权码，并存储到数据库中
            
            //通知随行康业务系统，获取部门信息和用户信息
            /*
             * 1.获取企业信息
             */
            String corpId= corpSuiteAuthVO.getCorpId();
            ServiceResult<CorpVO> src = corpManageService.getCorp(corpId);
            CorpVO corpVo = src.getResult();
            System.err.println("传入的corp：" + corpVo);
            /*
             * 2.发送数据到随行康
             */
            try{
            	System.err.println("传入的corpId：" + corpVo.getCorpId());
            	String rtn_str = confOapiRequestHelper.createCorp(corpVo);
            	System.err.println("新增corp成功：" + rtn_str);
            }
            catch(Exception e){
            	System.err.println(e);
            }
            
            
            
            if(!sr.isSuccess()){
                responseEncryMsg = "faile";
            }
        }else if(SuitePushType.CHANGE_AUTH.getKey().equals(eventType)){
            String corpId = callbackMsgJson.getString("AuthCorpId");
            ServiceResult<Void>  sr = corpSuiteAuthService.handleChangeAuth(suiteKey,corpId);
            if(!sr.isSuccess()){
                responseEncryMsg = "faile";
            }
        }else if(SuitePushType.SUITE_RELIEVE.getKey().equals(eventType)){
            String receiveCorpId = callbackMsgJson.getString("AuthCorpId");
            ServiceResult<Void>  sr = corpSuiteAuthService.handleRelieveAuth(suiteKey,receiveCorpId);
            if(!sr.isSuccess()){
                responseEncryMsg = "faile";
            }
        }else if(SuitePushType.CHECK_CREATE_SUITE_URL.getKey().equals(eventType)){
            //TOD  激活操作
     	
        }else if(SuitePushType.CHECK_UPDATE_SUITE_URL.getKey().equals(eventType)){
            String random = callbackMsgJson.getString("Random");
            responseEncryMsg = random;
            
            
            
        }else if(SuitePushType.CHECK_SUITE_LICENSE_CODE.getKey().equals(eventType)){
             //TODO
             //留给业务自行判断
        }else{
            //当开放平台更新了新的推送类型,为了避免不认识,需要报警出来
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "无法识别的EventType",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("callbackMsg", callbackMsg)
            ));
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "无法识别的EventType",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("callbackMsg", callbackMsg)
            ));
        }
        return responseEncryMsg;
    }
    
    @ResponseBody
    @RequestMapping(value = "/suite/corp_callback2/{suiteKey}")
    public String corpBack(){
    	return "success";
    }


    /**
     * 企业接受接受回调事件。这个维度的回调都是和授权企业相关的
     * @param suiteKey
     * @param signature
     * @param timestamp
     * @param nonce
     * @param json
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/suite/corp_callback/{suiteKey}", method = {RequestMethod.POST})
    public Map<String, String>  corpSuiteCallBack(@PathVariable("suiteKey") String suiteKey,
                                                @RequestParam(value = "signature", required = false) String signature,
                                                @RequestParam(value = "timestamp", required = false) String timestamp,
                                                @RequestParam(value = "nonce", required = false) String nonce,
                                                @RequestBody(required = false) JSONObject json
    ) {
        ServiceResult<SuiteVO> suiteVOSr = suiteManageService.getSuiteByKey(suiteKey);
        SuiteVO suiteVO = suiteVOSr.getResult();
        try {
        	System.err.println("通讯录回调：");
            DingTalkEncryptor dingTalkEncryptor = new DingTalkEncryptor(suiteVO.getToken(), suiteVO.getEncodingAesKey(), suiteVO.getSuiteKey());
            String encryptMsg = json.getString("encrypt");
            String plainText = dingTalkEncryptor.getDecryptMsg(signature, timestamp, nonce, encryptMsg);
            JSONObject jsonObject = JSON.parseObject(plainText);
            String eventType = jsonObject.getString("EventType");
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
            		"通讯录变更事件" + eventType,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json),
                    LogFormatter.KeyValue.getNew("plainText", plainText)
            ));

            if("check_url".equals(eventType)){
                Map<String, String> encryptedMap = dingTalkEncryptor.getEncryptedMap("success", System.currentTimeMillis(), com.dingtalk.oapi.lib.aes.Utils.getRandomStr(8));
                return encryptedMap;
            }


            //处理不同的回调事件,如果不关心的就不用处理
            SuiteCallBackMessage.Tag tag = null;
            if(SuiteCallBackMessage.Tag.USER_ADD_ORG.getKey().equals(eventType)){
                tag =  SuiteCallBackMessage.Tag.USER_ADD_ORG;
            }else if(SuiteCallBackMessage.Tag.USER_LEAVE_ORG.getKey().equals(eventType)){
                tag =  SuiteCallBackMessage.Tag.USER_LEAVE_ORG;
            }else if(SuiteCallBackMessage.Tag.CRM_CUSTOMER_UPDATE.getKey().equals(eventType)){
                tag =  SuiteCallBackMessage.Tag.CRM_CUSTOMER_UPDATE;
            }else if(SuiteCallBackMessage.Tag.CRM_CONTACT_CALL.getKey().equals(eventType)){
                tag =  SuiteCallBackMessage.Tag.CRM_CONTACT_CALL;
            }else if(SuiteCallBackMessage.Tag.REPORT_ADD_CRM_REPORT.getKey().equals(eventType)){
                tag =  SuiteCallBackMessage.Tag.REPORT_ADD_CRM_REPORT;
            }

            if(null!=tag){
                //通知业务方各种回调事件,业务方实现各自的业务
                //多加入一个套件Key维度
                jsonObject.put("suiteKey",suiteKey);
                jmsTemplate.send(suiteCallBackQueue,new SuiteCallBackMessage(jsonObject,tag));
            }

            Map<String, String> encryptedMap = dingTalkEncryptor.getEncryptedMap("success", System.currentTimeMillis(), com.dingtalk.oapi.lib.aes.Utils.getRandomStr(8));
            return encryptedMap;
        }catch (Exception e){
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("signature", signature),
                    LogFormatter.KeyValue.getNew("timestamp", timestamp),
                    LogFormatter.KeyValue.getNew("nonce", nonce),
                    LogFormatter.KeyValue.getNew("json", json)
                    ), e);
            return null;
        }

    }


























}
