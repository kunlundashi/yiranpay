package com.channel.bank.adapter.pay.api;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.channel.bank.adapter.pay.constants.ALIPayConstant;
import com.channel.bank.adapter.pay.constants.ReturnCode;
import com.channel.bank.adapter.pay.constants.WXPAYFundChannelKey;
import com.channel.bank.adapter.pay.domain.AmqoRequrst;
import com.channel.bank.adapter.pay.domain.ChannelFundRequest;
import com.channel.bank.adapter.pay.domain.ChannelFundResult;
import com.channel.bank.adapter.pay.domain.ChannelRequest;
import com.channel.bank.adapter.pay.domain.QueryRequest;
import com.channel.bank.adapter.pay.domain.ResultWrapper;
import com.channel.bank.adapter.pay.enums.FundChannelApiType;
import com.channel.bank.adapter.pay.mock.MockResultData;
import com.channel.bank.adapter.pay.property.PropertyHelper;
import com.channel.bank.adapter.pay.service.IAmqpService;
import com.channel.bank.adapter.pay.service.impl.ALIPayResultNotifyService;
import com.channel.bank.adapter.pay.utils.ChannelFundResultUtil;
import com.channel.bank.adapter.pay.utils.MapUtils;

import cn.hutool.json.JSONUtil;
/**
 * ???????????????
 * @author pandaa
 *
 */
@RestController
@RequestMapping("/api/yiranpay/channelpay/alipagepay")
public class ALIPagePayFundinFacade extends BasePayFundinFacade {

	private Logger logger = LoggerFactory.getLogger(ALIPagePayFundinFacade.class);
	
	@Autowired
	private PropertyHelper propertyHelper;
	
	@Autowired
	private IAmqpService amqpService;
	
	@Autowired
	private ALIPayResultNotifyService aliPayResultNotifyService;
	/**
	 * ????????????
	 * @param request
	 * @return
	 */
	@PostMapping("/pay")
	public ResultWrapper<Map<String,Object>>  fundin(@RequestBody String request) {
		logger.info("PayChannelOrder->Channel????????????????????????????????????"+request);
		ChannelFundResult result = new ChannelFundResult();
		ChannelFundRequest req = JSON.parseObject(request, ChannelFundRequest.class);
		logger.info("PayChannelOrder->Channel????????????????????????????????????????????????"+req);
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
            String subject = extension.get("subject");
    		BigDecimal amount = req.getAmount();
    		String totalAmount = amount.toString();
    		String orderNo = req.getInstOrderNo();
            String notifyUrl = properties.getProperty(ALIPayConstant.PAY_NOTIFY_URL);
            String returnUrl = extension.get(ALIPayConstant.RETURN_URL);
            //??????????????????
            String gateway = properties.getProperty(ALIPayConstant.GATEWAY);
            //????????????ID
            String appId = properties.getProperty(ALIPayConstant.APPID);
            //????????????
            String privateKey = properties.getProperty(ALIPayConstant.PRIVATE_KEY);
            //???????????????
            String publicKey = properties.getProperty(ALIPayConstant.PUBLIC_KEY);
            //????????????
            String charset = properties.getProperty(ALIPayConstant.CHARSET);
            //????????????
            String format = properties.getProperty(ALIPayConstant.FORMAT);
            //????????????
            String signType = properties.getProperty(ALIPayConstant.SIGNTYPE);
            AlipayClient client = getAlipayClient(gateway,appId,privateKey,publicKey,charset,format,signType);
            
            AlipayTradePagePayRequest alipayTradePagePayRequest = new AlipayTradePagePayRequest();
            alipayTradePagePayRequest.setNotifyUrl(notifyUrl);
            alipayTradePagePayRequest.setReturnUrl(returnUrl);
            AlipayTradePagePayModel alipayTradePagePayModel = new AlipayTradePagePayModel();
            alipayTradePagePayModel.setOutTradeNo(orderNo);
            alipayTradePagePayModel.setSubject(subject);
            alipayTradePagePayModel.setTotalAmount(totalAmount);
            //PAGE_PAY  WAP_WAY   APP_PAY
            alipayTradePagePayModel.setProductCode("FAST_INSTANT_TRADE_PAY");
            
            alipayTradePagePayRequest.setBizModel(alipayTradePagePayModel);
            AlipayTradePagePayResponse alipayTradePagePayResponse = client.pageExecute(alipayTradePagePayRequest);
            logger.info("??????????????????????????????{}", JSONUtil.toJsonStr(alipayTradePagePayResponse));
            if (alipayTradePagePayResponse.isSuccess()) {
            	logger.info("body:"+alipayTradePagePayResponse.getBody());
            	result.setApiResultMessage("???????????????????????????????????????");
    			result.setResultMessage("???????????????????????????????????????");
    			result.setApiResultCode("0");
    			result.setInstOrderNo(req.getInstOrderNo());
    			result.setRealAmount(req.getAmount());
    			result.setApiType(FundChannelApiType.SIGN);
    			result.setSuccess(true);
    			result.setFromHtml(alipayTradePagePayResponse.getBody());
    			result.setExtension(JSON.toJSONString(alipayTradePagePayResponse));
                return ResultWrapper.ok().putData(result);
            } else {
            	result.setApiResultMessage("???????????????????????????");
    			result.setResultMessage("???????????????????????????");
    			result.setApiResultCode("-1");
    			result.setInstOrderNo(req.getInstOrderNo());
    			result.setRealAmount(req.getAmount());
    			result.setApiType(FundChannelApiType.SIGN);
    			result.setSuccess(false);
                return ResultWrapper.ok().putData(result);
            }
			
        } catch (Exception e) {
        	logger.error("?????????????????????", e);
            result.setResultMessage(e.getMessage());
            result.setSuccess(false);
			result.setApiResultCode(ReturnCode.EXCEPTION);
            result.setApiResultMessage("?????????????????????????????????");
        }
        return ResultWrapper.ok().putData(result);
       
    }
	
	
	/**
	 * ??????????????????
	 * @param requestJson
	 * @return
	 */
	@PostMapping("/query")
	public ResultWrapper<Map<String,Object>>  query(@RequestBody String requestJson) {
		
		logger.info("PayChannelOrder->Channel??????????????????????????????????????????"+requestJson);
		ChannelFundResult result = new ChannelFundResult();
		QueryRequest request = JSON.parseObject(requestJson, QueryRequest.class);
		result.setApiType(request.getApiType());
		logger.info("PayChannelOrder->Channel??????????????????????????????????????????????????????"+request);
		Properties properties = propertyHelper.getProperties(request.getFundChannelCode());
        result.setSuccess(false);
        result.setFundChannelCode(result.getFundChannelCode());
        String operInfo = getInfo(request);
        try {
        	//??????????????????
            String gateway = properties.getProperty(ALIPayConstant.GATEWAY);
            //????????????ID
            String appId = properties.getProperty(ALIPayConstant.APPID);
            //????????????
            String privateKey = properties.getProperty(ALIPayConstant.PRIVATE_KEY);
            //???????????????
            String publicKey = properties.getProperty(ALIPayConstant.PUBLIC_KEY);
            //????????????
            String charset = properties.getProperty(ALIPayConstant.CHARSET);
            //????????????
            String format = properties.getProperty(ALIPayConstant.FORMAT);
            //????????????
            String signType = properties.getProperty(ALIPayConstant.SIGNTYPE);
            AlipayClient client = getAlipayClient(gateway,appId,privateKey,publicKey,charset,format,signType);

            AlipayTradeQueryRequest alipayTradeQueryRequest = new AlipayTradeQueryRequest();

            AlipayTradeQueryModel alipayTradeQueryModel = new AlipayTradeQueryModel();
            alipayTradeQueryModel.setOutTradeNo(request.getInstOrderNo());
            //alipayTradeQueryModel.setTradeNo(tradeNo);

            alipayTradeQueryRequest.setBizModel(alipayTradeQueryModel);

            AlipayTradeQueryResponse alipayTradeQueryResponse = client.execute(alipayTradeQueryRequest);
            logger.info("????????????????????????????????????{}", JSONUtil.toJsonStr(alipayTradeQueryResponse));
            if (alipayTradeQueryResponse.isSuccess()) {
                return ResultWrapper.ok().putData((JSONUtil.formatJsonStr(alipayTradeQueryResponse.getBody())));
            }
		
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(operInfo+"????????????", e);
            return ResultWrapper.error().putData(buildFaildQueryResponse("????????????",ReturnCode.EXCEPTION));
		}
		return ResultWrapper.ok().putData(result);
	}
	
	/**
	 * ??????????????????
	 * @param fundChannelCode
	 * @param data
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws AlipayApiException 
	 */
	@PostMapping("/notify/{fundChannelCode}")
	public Object  notify(@PathVariable("fundChannelCode") String fundChannelCode,@RequestBody String data) throws UnsupportedEncodingException, AlipayApiException {
		logger.info("????????????????????????????????????"+data);
    	if (StringUtils.isBlank(data)) {
            return "fail";
        }
    	Properties properties = propertyHelper.getProperties(fundChannelCode);
    	data = URLDecoder.decode(data, "UTF-8"); 
    	Map<String, String> dataToMap = MapUtils.getMapforUrl(data);
    	logger.info("?????????????????????????????????-??????Map?????????"+dataToMap);
    	logger.info("fundChannelCode???"+fundChannelCode);
    	//???????????????
        String publicKey = properties.getProperty(ALIPayConstant.PUBLIC_KEY);
        logger.info("??????????????????"+publicKey);
        //????????????
        String charset = properties.getProperty(ALIPayConstant.CHARSET);
        //????????????
        String signType = properties.getProperty(ALIPayConstant.SIGNTYPE);
    	/*boolean signResult = AlipaySignature.rsaCheckV1(dataToMap, publicKey,charset, signType);
    	if (!signResult) {
    		String sWord = AlipaySignature.getSignCheckContentV2(dataToMap);
            logger.info("???????????????????????????????????????????????????{}", sWord);
            return sWord;
    	}*/
    	ChannelRequest channelRequest = new ChannelRequest();
    	channelRequest.setFundChannelCode(fundChannelCode);
    	channelRequest.setApiType(FundChannelApiType.VERIFY_SIGN);
    	channelRequest.getExtension().put("notifyMsg", JSON.toJSONString(dataToMap));
    	ChannelFundResult result = aliPayResultNotifyService.aliNotify(channelRequest);
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
		//?????????
		String responseData ="success"; 
        return responseData;
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
     * @param apiResultMessage
     * @param apiResultCode
     * @return
     */
    protected ChannelFundResult buildFaildQueryResponse(String apiResultMessage, String apiResultCode) {
        return ChannelFundResultUtil.buildFaildChannelFundResult(apiResultMessage, apiResultCode, FundChannelApiType.SINGLE_QUERY);
    }
}
