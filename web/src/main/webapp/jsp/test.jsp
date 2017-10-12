<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script src="http://g.alicdn.com/dingding/open-develop/0.7.0/dingtalk.js"></script>
<script src="./static/javascript/jquery-3.1.0.js"></script>
<!--  <script type="text/javascript" src="./static/javascript/demo.js">-->
<script type="text/javascript">
		$(document).ready(function(){
			var url= window.location.href;
			var corpId = "${test}";//"dingddfa5378bf1b8a7035c2f4657eb6378f";//这个自己该成授权过的企业CORPID

			var signature = "";
			var nonce = "";
			var timeStamp = "";
			var agentId = "";

			$.post(
				"get_js_config",
				{
				  "url":url,
				  "corpId":corpId
				},
				function(result){
    			  signature = result.signature;
    			  nonce = result.nonce;
    			  timeStamp = result.timeStamp;
    			  agentId = result.agentId;
    			  corpId = result.corpId;

				dd.config({
					"agentId": agentId,
					"corpId": corpId,
					"timeStamp": timeStamp,
					"nonceStr": nonce,
					"signature": signature,
					jsApiList: ['device.notification.confirm',
								'device.notification.alert',
								'device.notification.prompt',
								'biz.chat.chooseConversation',
								'biz.ding.post',
								'biz.contact.choose',
								'biz.ding.post',
								'biz.util.openLink',
								'runtime.info']
				});


                dd.ready(function() {
                    //alert('dd ready');

                    document.addEventListener('pause', function() {
                        //alert('pause');
                    });

                    document.addEventListener('resume', function() {
                        //alert('resume');
                    });

/*                     dd.device.notification.alert({
                        message: 'dd.device.notification.alert',
                        title: 'This is title',
                        buttonName: 'button',
                        onSuccess: function(data) {
                            //alert('win: ' + JSON.stringify(data));
                        },
                        onFail: function(err) {
                            //alert('fail: ' + JSON.stringify(err));
                        }
                    }); */
                    
/* 					dd.runtime.info({
						onSuccess : function(info) {
							alert('runtime info: ' + JSON.stringify(info));
						},
						onFail : function(err) {
							alert('fail: ' + JSON.stringify(err));
						}
					}); */
					
 				    dd.biz.navigation.setTitle({
				        title: '随行康中医体检',
				        onSuccess: function(data) {
				        	//alert("设置标题");
				        },
				        onFail: function(err) {
				            //log.e(JSON.stringify(err));
				        }
				    }); 

					//权限验证开始
						dd.runtime.permission.requestAuthCode({
							corpId : corpId,
							onSuccess : function(info) {
								//alert('authcode: ' + info.code);
  								$.ajax({
									url : 'userinfo?code=' + info.code + '&corpid='
											+ corpId,
									type : 'GET',
									success : function(data, status, xhr) {
										var info = JSON.parse(data);
								//			alert(info.result.userId);
										document.getElementById("userName").innerHTML = info.result.name;
										document.getElementById("userId").innerHTML = info.result.userId;
										
										// 图片
										if(info.avatar.length != 0){
								            var img = document.getElementById("userImg");
								            img.src = info.avatar;
								                      img.height = '100';
								                      img.width = '100';
								          }
					
									},
									error : function(xhr, errorType, error) {
										//logger.e("yinyien:" + _config.corpId);
										alert(errorType + ', ' + error);
									}
								});  
					
							},
							onFail : function(err) {
								alert('fail: ' + JSON.stringify(err));
							}
						});
					//权限验证结束
                    
                });

                dd.error(function(err) {
                    alert('dd error: ' + JSON.stringify(err));
                });
                

                
                

  			});
		});
</script>
<title>随行康钉钉对接平台</title>
</head>
<body>
    <h1>随行康钉钉对接平台</h1>
    
	<h1>获取JsTicket</h1>
	<h1>corp:${test}</h1>
	<div id="userId"></div><br />
	<div id="userName"></div>
</body>
</html>