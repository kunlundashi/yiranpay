package com.channel.bank.adapter.pay.api;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson.JSON;
import com.channel.bank.adapter.pay.constants.PublicPayConstant;
import com.channel.bank.adapter.pay.constants.ReturnCode;
import com.channel.bank.adapter.pay.constants.WXPAYFundChannelKey;
import com.channel.bank.adapter.pay.domain.AmqoRequrst;
import com.channel.bank.adapter.pay.domain.ChannelFundRequest;
import com.channel.bank.adapter.pay.domain.ChannelFundResult;
import com.channel.bank.adapter.pay.domain.ChannelRequest;
import com.channel.bank.adapter.pay.domain.EBankChannelFundRequest;
import com.channel.bank.adapter.pay.domain.PayQueryRequest;
import com.channel.bank.adapter.pay.domain.PayRefundRequest;
import com.channel.bank.adapter.pay.domain.PublicPayRequest;
import com.channel.bank.adapter.pay.domain.QueryRequest;
import com.channel.bank.adapter.pay.domain.RefundQueryRequest;
import com.channel.bank.adapter.pay.domain.ResultWrapper;
import com.channel.bank.adapter.pay.enums.BestPayTypeEnum;
import com.channel.bank.adapter.pay.enums.FundChannelApiType;
import com.channel.bank.adapter.pay.mock.MockResultData;
import com.channel.bank.adapter.pay.mode.OrderQueryRequest;
import com.channel.bank.adapter.pay.mode.OrderQueryResponse;
import com.channel.bank.adapter.pay.mode.RefundRequest;
import com.channel.bank.adapter.pay.property.PropertyHelper;
import com.channel.bank.adapter.pay.service.IAmqpService;
import com.channel.bank.adapter.pay.service.impl.ChinaH5PayResultNotifyService;
import com.channel.bank.adapter.pay.service.impl.WXPayResultNotifyService;
import com.channel.bank.adapter.pay.utils.AmountUtils;
import com.channel.bank.adapter.pay.utils.BankChannelRefundUtil;
import com.channel.bank.adapter.pay.utils.ChannelFundResultUtil;
import com.channel.bank.adapter.pay.utils.MapUtils;
import com.channel.bank.adapter.pay.utils.Util;
import com.channel.bank.adapter.pay.utils.WXXMLUtil;
import com.channel.bank.adapter.pay.utils.https.HttpClientUtil;

/**
 * ??????H5??????
 * @author pandaa
 *
 */

@RestController
@RequestMapping("/api/yiranpay/channelpay/chinah5pay")
public class ChinaH5PayFundinFacade extends BaseBankInterface{
	private Logger logger = LoggerFactory.getLogger(ChinaH5PayFundinFacade.class);
	public static final java.lang.String EMPTY_STRING = "";
	@Autowired
	private PropertyHelper propertyHelper;
	
	@Autowired
	private ChinaH5PayResultNotifyService chinaH5PayResultNotifyService;
	
	@Autowired
	private IAmqpService amqpService;
	/**
	 * ????????????
	 * @param request
	 * @return
	 */
	@PostMapping("/pay")
	public ResultWrapper<Map<String,Object>>  fundin(@RequestBody String request) {
		logger.info("PayChannelOrder->Channel???????????????????????????????????????"+request);
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
        	//??????????????????
        	PublicPayRequest payRequest = buidRequest(req,properties);
        	Map<String,String> json = getMapObject(payRequest);
        	//MD5??????????????????
        	String md5Key = properties.getProperty(PublicPayConstant.MD5KEY);
        	//??????URL
        	String apiUrl_makeOrder = properties.getProperty(PublicPayConstant.APIURL_MAKEORDER);
        	String sign = Util.makeSign(md5Key, json);
        	json.put("sign", sign);
        	payRequest.setSign(sign);
        	String htmlData = getChinaHtmlData(formData(payRequest), apiUrl_makeOrder);
        	System.out.println("Html????????????:"+htmlData);
        	logger.info("Html????????????:"+htmlData);
        	result.setExtension(getMapToJson(json));
        	logger.info("??????HTML?????????map??????:"+getMapToJson(json));
        	System.out.println("??????HTML?????????map??????:"+getMapToJson(json));
			result.setInstUrl(apiUrl_makeOrder);
			result.setApiResultMessage("???????????????????????????????????????");
			result.setResultMessage("???????????????????????????????????????");
			result.setApiResultCode("0");
			result.setInstOrderNo(req.getInstOrderNo());
			result.setRealAmount(req.getAmount());
			result.setApiType(FundChannelApiType.SIGN);
			result.setSuccess(true);
        } catch (Exception e) {
        	logger.error("?????????????????????", e);
            result.setResultMessage(e.getMessage());
            result.setSuccess(false);
			result.setApiResultCode(ReturnCode.EXCEPTION);
            result.setApiResultMessage("????????????");
        }
        EBankChannelFundRequest ebankRequest = JSON.parseObject(request,
                EBankChannelFundRequest.class);
        result = super.convertChinaPayApplyResult(result, ebankRequest);
        return ResultWrapper.ok().putData(result);
       
    }
	/**
	 * ??????????????????
	 * @param fundChannelCode
	 * @param data
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@PostMapping("/notify/{fundChannelCode}")
	public Object  notify(@PathVariable("fundChannelCode") String fundChannelCode,@RequestBody String data) throws UnsupportedEncodingException {
    	logger.info("???????????????"+data);
    	data = URLDecoder.decode(data, "UTF-8"); 
    	Map<String, String> dataToMap = MapUtils.getMapforUrl(data);
    	logger.info("fundChannelCode???"+fundChannelCode);
    	ChannelRequest channelRequest = new ChannelRequest();
    	channelRequest.setFundChannelCode(fundChannelCode);
    	channelRequest.setApiType(FundChannelApiType.VERIFY_SIGN);
    	channelRequest.getExtension().put("notifyMsg", JSON.toJSONString(dataToMap));
    	ChannelFundResult result = chinaH5PayResultNotifyService.notify(channelRequest);
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
		String responseData ="SUCCESS"; 
        return responseData;
    }

	/**
	 * ??????????????????
	 * @param requestJson
	 * @return
	 */
	@PostMapping("/query")
	public ResultWrapper<Map<String,Object>>  query(@RequestBody String requestJson) {
		
		logger.info("PayChannelOrder->Channel?????????????????????????????????????????????"+requestJson);
		ChannelFundResult result = new ChannelFundResult();
		QueryRequest request = JSON.parseObject(requestJson, QueryRequest.class);
		result.setApiType(request.getApiType());
		logger.info("PayChannelOrder->Channel?????????????????????????????????????????????????????????"+request);
		Properties properties = propertyHelper.getProperties(request.getFundChannelCode());
        result.setSuccess(false);
        result.setFundChannelCode(result.getFundChannelCode());
        String operInfo = getInfo(request);
        try {
			//MD5??????????????????
        	String md5Key = properties.getProperty(PublicPayConstant.MD5KEY);
			//????????????url
			String query_api_url = properties.getProperty(PublicPayConstant.APIURL);
			System.out.println("???????????????????????????"+query_api_url);
			//??????????????????
			PayQueryRequest req= buidRequest(request,properties);
			//????????????map
			Map<String,String> map = getMapObject(req);
			map.put("sign", Util.makeSign(md5Key, map));
			String strReqJsonStr = JSON.toJSONString(map);
	        System.out.println("strReqJsonStr:"+strReqJsonStr);
	        logger.info("strReqJsonStr:"+strReqJsonStr);
	        logger.info(operInfo + "???????????????????????????????????????:" + strReqJsonStr);
			String respData = HttpClientUtil.sendChinaPost(query_api_url, strReqJsonStr);
			Map<String,String> respMap=JSON.parseObject(respData, Map.class);
			logger.info(operInfo + "???????????????????????????????????????????????????" + respMap);
			System.out.println(operInfo + "???????????????????????????????????????????????????" + respMap);
			//??????
			if(Util.checkSign(md5Key, respMap)){
				System.out.println("???????????????????????????????????????:????????????");
				logger.info("???????????????????????????????????????:????????????");
				if("SUCCESS".equals(respMap.get("errCode"))){
					result.setInstOrderNo(respMap.get("merOrderId"));
	                result.setApiResultCode(respMap.get("errCode"));
	                result.setApiResultSubCode(respMap.get("status"));
	                result.setApiResultMessage(respMap.get("errMsg"));
	                String amount = String.valueOf(respMap.get("totalAmount"));
	                BigDecimal realAmount = new BigDecimal(AmountUtils.Fen2Yuan(Long.parseLong(amount)));
					result.setRealAmount(realAmount);
	                result.setProcessTime(new Date());
	                result.setSuccess(true);
				}else{
					result.setInstOrderNo(request.getInstOrderNo());
	                result.setApiResultCode(respMap.get("errCode"));
	                result.setApiResultMessage(respMap.get("errMsg"));
	                result.setSuccess(false);
				}
			}else{
				logger.info("???????????????");
				result.setInstOrderNo(request.getInstOrderNo());
                result.setApiResultCode("0003");
                result.setApiResultMessage("????????????");
                result.setSuccess(false);
			}
			
		
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(operInfo+"????????????", e);
            return ResultWrapper.error().putData(buildFaildQueryResponse("????????????",ReturnCode.EXCEPTION));
		}
		return ResultWrapper.ok().putData(result);
	}
	
	/**
     * ?????? 
     * @param request
     * @return
     */
    @PostMapping("/repay")
	public ResultWrapper<Map<String,Object>>  refundin(@RequestBody String request) {
		logger.info("PayChannelOrder->Channel???????????????????????????????????????"+request);
		ChannelFundResult result = new ChannelFundResult();
		RefundRequest req = JSON.parseObject(request, RefundRequest.class);
		logger.info("PayChannelOrder->Channel???????????????????????????????????????????????????"+req);
		Properties properties = propertyHelper.getProperties(req.getFundChannelCode());
		//??????mock????????????????????????????????????mock??????
        String mock_switch = properties.getProperty(PublicPayConstant.MOCK_SWITCH);
        if("true".equals(mock_switch)){//??????????????????mock??????
        	result.setApiType(req.getApiType());
        	result.setRealAmount(req.getAmount());
 			result.setInstOrderNo(req.getInstOrderNo());
 			result.setProcessTime(new Date());
        	result = MockResultData.mockResule(result);
        	logger.info("????????????mock?????????");
        	return ResultWrapper.ok().putData(result);
        }
		String operInfo = getInfo(req);
        try {
	        //MD5??????????????????
        	String md5Key = properties.getProperty(PublicPayConstant.MD5KEY);
	        //????????????url 
	        String url = properties.getProperty(PublicPayConstant.APIURL);
	        //??????????????????
	        PayRefundRequest refundReq = buidRefRequest(req,properties);
	        //????????????map
        	Map<String, String> map = getMapObject(refundReq);
        	map.put("sign", Util.makeSign(md5Key, map));
        	String strReqJsonStr = JSON.toJSONString(map);
        	logger.info(operInfo + "??????????????????????????????:"+strReqJsonStr);
        	System.out.println(operInfo + "??????????????????????????????:"+strReqJsonStr);
        	
        	String respData = HttpClientUtil.sendChinaPost(url, strReqJsonStr);
			Map<String,String> respMap=JSON.parseObject(respData, Map.class);
			System.out.println(operInfo + "?????????????????????????????????????????????" + respMap);
			logger.info(operInfo + "?????????????????????????????????????????????" + respMap);
			//??????
			if(Util.checkSign(md5Key, respMap)){
				System.out.println("???????????????????????????????????????:????????????");
				logger.info("???????????????????????????????????????:????????????");
				result.setInstOrderNo(respMap.get("merOrderId"));
                result.setApiResultCode(respMap.get("errCode"));
                result.setApiResultSubCode(respMap.get("refundStatus"));
                result.setApiResultMessage(respMap.get("errCode"));
                String amount = String.valueOf(respMap.get("totalAmount"));
                BigDecimal realAmount = new BigDecimal(AmountUtils.Fen2Yuan(Long.parseLong(amount)));
				result.setRealAmount(realAmount);
                result.setProcessTime(new Date());
                result.setExtension(JSON.toJSONString(respData));
                result.setSuccess(true);
			}else{
				System.out.println("???????????????");
				result.setInstOrderNo(req.getInstOrderNo());
                result.setApiResultCode("0003");
                result.setApiResultMessage("????????????");
                result.setSuccess(false);
			}
			return ResultWrapper.ok().putData(result);
        	
        } catch (Exception e) {
            logger.error(operInfo+"????????????:", e);
            return ResultWrapper.error().putData(builFalidRefundResponse(req, 
                    "????????????", ReturnCode.EXCEPTION, ReturnCode.EXCEPTION,
                    EMPTY_STRING));
        }
    }
    
    /**
	 * ????????????????????????
	 * @param requestJson
	 * @return
	 */
	@PostMapping("/refquery")
	public ResultWrapper<Map<String,Object>>  refquery(@RequestBody String requestJson) {
		
		logger.info("PayChannelOrder->Channel?????????????????????????????????????????????"+requestJson);
		ChannelFundResult result = new ChannelFundResult();
		QueryRequest request = JSON.parseObject(requestJson, QueryRequest.class);
		result.setApiType(request.getApiType());
		logger.info("PayChannelOrder->Channel?????????????????????????????????????????????????????????"+request);
		Properties properties = propertyHelper.getProperties(request.getFundChannelCode());
        result.setSuccess(false);
        result.setFundChannelCode(result.getFundChannelCode());
        String operInfo = getInfo(request);
        try {
            //MD5??????????????????
          	String md5Key = properties.getProperty(PublicPayConstant.MD5KEY);
  			//????????????url
  			String query_api_url = properties.getProperty(PublicPayConstant.APIURL);
  			System.out.println("???????????????????????????"+query_api_url);
  			//??????????????????
  			RefundQueryRequest reRequest = buidRefQueryRequest(request,properties);
  			//????????????map
  			Map<String,String> map = getRefQueryMapObject(reRequest);
  			map.put("sign", Util.makeSign(md5Key, map));
  			String strReqJsonStr = JSON.toJSONString(map);
  	        System.out.println("strReqJsonStr:"+strReqJsonStr);
  	        logger.info(operInfo + "?????????????????????????????????????????????:" + strReqJsonStr);
  			String respData = HttpClientUtil.sendChinaPost(query_api_url, strReqJsonStr);
  			Map<String,String> respMap=JSON.parseObject(respData, Map.class);
  			System.out.println(operInfo + "?????????????????????????????????????????????????????????" + respMap);
  			logger.info(operInfo + "?????????????????????????????????????????????????????????" + respMap);
  			//??????
  			if(Util.checkSign(md5Key, respMap)){
  				if("SUCCESS".equals(respMap.get("errCode"))){
  					result.setInstOrderNo(respMap.get("merOrderId"));
  	                result.setApiResultCode(respMap.get("errCode"));
  	                result.setApiResultSubCode(respMap.get("refundStatus"));
  	                result.setApiResultMessage(respMap.get("errMsg"));
  	                String amount = String.valueOf(respMap.get("totalAmount"));
	                BigDecimal realAmount = new BigDecimal(AmountUtils.Fen2Yuan(Long.parseLong(amount)));
  					result.setRealAmount(realAmount);
  	                result.setProcessTime(new Date());
  	                result.setSuccess(true);
  				}else{
  					result.setInstOrderNo(request.getInstOrderNo());
  	                result.setApiResultCode(respMap.get("errCode"));
  	                result.setApiResultMessage(respMap.get("errMsg"));
  	                result.setSuccess(false);
  				}
  				
  			}else{
  				System.out.println("???????????????");
  				result.setInstOrderNo(request.getInstOrderNo());
                  result.setApiResultCode("0003");
                  result.setApiResultMessage("????????????");
                  result.setSuccess(false);
  			}
  			return ResultWrapper.ok().putData(result);
          } catch (Exception ex) {
              logger.error(operInfo+"????????????????????????",ex);
              return ResultWrapper.error().putData((buildFaildQueryResponse("??????????????????", ReturnCode.EXCEPTION)));
          }
	}
    
    private Map<String, String> getRefQueryMapObject(RefundQueryRequest req) {
    	Map<String,String> map = new HashMap<String,String>();
    	if(!StringUtils.isBlank(req.getMsgId())){
    		map.put("msgId", req.getMsgId());
    	}
    	map.put("msgSrc", req.getMsgSrc());
    	map.put("msgType", req.getMsgType());
    	if(!StringUtils.isBlank(req.getRequestTimestamp())){
    		map.put("requestTimestamp", req.getRequestTimestamp());
    	}
    	if(!StringUtils.isBlank(req.getSrcReserve())){
    		map.put("srcReserve", req.getSrcReserve());
    	}
    	map.put("mid", req.getMid());
    	map.put("tid", req.getTid());
    	if(!StringUtils.isBlank(req.getInstMid())){
    		map.put("instMid", req.getInstMid());
    	}
    	map.put("merOrderId", req.getMerOrderId());
    	map.put("signType", req.getSignType());
    	if(!StringUtils.isBlank(req.getSign())){
    		map.put("sign", req.getSign());
    	}
    	return map;
	}
	private RefundQueryRequest buidRefQueryRequest(QueryRequest req, Properties properties) {
    	RefundQueryRequest request = new RefundQueryRequest();
	   String date = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
       String rand = RandomStringUtils.randomNumeric(7);
       String msgId = date + rand;//??????ID???????????????
       request.setMsgId(msgId);
       //????????????
       request.setMsgSrc(properties.getProperty(PublicPayConstant.MSG_SRC));
       //????????????
       request.setMsgType("refundQuery");
       //???????????????????????????yyyy-MM-dd HH:mm:ss
		request.setRequestTimestamp(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
		//????????????????????????
		//request.setSrcReserve("");
		//???????????????
		/*request.setMerOrderId(req.getOriginalInstOrderNo());
		System.out.println("?????????????????????:"+req.getOriginalInstOrderNo());*/
		request.setMerOrderId(req.getInstOrderNo());
		System.out.println("?????????????????????:"+req.getInstOrderNo());
		//?????????
		request.setMid(properties.getProperty(PublicPayConstant.MID));
		//?????????
		request.setTid(properties.getProperty(PublicPayConstant.TID));
		//????????????
		request.setInstMid(properties.getProperty(PublicPayConstant.INSTMID));
		//????????????
		request.setSignType(properties.getProperty(PublicPayConstant.SIGN_TYPE));
		
		return request;
	}
    
	private String formData(PublicPayRequest req){
    	StringBuffer sb = new StringBuffer();
    	if(!StringUtils.isBlank(req.getMsgId())){
    		sb.append("<input type='hidden' name='msgId' value='"+req.getMsgId()+"' /> \r\n");
    	}
    	sb.append("<input type='hidden' name='msgSrc' value='"+req.getMsgSrc()+"' /> \r\n");
    	sb.append("<input type='hidden' name='msgType' value='"+req.getMsgType()+"' /> \r\n");
    	if(!StringUtils.isBlank(req.getRequestTimestamp())){
    		sb.append("<input type='hidden' name='requestTimestamp' value='"+req.getRequestTimestamp()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getExpireTime())){
    		sb.append("<input type='hidden' name='expireTime' value='"+req.getExpireTime()+"' /> \r\n");
    	}
    	sb.append("<input type='hidden' name='merOrderId' value='"+req.getMerOrderId()+"' /> \r\n");
    	if(!StringUtils.isBlank(req.getSrcReserve())){
    		sb.append("<input type='hidden' name='srcReserve' value='"+req.getSrcReserve()+"' /> \r\n");
    	}
    	sb.append("<input type='hidden' name='mid' value='"+req.getMid()+"' /> \r\n");
    	sb.append("<input type='hidden' name='tid' value='"+req.getTid()+"' /> \r\n");
    	if(!StringUtils.isBlank(req.getInstMid())){
    		sb.append("<input type='hidden' name='instMid' value='"+req.getInstMid()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getAttachedData())){
    		sb.append("<input type='hidden' name='attachedData' value='"+req.getAttachedData()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getOrderDesc())){
    		sb.append("<input type='hidden' name='orderDesc' value='"+req.getOrderDesc()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getGoodsTag())){
    		sb.append("<input type='hidden' name='goodsTag' value='"+req.getGoodsTag()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getOriginalAmount())){
    		sb.append("<input type='hidden' name='originalAmount' value='"+req.getOriginalAmount()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getTotalAmount())){
    		sb.append("<input type='hidden' name='totalAmount' value='"+req.getTotalAmount()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getNotifyUrl())){
    		sb.append("<input type='hidden' name='notifyUrl' value='"+req.getNotifyUrl()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getReturnUrl())){
    		sb.append("<input type='hidden' name='returnUrl' value='"+req.getReturnUrl()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getSystemId())){
    		sb.append("<input type='hidden' name='systemId' value='"+req.getSystemId()+"' /> \r\n");
    	}
    	sb.append("<input type='hidden' name='signType' value='"+req.getSignType()+"' /> \r\n");
    	if(!StringUtils.isBlank(req.getSubOpenId())){
    		sb.append("<input type='hidden' name='subOpenId' value='"+req.getSubOpenId()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getSubAppId())){
    		sb.append("<input type='hidden' name='subAppId' value='"+req.getSubAppId()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getName())){
    		sb.append("<input type='hidden' name='name' value='"+req.getName()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getMobile())){
    		sb.append("<input type='hidden' name='mobile' value='"+req.getMobile()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getCertType())){
    		sb.append("<input type='hidden' name='certType' value='"+req.getCertType()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getCertNo())){
    		sb.append("<input type='hidden' name='certNo' value='"+req.getCertNo()+"' /> \r\n");
    	}
    	
    	if(!StringUtils.isBlank(req.getFixBuyer())){
    		sb.append("<input type='hidden' name='fixBuyer' value='"+req.getFixBuyer()+"' /> \r\n");
    	}
    	
    	if(!StringUtils.isBlank(req.getLimitCreditCard())){
    		sb.append("<input type='hidden' name='limitCreditCard' value='"+req.getLimitCreditCard()+"' /> \r\n");
    	}
    	
    	if(!StringUtils.isBlank(req.getSecureTransaction())){
    		sb.append("<input type='hidden' name='secureTransaction' value='"+req.getSecureTransaction()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getSign())){
    		sb.append("<input type='hidden' name='sign' value='"+req.getSign()+"' /> \r\n");
    	}
    	if(!StringUtils.isBlank(req.getInstallmentNumber())){
    		sb.append("<input type='hidden' name='installmentNumber' value='"+req.getInstallmentNumber()+"' /> \r\n");
    	}
    	
		return sb.toString();
    }
    private Map<String,String> getMapObject (PublicPayRequest req) {
    	Map<String,String> json = new HashMap<String,String>();
    	if(!StringUtils.isBlank(req.getMsgId())){
    		json.put("msgId", req.getMsgId());
    	}
    	json.put("msgSrc", req.getMsgSrc());
    	json.put("msgType", req.getMsgType());
    	if(!StringUtils.isBlank(req.getRequestTimestamp())){
    		json.put("requestTimestamp", req.getRequestTimestamp());
    	}
    	if(!StringUtils.isBlank(req.getExpireTime())){
    		json.put("expireTime", req.getExpireTime());
    	}
    	json.put("merOrderId", req.getMerOrderId());
    	if(!StringUtils.isBlank(req.getSrcReserve())){
    		json.put("srcReserve", req.getSrcReserve());
    	}
    	json.put("mid", req.getMid());
    	json.put("tid", req.getTid());
    	if(!StringUtils.isBlank(req.getInstMid())){
    		json.put("instMid", req.getInstMid());
    	}
    	if(!StringUtils.isBlank(req.getAttachedData())){
    		json.put("attachedData", req.getAttachedData());
    	}
    	if(!StringUtils.isBlank(req.getOrderDesc())){
    		json.put("orderDesc", req.getOrderDesc());
    	}
    	if(!StringUtils.isBlank(req.getGoodsTag())){
    		json.put("goodsTag", req.getGoodsTag());
    	}
    	if(!StringUtils.isBlank(req.getOriginalAmount())){
    		json.put("originalAmount", req.getOriginalAmount());
    	}
    	if(!StringUtils.isBlank(req.getTotalAmount())){
    		json.put("totalAmount", req.getTotalAmount());
    	}
    	if(!StringUtils.isBlank(req.getNotifyUrl())){
    		json.put("notifyUrl", req.getNotifyUrl());
    	}
    	if(!StringUtils.isBlank(req.getReturnUrl())){
    		json.put("returnUrl", req.getReturnUrl());
    	}
    	if(!StringUtils.isBlank(req.getSystemId())){
    		json.put("systemId", req.getSystemId());
    	}
    	json.put("signType", req.getSignType());
    	if(!StringUtils.isBlank(req.getSubOpenId())){
    		json.put("subOpenId", req.getSubOpenId());
    	}
    	if(!StringUtils.isBlank(req.getSubAppId())){
    		json.put("subAppId", req.getSubAppId());
    	}
    	if(!StringUtils.isBlank(req.getName())){
    		json.put("name", req.getName());
    	}
    	if(!StringUtils.isBlank(req.getMobile())){
    		json.put("mobile", req.getMobile());
    	}
    	if(!StringUtils.isBlank(req.getCertType())){
    		json.put("certType", req.getCertType());
    	}
    	if(!StringUtils.isBlank(req.getCertNo())){
    		json.put("certNo", req.getCertNo());
    	}
    	
    	if(!StringUtils.isBlank(req.getFixBuyer())){
    		json.put("fixBuyer", req.getFixBuyer());
    	}
    	
    	if(!StringUtils.isBlank(req.getLimitCreditCard())){
    		json.put("limitCreditCard", req.getLimitCreditCard());
    	}
    	
    	if(!StringUtils.isBlank(req.getSecureTransaction())){
    		json.put("secureTransaction", req.getSecureTransaction());
    	}
    	if(!StringUtils.isBlank(req.getSign())){
    		json.put("sign", req.getSign());
    	}
    	if(!StringUtils.isBlank(req.getInstallmentNumber())){
    		json.put("installmentNumber", req.getInstallmentNumber());
    	}
		return json;
	}

    
    /**
     * ??????????????????
     * @param req
     * @param properties
     * @return
     * @throws UnknownHostException 
     * @throws UnsupportedEncodingException 
     */
	private PublicPayRequest buidRequest(ChannelFundRequest req,
			Properties properties) throws UnknownHostException, UnsupportedEncodingException {
		
	   Map<String, String> extension = req.getExtension();
	   PublicPayRequest request = new PublicPayRequest();
	   String date = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
       String rand = RandomStringUtils.randomNumeric(7);
       String msgId = date + rand;//??????ID???????????????
       request.setMsgId(msgId);
       //????????????
       request.setMsgSrc(properties.getProperty(PublicPayConstant.MSG_SRC));
       //????????????
       request.setMsgType(properties.getProperty(PublicPayConstant.MSG_TYPE));
       //???????????????????????????yyyy-MM-dd HH:mm:ss
		request.setRequestTimestamp(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
		//????????????????????? ??????????????????  ??????30??????
		String expireTime = extension.get("expireTime");
		if(!StringUtils.isBlank(expireTime)){
			request.setExpireTime(expireTime);
			logger.info("?????????????????????"+expireTime);
		}
		logger.info("?????????????????????"+expireTime);
		//???????????????
		request.setMerOrderId(req.getInstOrderNo());
		//?????????
		request.setMid(properties.getProperty(PublicPayConstant.MID));
		//?????????
		request.setTid(properties.getProperty(PublicPayConstant.TID));
		//????????????
		request.setInstMid(properties.getProperty(PublicPayConstant.INSTMID));
		String subject = extension.get("subject");
		request.setOrderDesc(StringUtils.isBlank(subject)?"":subject);
		logger.info("???????????????"+subject);
		//??????????????????????????? ????????????
		BigDecimal amount = req.getAmount();
		String amountFen = AmountUtils.Yuan2Fen(amount.toString());
		logger.info("???????????????????????????????????????:"+amountFen);
		System.out.println("???????????????????????????????????????:"+amountFen);
		request.setTotalAmount(amountFen);
		
		//notifyUrl  ????????????????????????
		request.setNotifyUrl(properties.getProperty(PublicPayConstant.PAY_NOTIFY_URL));
		//returnUrl ??????????????????
		String returnUrl = extension.get(PublicPayConstant.RETURN_URL);
		request.setReturnUrl(StringUtils.isBlank(returnUrl)?"":returnUrl);
		logger.info("?????????????????????????????????"+returnUrl);
		//????????????
		request.setSignType(properties.getProperty(PublicPayConstant.SIGN_TYPE));
		
		return request;
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
	 *  ??????????????????
	 * @param request
	 * @param properties
	 * @return
	 */
	private PayQueryRequest buidRequest(QueryRequest req,
			Properties properties) {
		
		PayQueryRequest request = new PayQueryRequest();
	   String date = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
       String rand = RandomStringUtils.randomNumeric(7);
       String msgId = date + rand;//??????ID???????????????
       request.setMsgId(msgId);
       //????????????
       request.setMsgSrc(properties.getProperty(PublicPayConstant.MSG_SRC));
       //????????????
       request.setMsgType("query");
       //???????????????????????????yyyy-MM-dd HH:mm:ss
		request.setRequestTimestamp(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
		//????????????????????????
		//request.setSrcReserve("");
		//???????????????
		request.setMerOrderId(req.getInstOrderNo());
		//?????????
		request.setMid(properties.getProperty(PublicPayConstant.MID));
		//?????????
		request.setTid(properties.getProperty(PublicPayConstant.TID));
		//????????????
		request.setInstMid(properties.getProperty(PublicPayConstant.INSTMID));
		//????????????
		request.setSignType(properties.getProperty(PublicPayConstant.SIGN_TYPE));
		
		return request;
	}

	private Map<String,String> getMapObject (PayQueryRequest req) {
    	Map<String,String> map = new HashMap<String,String>();
    	if(!StringUtils.isBlank(req.getMsgId())){
    		map.put("msgId", req.getMsgId());
    	}
    	map.put("msgSrc", req.getMsgSrc());
    	map.put("msgType", req.getMsgType());
    	if(!StringUtils.isBlank(req.getRequestTimestamp())){
    		map.put("requestTimestamp", req.getRequestTimestamp());
    	}
    	if(!StringUtils.isBlank(req.getSrcReserve())){
    		map.put("srcReserve", req.getSrcReserve());
    	}
    	map.put("mid", req.getMid());
    	map.put("tid", req.getTid());
    	if(!StringUtils.isBlank(req.getInstMid())){
    		map.put("instMid", req.getInstMid());
    	}
    	map.put("merOrderId", req.getMerOrderId());
    	map.put("signType", req.getSignType());
    	
    	return map;
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
    
    
    private Map<String,String> getMapObject (PayRefundRequest req) {
    	Map<String,String> map = new HashMap<String,String>();
    	if(!StringUtils.isBlank(req.getMsgId())){
    		map.put("msgId", req.getMsgId());
    	}
    	map.put("msgSrc", req.getMsgSrc());
    	map.put("msgType", req.getMsgType());
    	if(!StringUtils.isBlank(req.getRequestTimestamp())){
    		map.put("requestTimestamp", req.getRequestTimestamp());
    	}
    	map.put("merOrderId", req.getMerOrderId());
    	if(!StringUtils.isBlank(req.getSrcReserve())){
    		map.put("srcReserve", req.getSrcReserve());
    	}
    	map.put("mid", req.getMid());
    	map.put("tid", req.getTid());
    	if(!StringUtils.isBlank(req.getInstMid())){
    		map.put("instMid", req.getInstMid());
    	}
    	if(!StringUtils.isBlank(req.getRefundOrderId())){
    		map.put("refundOrderId", req.getRefundOrderId());
    	}
    	map.put("refundAmount", req.getRefundAmount());
    	if(!StringUtils.isBlank(req.getSign())){
    		map.put("sign", req.getSign());
    	}
    	
    	map.put("signType", req.getSignType());
    	
    	return map;
    }
    /**
     * ????????????????????????
     * 
     * @param refundNo  ?????????????????????
     * @param refundAmount  ????????????
     * @param instReturnOrderNo	?????????????????????
     * @param apiResultCode  api?????????
     * @param apiMsg   api???????????????
     * @return
     */
    	protected ChannelFundResult buildRefundResult(String refundNo, String refundAmount,
    		String instReturnOrderNo,String apiResultCode, String apiMsg) {
        return BankChannelRefundUtil.buildRefundResult(refundNo, refundAmount,instReturnOrderNo, apiResultCode, apiMsg);
    	}
    	/**
    	 * ????????????
    	 * @param req
    	 * @param properties
    	 * @return
    	 * @throws UnsupportedEncodingException
    	 */
    	private PayRefundRequest buidRefRequest(RefundRequest req,Properties properties) throws UnsupportedEncodingException {
    	PayRefundRequest refundReq = new PayRefundRequest();
		String date = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
        String rand = RandomStringUtils.randomNumeric(7);
        String msgId = date + rand;//??????ID???????????????
        refundReq.setMsgId(msgId);
        //????????????
        refundReq.setMsgSrc(properties.getProperty(PublicPayConstant.MSG_SRC));
        //????????????
        refundReq.setMsgType("refund");
        //???????????????????????????yyyy-MM-dd HH:mm:ss
        refundReq.setRequestTimestamp(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
 		//????????????????????????????????????
        refundReq.setMerOrderId(req.getOrignalInstOrderNo());
 		//?????????
        refundReq.setMid(properties.getProperty(PublicPayConstant.MID));
		//?????????
        refundReq.setTid(properties.getProperty(PublicPayConstant.TID));
		//????????????
        refundReq.setInstMid(properties.getProperty(PublicPayConstant.INSTMID));
		//????????????
		refundReq.setRefundAmount(AmountUtils.Yuan2Fen(req.getAmount().toString()));
		//refundOrderId
		refundReq.setRefundOrderId(req.getInstOrderNo());
		//????????????
		refundReq.setSignType(properties.getProperty(PublicPayConstant.SIGN_TYPE));
		return refundReq;
	}
    
    /**
     * ??????????????????????????????
     * 
     * @param refundRequest
     * @param resultMsg
     * @param resultCode
     * @param apiResultCode
     * @param apiResultMessage
     * @return
     */
    protected ChannelFundResult builFalidRefundResponse(RefundRequest refundRequest, String resultMsg,
                                                        String resultCode, String apiResultCode, String apiResultMessage) {
        return BankChannelRefundUtil.builFalidRefundResponse(refundRequest, resultMsg, resultCode, apiResultCode,
                                                             apiResultMessage);
    }

    
}
