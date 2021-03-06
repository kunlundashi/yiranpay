package com.channel.bank.adapter.pay.api;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.channel.bank.adapter.pay.config.WxPayH5Config;
import com.channel.bank.adapter.pay.constants.ReturnCode;
import com.channel.bank.adapter.pay.constants.WXPAYFundChannelKey;
import com.channel.bank.adapter.pay.domain.AmqoRequrst;
import com.channel.bank.adapter.pay.domain.ChannelFundRequest;
import com.channel.bank.adapter.pay.domain.ChannelFundResult;
import com.channel.bank.adapter.pay.domain.ChannelRequest;
import com.channel.bank.adapter.pay.domain.QueryRequest;
import com.channel.bank.adapter.pay.domain.ResultWrapper;
import com.channel.bank.adapter.pay.enums.BestPayTypeEnum;
import com.channel.bank.adapter.pay.enums.FundChannelApiType;
import com.channel.bank.adapter.pay.filedown.WinXinFileDown;
import com.channel.bank.adapter.pay.mock.MockResultData;
import com.channel.bank.adapter.pay.mode.OrderQueryRequest;
import com.channel.bank.adapter.pay.mode.OrderQueryResponse;
import com.channel.bank.adapter.pay.mode.PayFundRequest;
import com.channel.bank.adapter.pay.mode.PayFundResponse;
import com.channel.bank.adapter.pay.property.PropertyHelper;
import com.channel.bank.adapter.pay.service.IAmqpService;
import com.channel.bank.adapter.pay.service.impl.BestPayServiceImpl;
import com.channel.bank.adapter.pay.service.impl.WXPayResultNotifyService;
import com.channel.bank.adapter.pay.utils.JsonUtil;
import com.channel.bank.adapter.pay.utils.StringUtils;
import com.channel.bank.adapter.pay.utils.WXXMLUtil;

/**
 * ????????????
 * @author pandaa
 *
 */
@RestController
@RequestMapping("/api/yiranpay/channelpay/wxpay")
public class WeiXinFundinFacade extends BasePayFundinFacade{
	private Logger logger = LoggerFactory.getLogger(WeiXinFundinFacade.class);
	@Autowired
	private PropertyHelper propertyHelper;
	@Autowired
    private BestPayServiceImpl bestPayService;
	@Autowired
	private WXPayResultNotifyService wxPayResultNotifyService;
	@Autowired
	private IAmqpService amqpService;
	//@Autowired
	//private IResultNotifyFacade resultNotifyFacade;
	
	@Autowired
	private WinXinFileDown winXinFileDown;
	
	@PostMapping("/pay")
	public ResultWrapper<Map<String,Object>>  fundin(@RequestBody String request) {
		logger.info("PayChannelOrder->Channel?????????????????????????????????"+request);
		ChannelFundResult result = new ChannelFundResult();
		ChannelFundRequest req = JSON.parseObject(request, ChannelFundRequest.class);
		logger.info("PayChannelOrder->Channel?????????????????????????????????????????????"+req);
		Properties properties = propertyHelper.getProperties(req.getFundChannelCode());
		//??????mock????????????????????????????????????mock??????
        String mock_switch = properties.getProperty(WXPAYFundChannelKey.MOCK_SWITCH);
        if("true".equals(mock_switch)){//??????????????????mock??????
        	result.setApiType(req.getApiType());
        	result.setRealAmount(req.getAmount());
 			result.setInstOrderNo(req.getInstOrderNo());
 			result.setProcessTime(new Date());
        	result = MockResultData.mockResule(result);
        	logger.info("????????????mock?????????");
        	return ResultWrapper.ok().putData(result);
        }
        try {
        	initBestPayService(properties);
        	PayFundRequest payRequest = new PayFundRequest();
        	String openId = req.getExtension().get("openId");
            payRequest.setOpenid(openId);
            payRequest.setOrderAmount(req.getAmount().doubleValue());
            payRequest.setOrderId(req.getInstOrderNo());
            String orderName = req.getExtension().get("orderName");
            payRequest.setOrderName(orderName);
            String memberId = req.getExtension().get("memberId");
            String payType = req.getExtension().get("payType");
            if("NATIVE".equals(payType)){
            	//NATIVE????????????
            	payRequest.setPayTypeEnum(BestPayTypeEnum.WXPAY_NATIVE);
            }else{
            	 payRequest.setPayTypeEnum(BestPayTypeEnum.WXPAY_H5);
            }
            logger.info("??????????????????????????????, request={}", JsonUtil.toJson(payRequest));

            PayFundResponse payResponse = bestPayService.pay(payRequest);
            logger.info("??????????????????????????????,????????????, response={}", JsonUtil.toJson(payResponse));
            
            if("FAIL".equals(payResponse.getResultCode())){
            	result.setApiResultCode(payResponse.getErrCode());
                result.setApiResultMessage(payResponse.getErrCodeDes());
                result.setResultMessage(payResponse.getReturnMsg());
                result.setSuccess(false);
    			result.setRealAmount(req.getAmount());
    			result.setProcessTime(new Date());
    	    	result.setFundChannelCode(req.getFundChannelCode());
    	    	result.setApiType(FundChannelApiType.DEBIT);
    	    	result.setExtension(JSON.toJSONString(payResponse));
    	    	result.setInstOrderNo(req.getInstOrderNo());
    	    	logger.info("????????????????????????:"+JSON.toJSONString(result));
            	return ResultWrapper.ok().putData(result);
            }else{
            	result.setApiResultCode(payResponse.getReturnCode());
                result.setApiResultMessage(payResponse.getReturnMsg());
                result.setResultMessage(payResponse.getReturnMsg());
                result.setSuccess(true);
    			result.setRealAmount(req.getAmount());
    			result.setProcessTime(new Date());
    	    	result.setFundChannelCode(req.getFundChannelCode());
    	    	result.setApiType(FundChannelApiType.DEBIT);
    	    	result.setExtension(JSON.toJSONString(payResponse));
    	    	result.setInstOrderNo(req.getInstOrderNo());
    	    	result.setInstReturnOrderNo(payResponse.getOutTradeNo());
    	    	logger.info("????????????????????????:"+JSON.toJSONString(result));
            	return ResultWrapper.ok().putData(result);
            }
        	
        }catch (Exception e) {
        	logger.error("?????????[" + req.getFundChannelCode() + "]????????????", e);
        	Map<String, String> map = new HashMap<String,String>();
            map.put("fundsChannel", req.getFundChannelCode());
            result.setExtension(JSON.toJSONString(map));
            result = builFalidFundinResponse(req, "????????????", ReturnCode.FAILED, ReturnCode.FAILED,
                StringUtils.EMPTY_STRING);
            ResultWrapper.error().putData(result);
        }
		return null;
	}

	private void initBestPayService(Properties properties) {
		WxPayH5Config wxPayH5Config = new WxPayH5Config();
		String appId = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_APPID);
		logger.info("????????????????????????->?????????appID??????"+appId);
		wxPayH5Config.setAppId(appId);
		String appSecret = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_APPSECRET);
		logger.info("????????????????????????->???????????????appSecret??????"+appSecret);
		wxPayH5Config.setAppSecret(appSecret);
		String mchId = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_MCHID);
		logger.info("????????????????????????->???????????????ID??????"+mchId);
		wxPayH5Config.setMchId(mchId);
		String mchSecretKey = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_MCHSECRETKEY);
		logger.info("????????????????????????->???????????????????????????"+mchSecretKey);
		wxPayH5Config.setMchKey(mchSecretKey);
		String mchSecretKeyPath = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_MCHSECRETKEYPATH);
		logger.info("????????????????????????->?????????????????????????????????"+mchSecretKeyPath);
		wxPayH5Config.setKeyPath(mchSecretKeyPath);
		String notifyUrl = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_NOTIFYURL);
		logger.info("????????????????????????->???????????????URL??????"+notifyUrl);
		wxPayH5Config.setNotifyUrl(notifyUrl);
		bestPayService.setWxPayH5Config(wxPayH5Config);
	}
	
	@PostMapping("/query")
	public ResultWrapper<Map<String,Object>>  query(@RequestBody String request) {
		
		logger.info("PayChannelOrder->Channel???????????????????????????????????????"+request);
		ChannelFundResult result = new ChannelFundResult();
		QueryRequest req = JSON.parseObject(request, QueryRequest.class);
		result.setApiType(req.getApiType());
		logger.info("PayChannelOrder->Channel???????????????????????????????????????????????????"+req);
		Properties properties = propertyHelper.getProperties(req.getFundChannelCode());
        try {
        	String mock_switch = properties.getProperty(WXPAYFundChannelKey.MOCK_SWITCH);
            if("true".equals(mock_switch)){//??????????????????mock??????
            	result.setFundChannelCode(req.getFundChannelCode());
    			result.setInstOrderNo(req.getInstOrderNo());
    			result.setSuccess(true);
    			result.setApiType(req.getApiType());
    			result.setRealAmount(req.getAmount());
    			result.setInstOrderNo(req.getInstOrderNo());
    			result.setApiResultCode("0000");
    			result.setApiResultSubCode("SUCCESS");
    			result.setApiResultMessage("??????????????????mock????????????????????????");
    			result.setResultMessage("??????????????????mock????????????????????????");
    			result.setApiResultSubMessage("??????????????????mock????????????????????????");
            	logger.info("????????????mock?????????");
            	return ResultWrapper.ok().putData(result);
            }
        	initBestPayService(properties);
        	OrderQueryRequest queryRequest = new OrderQueryRequest();
        	queryRequest.setOrderId(req.getInstOrderNo());
        	queryRequest.setPayTypeEnum(BestPayTypeEnum.WXPAY_H5);
        	OrderQueryResponse queryResult = bestPayService.query(queryRequest);
        	logger.info("??????????????????????????????:"+JSON.toJSONString(queryResult));
        	if("FAIL".equals(queryResult.getResultCode())){
        		result.setFundChannelCode(req.getFundChannelCode());
        		result.setInstOrderNo(req.getInstOrderNo());
        		result.setApiResultCode(queryResult.getErrCode());
            	result.setRealAmount(req.getAmount());
            	result.setApiResultMessage(queryResult.getErrCodeDes());
    			result.setResultMessage(queryResult.getErrCodeDes());
            	result.setSuccess(false);
            	result.setExtension(JSON.toJSONString(queryResult));
            	logger.info("??????????????????:"+JSON.toJSONString(result));
            	return ResultWrapper.ok().putData(result);
            }else{
            	if("SUCCESS".equals(queryResult.getResultCode()) && "SUCCESS".equals(queryResult.getTradeState())){
                	result.setFundChannelCode(req.getFundChannelCode());
                	result.setInstOrderNo(req.getInstOrderNo());
                	result.setApiResultCode(queryResult.getResultCode());
                	result.setRealAmount(req.getAmount());
                	result.setApiResultSubCode(queryResult.getTradeState());
                	result.setResultMessage(queryResult.getReturnMsg());
                	result.setApiResultMessage(queryResult.getReturnMsg());
                	result.setApiResultSubMessage(queryResult.getTradeStateDesc());
                	result.setSuccess(true);
                	result.setInstReturnOrderNo(queryResult.getTransactionId());
                	result.setExtension(JSON.toJSONString(queryResult));
                	logger.info("??????????????????:"+JSON.toJSONString(result));
                	return ResultWrapper.ok().putData(result);
                }else{
                	result.setFundChannelCode(req.getFundChannelCode());
                	result.setInstOrderNo(req.getInstOrderNo());
                	result.setApiResultCode(queryResult.getResultCode());
                	result.setRealAmount(req.getAmount());
                	result.setApiResultSubCode(queryResult.getTradeState());
                	result.setResultMessage(queryResult.getReturnMsg());
                	result.setApiResultMessage(queryResult.getReturnMsg());
                	result.setApiResultSubMessage(queryResult.getTradeStateDesc());
                	result.setSuccess(true);
                	result.setInstReturnOrderNo(queryResult.getTransactionId());
                	result.setExtension(JSON.toJSONString(queryResult));
                	logger.info("??????????????????:"+JSON.toJSONString(result));
                	return ResultWrapper.ok().putData(result);
                }
            }
        }catch (Exception ex) {
            logger.error("????????????", ex);
            result = buildFaildChannelFundResult("??????????????????", ReturnCode.FAILED, FundChannelApiType.SINGLE_QUERY);
           return ResultWrapper.error().putData(result);
        }
	}
	
	
	@PostMapping("/downloadBill")
	public ResultWrapper<Map<String,Object>>  downloadBill(@RequestBody String request) {
		logger.info("PayChannelOrder->Channel?????????????????????????????????"+request);
		ChannelFundResult result = new ChannelFundResult();
		ChannelFundRequest req = JSON.parseObject(request, ChannelFundRequest.class);
		logger.info("PayChannelOrder->Channel???????????????????????????????????????????????????"+req);
		Properties properties = propertyHelper.getProperties(req.getFundChannelCode());
      //??????mock????????????????????????????????????mock??????
        String mock_switch = properties.getProperty(WXPAYFundChannelKey.MOCK_SWITCH);
        if("true".equals(mock_switch)){//??????????????????mock??????
        	result.setApiType(req.getApiType());
        	result.setRealAmount(req.getAmount());
 			result.setInstOrderNo(req.getInstOrderNo());
 			result.setProcessTime(new Date());
        	result = MockResultData.mockResule(result);
        	logger.info("????????????mock?????????");
        	return ResultWrapper.ok().putData(result);
        }
        try {
        	Map<String, String> extension = req.getExtension();
        	/***???????????????/ ***/
        	String bill_dowload_url = properties.getProperty(WXPAYFundChannelKey.KEY_BILL_DOWLOAD_URL);
        	logger.info("????????????????????????->????????????????????????"+bill_dowload_url);
        	String appId = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_APPID);
     		logger.info("????????????????????????->?????????appID??????"+appId);
     		String mchId = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_MCHID);
     		logger.info("????????????????????????->???????????????ID??????"+mchId);
     		String appSecret = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_MCHSECRETKEY);
     		logger.info("????????????????????????->???????????????????????????"+appSecret);
     		String mchSecretKeyPath = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_MCHSECRETKEYPATH);
     		logger.info("????????????????????????->?????????????????????????????????"+mchSecretKeyPath);
     		String billType = properties.getProperty(WXPAYFundChannelKey.KEY_WEIXIN_BILL_TYPE);
     		// ??????????????? ALL????????????????????????????????????????????? SUCCESS????????????????????????????????????  REFUND???????????????????????????
     		logger.info("????????????????????????->???????????????????????????"+billType);
     		String billDirPath = properties.getProperty(WXPAYFundChannelKey.KEY_BILL_DIR_PATH);
     		logger.info("????????????????????????->???????????????????????????"+billDirPath);
     		
     		Map<String,String> map = new HashMap<String,String>();
     		map.put("bill_dowload_url", bill_dowload_url);
     		map.put("billDate", extension.get("billDate"));
     		map.put("billDirPath", billDirPath);
     		map.put("bill_type", billType);
     		map.put("appid", appId);
     		map.put("mch_id", mchId);
     		map.put("appSecret", appSecret);
     		
     		File file = winXinFileDown.fileDown(map);
     		result.setSuccess(true);
     		Map<String, String> extensionMap = new HashMap<String, String>();
     		String bill_file = file.getCanonicalPath();
     		extensionMap.put("bill_file", bill_file);
     		result.setExtension(JSON.toJSONString(extensionMap));
     		result.setFundChannelCode(req.getFundChannelCode());
        	result.setInstOrderNo(req.getInstOrderNo());
        	result.setApiResultCode("0000");
        	result.setRealAmount(req.getAmount());
        	result.setResultMessage("????????????????????????");
        	result.setApiResultMessage("????????????????????????");
        	result.setSuccess(true);
        	return ResultWrapper.ok().putData(result);
        }catch (Exception e) {
        	logger.error("?????????[" + req.getFundChannelCode() + "]??????????????????", e);
        	Map<String, String> map = new HashMap<String,String>();
            map.put("fundsChannel", req.getFundChannelCode());
            result.setExtension(JSON.toJSONString(map));
            result = builFalidFundinResponse(req, "??????????????????", ReturnCode.FAILED, ReturnCode.FAILED,
                StringUtils.EMPTY_STRING);
            ResultWrapper.error().putData(result);
        }
		return null;
	}
	
	

	/**
     * ????????????????????????
     * @param request
     * @return
     */
    protected String getInfo(ChannelRequest request){
    	StringBuffer sb = new StringBuffer();
    	sb.append("FundChannelApi=").append(request.getFundChannelCode())
    	.append("-").append(request.getApiType().getCode())
    	.append(",InstOrderNo=").append(request.getInstOrderNo());
    	return sb.toString();
	}
    
    
    /**
     * ?????????????????????????????????
     * 
     * @param apiResultMessage  api???????????????
     * @param apiResultCode  api?????????
     * @param apiType   api??????
     * @return
     */
    public static ChannelFundResult buildFaildChannelFundResult(String apiResultMessage, String apiResultCode,
                                                                FundChannelApiType apiType) {
        ChannelFundResult response = new ChannelFundResult();
        response.setApiType(apiType);
        response.setApiResultCode(apiResultCode);
        response.setApiResultMessage(apiResultMessage);
        response.setProcessTime(new Date());
        response.setSuccess(false);
        return response;
    }
    
    @PostMapping("/notify/{fundChannelCode}")
	public Object  notify(@PathVariable("fundChannelCode") String fundChannelCode,@RequestBody String data) {
    	logger.info("???????????????"+data);
    	Map<String, String> xmlToMap = WXXMLUtil.xmlToMap(data);
    	logger.info("fundChannelCode???"+fundChannelCode);
    	ChannelRequest channelRequest = new ChannelRequest();
    	channelRequest.setFundChannelCode(fundChannelCode);
    	channelRequest.setApiType(FundChannelApiType.DEBIT);
    	channelRequest.getExtension().put("notifyMsg", data);
    	ChannelFundResult result = wxPayResultNotifyService.notify(channelRequest);
    	//????????????MQ???????????????????????????
    	Map<String,Object> map = new HashMap<String,Object>();
		map.put("message", result);
		//???????????????????????????
		AmqoRequrst requrst = new AmqoRequrst();
    	requrst.setExchange("exchange.payresult.process");
    	requrst.setRoutingKey("key.payresult.process");
    	requrst.setMap(map);
    	logger.info("??????MQ??????:"+JSON.toJSONString(requrst));
		amqpService.sendMessage(requrst);
		logger.info("MQ??????????????????");
        //??????????????????
        //resultNotifyFacade.notifyBiz(instOrderResult.getInstOrderNo(),xmlToMap);
        String return_result = setXml("SUCCESS", "OK");
        return return_result;
    }
    
    /**
     * ??????xml ??????????????????
     * @param returnCode
     * @param returnMsg
     * @return
     */
  	public static String setXml(String returnCode, String returnMsg) {
  		SortedMap<String, String> parameters = new TreeMap<String, String>();
  		parameters.put("return_code", returnCode);
  		parameters.put("return_msg", returnMsg);
  		return "<xml><return_code><![CDATA[" + returnCode + "]]>" + 
  				"</return_code><return_msg><![CDATA[" + returnMsg + "]]></return_msg></xml>";
  	}
    
}
