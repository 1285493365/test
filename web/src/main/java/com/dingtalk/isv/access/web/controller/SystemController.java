package com.dingtalk.isv.access.web.controller;

import com.alibaba.fastjson.JSON;
import com.dingtalk.isv.access.api.constant.AccessSystemConfig;
import com.dingtalk.isv.access.api.model.corp.CorpAppVO;
import com.dingtalk.isv.access.api.model.corp.CorpJSAPITicketVO;
import com.dingtalk.isv.access.api.model.corp.CorpTokenVO;
import com.dingtalk.isv.access.api.model.corp.DepartmentVO;
import com.dingtalk.isv.access.api.model.corp.LoginUserVO;
import com.dingtalk.isv.access.api.model.corp.StaffVO;
import com.dingtalk.isv.access.api.model.event.mq.SuiteCallBackMessage;
import com.dingtalk.isv.access.api.model.suite.CorpSuiteAuthVO;
import com.dingtalk.isv.access.api.model.suite.CorpSuiteCallBackVO;
import com.dingtalk.isv.access.api.model.suite.SuiteVO;
import com.dingtalk.isv.access.api.service.corp.CorpManageService;
import com.dingtalk.isv.access.api.service.corp.StaffManageService;
import com.dingtalk.isv.access.api.service.corp.DeptManageService;
import com.dingtalk.isv.access.api.service.suite.CorpSuiteAuthService;
import com.dingtalk.isv.access.api.service.suite.SuiteManageService;
import com.dingtalk.isv.access.api.service.message.SendMessageService;
import com.dingtalk.isv.access.biz.corp.dao.CorpJSAPITicketDao;
import com.dingtalk.isv.access.biz.corp.model.helper.CorpJSAPITicketConverter;
import com.dingtalk.isv.access.biz.dingutil.ConfOapiRequestHelper;
import com.dingtalk.isv.access.biz.dingutil.CorpOapiRequestHelper;
import com.dingtalk.isv.access.web.controller.suite.callback.SuiteCallBackController;
import com.dingtalk.isv.common.code.ServiceResultCode;
import com.dingtalk.isv.common.log.format.LogFormatter;
import com.dingtalk.isv.common.model.HttpResult;
import com.dingtalk.isv.common.model.ServiceResult;
import com.dingtalk.isv.common.util.HttpUtils;
import com.dingtalk.oapi.lib.aes.DingTalkJsApiSingnature;
import com.dingtalk.open.client.api.model.corp.Department;
import com.dingtalk.open.client.api.model.corp.MessageBody;
import com.google.common.eventbus.EventBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 这个controller功能如下
 * 1.健康检测
 * 2.数据订正
 * 3.临时测试
 */
@Controller
public class SystemController {
	
	private static final Logger     bizLogger = LoggerFactory.getLogger("SystemController_LOGGER");
    private static final Logger    mainLogger = LoggerFactory.getLogger(SystemController.class);

	
    @Autowired
    private EventBus corpAuthSuiteEventBus;

    @Resource
    private CorpSuiteAuthService corpSuiteAuthService;
    @Resource
    private CorpOapiRequestHelper corpRequestHelper;
    @Resource
    private SendMessageService sendMessageService;
    @Resource
    private CorpManageService corpManageService;
    @Resource
    private DeptManageService deptManageService;
    @Resource
    SuiteManageService suiteManageService;
    @Resource
    AccessSystemConfig accessSystemConfig;
    @Resource
    CorpJSAPITicketDao corpJSAPITicketDao;
    @Resource
    private StaffManageService staffManageService;
    @Resource
    ConfOapiRequestHelper confOapiRequestHelper;
    private Long microappAppId = 4224L;                      //该应用原声的appid
    private final String suiteKey = "suitef84d7c433xx1se9h";//"suitexdhgv7mn5ufoi9ui"; //该应用所属的套件suitekey
    private String corpId="ding1e7324915d5fb71b35c2f4657eb6378f";
    private Long appId=4224L;
    
    @Resource
    private HttpResult httpResult;

    @RequestMapping("/test")
//    @ResponseBody
    public ModelAndView test(@RequestParam(value = "corp", required = false) String corp) {
    	ServiceResult<SuiteVO> suiteVOSr = suiteManageService.getSuiteByKey(suiteKey);
        SuiteVO suiteVO = suiteVOSr.getResult();
        ModelAndView mv = new ModelAndView("test");
        mv.addObject("suiteVO",JSON.toJSONString(suiteVO));
        mv.addObject("test",corp);
        return mv;
    }
    
    @RequestMapping("/get_js_config")
    @ResponseBody
    public Map<String, Object>  getJSConfig(@RequestParam(value = "url_rtn", required = false) String url_rtn,
                              @RequestParam(value = "corpId", required = false) String corpId

    ) {
        try{
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("url", url_rtn),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("appId", microappAppId)
            ));
            url_rtn = check(url_rtn,corpId,suiteKey,microappAppId);
            ServiceResult<CorpJSAPITicketVO> jsapiTicketSr = corpManageService.getCorpJSAPITicket(suiteKey, corpId);
            ServiceResult<CorpAppVO> corpAppVOSr = corpManageService.getCorpApp(corpId, microappAppId);
            String nonce = com.dingtalk.oapi.lib.aes.Utils.getRandomStr(8);
            Long timeStamp = System.currentTimeMillis();
            String sign = DingTalkJsApiSingnature.getJsApiSingnature(url_rtn, nonce, timeStamp, jsapiTicketSr.getResult().getCorpJSAPITicket());
            Map<String,Object> jsapiConfig = new HashMap<String, Object>();
            jsapiConfig.put("signature",sign);
            jsapiConfig.put("nonce",nonce);
            jsapiConfig.put("timeStamp",timeStamp);
            jsapiConfig.put("agentId",corpAppVOSr.getResult().getAgentId());
            jsapiConfig.put("corpId",corpId);
            System.out.println(JSON.toJSONString(httpResult.getSuccess(jsapiConfig)));
            return httpResult.getSuccess(jsapiConfig);
        }catch (Exception e){
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统错误",
                    LogFormatter.KeyValue.getNew("url", url_rtn),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ),e);
            return httpResult.getFailure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }
    
    @RequestMapping("/get_js_config2")
    @ResponseBody
    public void  getJSConfig2(HttpServletRequest request,
    						  HttpServletResponse response,
    						  @RequestParam(value = "url_rtn", required = false) String url_rtn,
                              @RequestParam(value = "corpId", required = false) String corpId,
                              @RequestParam(value = "jsoncallback", required = false) String jsoncallback

    ) {
        try{
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("url", url_rtn),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("appId", microappAppId)
            ));
            url_rtn = check(url_rtn,corpId,suiteKey,microappAppId);
            ServiceResult<CorpJSAPITicketVO> jsapiTicketSr = corpManageService.getCorpJSAPITicket(suiteKey, corpId);
            ServiceResult<CorpAppVO> corpAppVOSr = corpManageService.getCorpApp(corpId, microappAppId);
            String nonce = com.dingtalk.oapi.lib.aes.Utils.getRandomStr(8);
            Long timeStamp = System.currentTimeMillis();
            String sign = DingTalkJsApiSingnature.getJsApiSingnature(url_rtn, nonce, timeStamp, jsapiTicketSr.getResult().getCorpJSAPITicket());
            Map<String,Object> jsapiConfig = new HashMap<String, Object>();
            jsapiConfig.put("signature",sign);
            jsapiConfig.put("nonce",nonce);
            jsapiConfig.put("timeStamp",timeStamp);
            jsapiConfig.put("agentId",corpAppVOSr.getResult().getAgentId());
            jsapiConfig.put("corpId",corpId);
            String rtn_str = jsoncallback + "(" + JSON.toJSONString(httpResult.getSuccess(jsapiConfig)) + ")";
            //String rtn_str = "{‘" + jsoncallback + "’:‘tel:13770003333,wel:2342’}";
            //String rtn_str = jsoncallback + "([{\"_name\":\"湖南省\",\"_regionId\":134},{\"_name\":\"北京市\",\"_regionId\":143}])";
            System.out.println(rtn_str);
            response.getWriter().print(rtn_str);
        }catch (Exception e){
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统错误",
                    LogFormatter.KeyValue.getNew("url", url_rtn),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ),e);
            //return corpId + JSON.toJSONString(httpResult.getFailure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg()));
        }
    }
    
    private String check(String url,String corpId,String suiteKey,Long appId) throws Exception{//TODO 妈蛋的就然没有定义serviceexception
        try {
            url = URLDecoder.decode(url,"UTF-8");
            URL urler = new URL(url);
            StringBuffer urlBuffer = new StringBuffer();
            urlBuffer.append(urler.getProtocol());
            urlBuffer.append(":");
            if (urler.getAuthority() != null && urler.getAuthority().length() > 0) {
                urlBuffer.append("//");
                urlBuffer.append(urler.getAuthority());
            }
            if (urler.getPath() != null) {
                urlBuffer.append(urler.getPath());
            }
            if (urler.getQuery() != null) {
                urlBuffer.append('?');
                urlBuffer.append(URLDecoder.decode(urler.getQuery(), "utf-8"));
            }
            url = urlBuffer.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("url非法");
        }
        return url;
    }

    @ResponseBody
    @RequestMapping(value = "/token/{suiteKey}", method = {RequestMethod.GET})
    public String genToken( HttpServletRequest request,@PathVariable("suiteKey") String suiteKey ) {
        String ip = HttpUtils.getRemortIP(request);
        if("127.0.0.1".equals(ip)){
            ServiceResult<Void> sr = suiteManageService.saveOrUpdateSuiteToken(suiteKey);
            if(sr.isSuccess()){
                return "success";
            }
            return sr.getCode()+","+sr.getMessage();
        }
        return "is not 127.0.0.1";
    }


    @RequestMapping(value = "/getCorpToken", method = {RequestMethod.GET})
    @ResponseBody
    public String getAccessToken(HttpServletRequest request,
                                  @RequestParam(value = "corpId", required = true) String corpId,
                                  @RequestParam(value = "suiteKey", required = true) String suiteKey) {
        String ip = HttpUtils.getRemortIP(request);
        if("127.0.0.1".equals(ip)){
            ServiceResult<CorpTokenVO> sr = corpManageService.getCorpToken(suiteKey, corpId);
            if(sr.isSuccess()){
                return "success";
            }
            return sr.getCode()+","+sr.getMessage();
        }
        return "is not 127.0.0.1";
    }


    @RequestMapping(value = "/getJsTicket", method = {RequestMethod.GET})
    @ResponseBody
    public String getJsTicket(HttpServletRequest request,
                                 @RequestParam(value = "corpId", required = true) String corpId,
                                 @RequestParam(value = "suiteKey", required = true) String suiteKey) {
        String ip = HttpUtils.getRemortIP(request);
        if("127.0.0.1".equals(ip)){
            ServiceResult<CorpTokenVO> corpTokenVoSr = corpManageService.getCorpToken(suiteKey, corpId);
            ServiceResult<CorpJSAPITicketVO> jsAPITicketSr = confOapiRequestHelper.getJSTicket(suiteKey, corpId, corpTokenVoSr.getResult().getCorpToken());
            corpJSAPITicketDao.saveOrUpdateCorpJSAPITicket(CorpJSAPITicketConverter.corpJSTicketVO2CorpJSTicketDO(jsAPITicketSr.getResult()));
            return "success";
        }
        return "is not 127.0.0.1";
    }


    @RequestMapping(value = "/getSuiteToken", method = {RequestMethod.GET})
    @ResponseBody
    public String getSuiteAccessToken(HttpServletRequest request,
                                      @RequestParam(value = "suiteKey", required = true) String suiteKey) {
        String ip = HttpUtils.getRemortIP(request);
        if("127.0.0.1".equals(ip)){
            ServiceResult<Void> sr = suiteManageService.saveOrUpdateSuiteToken(suiteKey);
            if(sr.isSuccess()){
                return "success";
            }
            return sr.getCode()+","+sr.getMessage();
        }
        return "is not 127.0.0.1";
    }



    /**
     * 更新企业回调地址
     * @param request
     * @param suiteKey
     * @return
     */
    @RequestMapping(value = "/updateCorpCallBack", method = {RequestMethod.GET})
    @ResponseBody
    public String updateCorpCallBack(HttpServletRequest request,
                                    @RequestParam(value = "suiteKey", required = true) String suiteKey,
                                    @RequestParam(value = "corpId", required = true) String corpId
                                    ) {
        String ip = HttpUtils.getRemortIP(request);
        if("127.0.0.1".equals(ip)){
            //订正全体
            if(StringUtils.isEmpty(corpId)){
                ServiceResult<List<CorpSuiteAuthVO>> sr = corpSuiteAuthService.getCorpSuiteAuthByPage(suiteKey, 0, Integer.MAX_VALUE);
                List<CorpSuiteAuthVO> corpSuiteAuthVOList = sr.getResult();
                for(CorpSuiteAuthVO corpSuiteAuthVO:corpSuiteAuthVOList){
                    ServiceResult<CorpSuiteCallBackVO> callBackSr =  corpSuiteAuthService.getCorpCallback(suiteKey, corpSuiteAuthVO.getCorpId());
                    if(callBackSr.isSuccess()){
                        corpSuiteAuthService.updateCorpCallback(suiteKey, corpSuiteAuthVO.getCorpId(), (accessSystemConfig.getCorpSuiteCallBackUrl() + suiteKey), SuiteCallBackMessage.Tag.getAllTag());
                        continue;
                    }
                    corpSuiteAuthService.saveCorpCallback(suiteKey, corpSuiteAuthVO.getCorpId(), (accessSystemConfig.getCorpSuiteCallBackUrl()+suiteKey), SuiteCallBackMessage.Tag.getAllTag());
                }
            }else{
                ServiceResult<CorpSuiteCallBackVO> callBackSr =  corpSuiteAuthService.getCorpCallback(suiteKey, corpId);
                if(callBackSr.isSuccess()){
                    corpSuiteAuthService.updateCorpCallback(suiteKey, corpId, (accessSystemConfig.getCorpSuiteCallBackUrl() + suiteKey), SuiteCallBackMessage.Tag.getAllTag());
                }
                corpSuiteAuthService.saveCorpCallback(suiteKey, corpId, (accessSystemConfig.getCorpSuiteCallBackUrl()+suiteKey), SuiteCallBackMessage.Tag.getAllTag());
            }
            return "success";
        }
        return "is not 127.0.0.1";
    }

    /**
     * 获取部门信息
     * @param request
     * @param suiteKey
     * @return
     */
    @RequestMapping(value = "/getDep", method = {RequestMethod.GET})
    @ResponseBody
    public void getDep()
    {	
        ServiceResult<DepartmentVO> dept = deptManageService.getDept(1L,corpId, suiteKey);        
        System.err.println("部门详情：");
        System.err.println(JSON.toJSONString(dept));
//        return JSON.toJSONString(dept);
    }
    
    @RequestMapping(value = "/getDepList", method = {RequestMethod.GET})
    @ResponseBody
    public void getDepList()
    {	
    	ServiceResult<List<Department>> dept = deptManageService.getDeptList(1L,corpId, suiteKey);        
        System.err.println("部门列表：");
        System.err.println(JSON.toJSONString(dept));
//        return JSON.toJSONString(dept);
    }
    
    
    /**
     * 获取登录用户信息
     * @param code
     * @param corpid
     * @return
     */
    @RequestMapping(value = "/userinfo", method = {RequestMethod.GET})
    @ResponseBody
    public void getUserinfo(HttpServletRequest request,
    		HttpServletResponse response,
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "corpid", required = true) String corpid,
            @RequestParam(value = "jsoncallback", required = false) String jsoncallback
            )
    {
    	try{
		        String corpId = corpid;
		        ServiceResult<CorpTokenVO> ssr = corpManageService.getCorpToken(suiteKey, corpId);
		        CorpTokenVO corpTokenVO = ssr.getResult();
		        String corpToken= corpTokenVO.getCorpToken();
		        bizLogger.info(LogFormatter.getKVLogData(null,
	                    "正确信息:",
		                LogFormatter.KeyValue.getNew("code", code),
		                LogFormatter.KeyValue.getNew("corpId", corpId),
		                LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
		                LogFormatter.KeyValue.getNew("corpToken", corpToken)
		        ));
		        
		        ServiceResult<LoginUserVO> loginUserVO = corpRequestHelper.getStaffByAuthCode(suiteKey, corpId, corpToken,code);
		    	//String ip = HttpUtils.getRemortIP(request);
		        System.err.println("登录用户信息：");
		        System.err.println(JSON.toJSONString(loginUserVO));
		        response.getWriter().print(jsoncallback + "(" + JSON.toJSONString(loginUserVO) + ")");
    	}
    	catch(Exception e){
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "失败:",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
            ),e);
    	}
    	//return ServiceResultCode.SYS_ERROR.getErrCode() + ServiceResultCode.SYS_ERROR.getErrMsg();
    }

    @RequestMapping(value = "/sendmsg", method = {RequestMethod.GET})
    @ResponseBody
    public void sendOAMessageToUser(){
    	try{

    	    String msgType="text";
    	    List<String> staffIdList=new ArrayList();
    	    staffIdList.add("manager8784");
    	    List<Long> deptIdList=new ArrayList();
    	    deptIdList.add(1L);
            MessageBody.OABody message = new MessageBody.OABody();
            MessageBody.OABody.Head head = new MessageBody.OABody.Head();
            MessageBody.OABody.Body body = new MessageBody.OABody.Body();
            head.setText("HEAD");
            head.setBgcolor("ffffa328");
            head.setText("客户详情");
            //body.setAuthor("dd_test");
            body.setContent("标题 \r\n 换行");
            body.setTitle("xxxxxxxx \r\n  换行");
            //body.setTitle("http://qr.dingtalk.com/page/crminfo?appid=-23&corpid=%24"+corpId+"%24&staffid=\"\"");
            message.setHead(head);
            message.setBody(body);
            message.setMessage_url("http://qr.dingtalk.com/page/crminfo?appid=-23&corpid=%24"+corpId+"%24");
            System.err.println("开始读取：");
            System.err.println(message.toString());
            System.err.println("开始输出：");
    	    ServiceResult<Void> sr = sendMessageService.sendOAMessageToUser(
									suiteKey, 
									corpId,
									appId, 
									msgType, 
									staffIdList, 
									deptIdList,
									message);
    	    System.err.println(JSON.toJSONString(sr));
    		}
    	catch(Exception e){
    		System.err.println(e.toString());
    	}
    }
    @RequestMapping(value = "/getstaff", method = {RequestMethod.GET})
    @ResponseBody   
    public void getStaff()
    {
        String staffId = "manager8784";
        ServiceResult<StaffVO> sr = staffManageService.getStaff(staffId,corpId,suiteKey);
        System.err.println(JSON.toJSON(sr));
    }
    @RequestMapping(value = "/getUserList", method = {RequestMethod.GET})
    @ResponseBody 
    public void getUserList(){
    	Long department_id =49845244L;
    	Long offset=0L;
    	Integer size=100;
    	String order="entry_asc";
    	ServiceResult<List<StaffVO>> sr = staffManageService.getUserList(suiteKey,corpId,department_id,offset,size,order);
    	System.err.println(JSON.toJSON(sr));
    }
    
    @RequestMapping(value = "/addRegister", method = {RequestMethod.GET})
    @ResponseBody 
    public void getRegister(){
        ServiceResult<CorpTokenVO> ssr = corpManageService.getCorpToken(suiteKey, corpId);
        CorpTokenVO corpTokenVO = ssr.getResult();
        String corpToken= corpTokenVO.getCorpToken();	

        String aes_key = "ognx09sr2btmicx1a0c1j5yy5yagb11iv92ij9b29ec";
        String token="jin1234567890987123123";
        String rtn_url="http://114.55.100.6:8080/ding-isv-access/suite/callback/suitef84d7c433xx1se9h";
        String call_back_tag = "[\"user_add_org\", \"user_modify_org\", \"user_leave_org\", \"org_dept_create\", \"org_dept_modify\", \"org_dept_remove\"]";
        ServiceResult<String> sr = corpManageService.registerEvent(suiteKey, call_back_tag,corpToken,token,aes_key,rtn_url);
        System.err.println(JSON.toJSON(sr));
    	
    }
    
    @RequestMapping(value = "/updateRegister", method = {RequestMethod.GET})
    @ResponseBody 
    public void updateRegister(){
        ServiceResult<CorpTokenVO> ssr = corpManageService.getCorpToken(suiteKey, corpId);
        CorpTokenVO corpTokenVO = ssr.getResult();
        String corpToken= corpTokenVO.getCorpToken();	

        String aes_key = "ognx09sr2btmicx1a0c1j5yy5yagb11iv92ij9b29ec";
        String token="jin1234567890987123123";
        String rtn_url="http://114.55.100.6:8080/ding-isv-access/suite/callback/suitef84d7c433xx1se9h";
        String call_back_tag = "[\"user_add_org\", \"user_modify_org\", \"user_leave_org\", \"org_dept_create\", \"org_dept_modify\", \"org_dept_remove\"]";
        ServiceResult<String> sr = corpManageService.updateEvent(suiteKey, call_back_tag,corpToken,token,aes_key,rtn_url);
        System.err.println(JSON.toJSON(sr));
    	
    }
    
    @RequestMapping(value = "/getUser", method = {RequestMethod.GET})
    @ResponseBody
    public void getUser(){
    	try{
        	String rtn_str = confOapiRequestHelper.updateUser("123");
        	System.err.println("更新user成功：" + rtn_str);
        }
        catch(Exception e){
        	System.err.println(e);
        }
    }
    


}
