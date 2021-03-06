package com.channel.bank.adapter.pay.service.impl;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.channel.bank.adapter.pay.config.SignType;
import com.channel.bank.adapter.pay.config.WxPayH5Config;
import com.channel.bank.adapter.pay.constants.WxPayConstants;
import com.channel.bank.adapter.pay.enums.BestPayTypeEnum;
import com.channel.bank.adapter.pay.mode.OrderQueryRequest;
import com.channel.bank.adapter.pay.mode.OrderQueryResponse;
import com.channel.bank.adapter.pay.mode.OrderRefundQueryRequest;
import com.channel.bank.adapter.pay.mode.OrderRefundQueryResponse;
import com.channel.bank.adapter.pay.mode.PayFundRequest;
import com.channel.bank.adapter.pay.mode.PayFundResponse;
import com.channel.bank.adapter.pay.mode.RePayRequest;
import com.channel.bank.adapter.pay.mode.RePayResponse;
import com.channel.bank.adapter.pay.mode.wxpay.WxPayApi;
import com.channel.bank.adapter.pay.mode.wxpay.request.WxOrderQueryRequest;
import com.channel.bank.adapter.pay.mode.wxpay.request.WxPayRefundRequest;
import com.channel.bank.adapter.pay.mode.wxpay.request.WxPayUnifiedorderRequest;
import com.channel.bank.adapter.pay.mode.wxpay.request.WxRefundqueryRequest;
import com.channel.bank.adapter.pay.mode.wxpay.response.WxOrderQueryResponse;
import com.channel.bank.adapter.pay.mode.wxpay.response.WxPayAsyncResponse;
import com.channel.bank.adapter.pay.mode.wxpay.response.WxPaySyncResponse;
import com.channel.bank.adapter.pay.mode.wxpay.response.WxRefundResponse;
import com.channel.bank.adapter.pay.mode.wxpay.response.WxRefundqueryResponse;
import com.channel.bank.adapter.pay.service.BestPayService;
import com.channel.bank.adapter.pay.utils.MapUtils;
import com.channel.bank.adapter.pay.utils.MoneyUtil;
import com.channel.bank.adapter.pay.utils.PayRandomUtil;
import com.channel.bank.adapter.pay.utils.XmlUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WxPayServiceImpl implements BestPayService {
	private static final Logger logger = LoggerFactory.getLogger(WxPayServiceImpl.class);
    private WxPayH5Config wxPayH5Config;

    public void setWxPayH5Config(WxPayH5Config wxPayH5Config) {
        this.wxPayH5Config = wxPayH5Config;
    }

    @Override
    public PayFundResponse pay(PayFundRequest request) {
        WxPayUnifiedorderRequest wxRequest = new WxPayUnifiedorderRequest();
        wxRequest.setOutTradeNo(request.getOrderId());
        wxRequest.setTotalFee(MoneyUtil.Yuan2Fen(request.getOrderAmount()));
        wxRequest.setBody(request.getOrderName());
        wxRequest.setOpenid(request.getOpenid());

        wxRequest.setTradeType(switchH5TradeType(request.getPayTypeEnum()));
        wxRequest.setAppid(wxPayH5Config.getAppId());
        wxRequest.setMchId(wxPayH5Config.getMchId());
        wxRequest.setNotifyUrl(wxPayH5Config.getNotifyUrl());
        wxRequest.setNonceStr(PayRandomUtil.getRandomStr());
        wxRequest.setSpbillCreateIp(request.getSpbillCreateIp() == null || request.getSpbillCreateIp().isEmpty() ? "8.8.8.8" : request.getSpbillCreateIp());
        wxRequest.setSign(WxPaySignature.sign(MapUtils.buildMap(wxRequest), wxPayH5Config.getMchKey()));

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WxPayConstants.WXPAY_GATEWAY)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .addInterceptor((new HttpLoggingInterceptor()
                                .setLevel(HttpLoggingInterceptor.Level.BODY)))
                        .build()
                )
                .build();
        RequestBody body = RequestBody.create(MediaType.parse("application/xml; charset=utf-8"), XmlUtil.toString(wxRequest));
        Call<WxPaySyncResponse> call = retrofit.create(WxPayApi.class).unifiedorder(body);
        Response<WxPaySyncResponse> retrofitResponse  = null;
        try{
            retrofitResponse = call.execute();
        }catch (IOException e) {
            e.printStackTrace();
        }
        if (!retrofitResponse.isSuccessful()) {
            throw new RuntimeException("????????????????????????????????????, ????????????");
        }
        WxPaySyncResponse response = retrofitResponse.body();

        if(!response.getReturnCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("????????????????????????????????????, returnCode != SUCCESS, returnMsg = " + response.getReturnMsg());
        }
        if (!response.getResultCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("????????????????????????????????????, resultCode != SUCCESS, err_code = " + response.getErrCode() + " err_code_des=" + response.getErrCodeDes());
        }

        return buildPayResponse(response);
    }

    @Override
    public boolean verify(Map map, SignType signType, String sign) {
        return WxPaySignature.verify(map, wxPayH5Config.getMchKey());
    }

    @Override
    public PayFundResponse syncNotify(HttpServletRequest request) {
        return null;
    }

    /**
     * ????????????
     * @param notifyData
     * @return
     */
    @Override
    public PayFundResponse asyncNotify(String notifyData) {
        //????????????
        if (!WxPaySignature.verify(XmlUtil.toMap(notifyData), wxPayH5Config.getMchKey())) {
        	logger.error("????????????????????????????????????????????????, response={}", notifyData);
            throw new RuntimeException("????????????????????????????????????????????????");
        }

        //xml???????????????
        WxPayAsyncResponse asyncResponse = (WxPayAsyncResponse) XmlUtil.toObject(notifyData, WxPayAsyncResponse.class);

        if(!asyncResponse.getReturnCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("??????????????????????????????????????????, returnCode != SUCCESS, returnMsg = " + asyncResponse.getReturnMsg());
        }
        //??????????????????????????????
        if (!asyncResponse.getResultCode().equals(WxPayConstants.SUCCESS)
                && asyncResponse.getErrCode().equals("ORDERPAID")) {
            return buildPayResponse(asyncResponse);
        }

        if (!asyncResponse.getResultCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("??????????????????????????????????????????, resultCode != SUCCESS, err_code = " + asyncResponse.getErrCode() + " err_code_des=" + asyncResponse.getErrCodeDes());
        }

        return buildPayResponse(asyncResponse);
    }

    /**
     * ????????????
     * @param request
     * @return
     */
    public RePayResponse refund(RePayRequest request) {
        WxPayRefundRequest wxRequest = new WxPayRefundRequest();
        wxRequest.setOutTradeNo(request.getOrderId());
        wxRequest.setOutRefundNo(request.getOrderId());
        wxRequest.setTotalFee(MoneyUtil.Yuan2Fen(request.getOrderAmount()));
        wxRequest.setRefundFee(MoneyUtil.Yuan2Fen(request.getOrderAmount()));

        wxRequest.setAppid(wxPayH5Config.getAppId());
        wxRequest.setMchId(wxPayH5Config.getMchId());
        wxRequest.setNonceStr(PayRandomUtil.getRandomStr());
        wxRequest.setSign(WxPaySignature.sign(MapUtils.buildMap(wxRequest), wxPayH5Config.getMchKey()));

        //???????????????
        if (wxPayH5Config.getSslContext() == null) {
            wxPayH5Config.initSSLContext();
        }
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder()
                .sslSocketFactory(wxPayH5Config.getSslContext().getSocketFactory())
                .addInterceptor((new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY)))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WxPayConstants.WXPAY_GATEWAY)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(okHttpClient)
                .build();
        String xml = XmlUtil.toString(wxRequest);
        RequestBody body = RequestBody.create(MediaType.parse("application/xml; charset=utf-8"),xml);
        Call<WxRefundResponse> call = retrofit.create(WxPayApi.class).refund(body);
        Response<WxRefundResponse> retrofitResponse  = null;
        try{
            retrofitResponse = call.execute();
        }catch (IOException e) {
            e.printStackTrace();
        }
        if (!retrofitResponse.isSuccessful()) {
            throw new RuntimeException("??????????????????????????????, ????????????");
        }
        WxRefundResponse response = retrofitResponse.body();

        if(!response.getReturnCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("??????????????????????????????, returnCode != SUCCESS, returnMsg = " + response.getReturnMsg());
        }
        if (!response.getResultCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("??????????????????????????????, resultCode != SUCCESS, err_code = " + response.getErrCode() + " err_code_des=" + response.getErrCodeDes());
        }

        return buildRefundResponse(response);
    }

    private RePayResponse buildRefundResponse(WxRefundResponse response) {
        RePayResponse refundResponse = new RePayResponse();
        refundResponse.setResultCode(response.getResultCode());
        refundResponse.setReturnCode(response.getReturnCode());
        refundResponse.setReturnMsg(response.getReturnMsg());
        refundResponse.setErrCode(response.getErrCode());
        refundResponse.setErrCodeDes(response.getErrCodeDes());
        refundResponse.setOrderId(response.getOutTradeNo());
        refundResponse.setOrderAmount(MoneyUtil.Fen2Yuan(response.getTotalFee()));
        refundResponse.setOutTradeNo(response.getTransactionId());
        refundResponse.setRefundId(response.getOutRefundNo());
        refundResponse.setOutRefundNo(response.getRefundId());
        return refundResponse;
    }

    private PayFundResponse buildPayResponse(WxPayAsyncResponse response) {
        PayFundResponse payResponse = new PayFundResponse();
        payResponse.setResultCode(response.getResultCode());
        payResponse.setReturnMsg(response.getReturnMsg());
        payResponse.setReturnCode(response.getReturnCode());
        payResponse.setErrCode(response.getErrCode());
        payResponse.setErrCodeDes(response.getErrCodeDes());
        payResponse.setOrderAmount(MoneyUtil.Fen2Yuan(response.getTotalFee()));
        payResponse.setOrderId(response.getOutTradeNo());
        payResponse.setOutTradeNo(response.getTransactionId());
        payResponse.setMwebUrl(response.getMwebUrl());
        return payResponse;
    }

    /**
     * ?????????h5?????????
     * @param response
     * @return
     */
    private PayFundResponse buildPayResponse(WxPaySyncResponse response) {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = PayRandomUtil.getRandomStr();
        String packAge = "prepay_id=" + response.getPrepayId();
        String signType = "MD5";

        //?????????????????????map
        Map<String, String> map = new HashMap<>();
        map.put("appId", response.getAppid());
        map.put("timeStamp", timeStamp);
        map.put("nonceStr", nonceStr);
        map.put("package", packAge);
        map.put("signType", signType);

        PayFundResponse payResponse = new PayFundResponse();
        payResponse.setReturnCode(response.getReturnCode());
        payResponse.setResultCode(response.getResultCode());
        payResponse.setReturnMsg(response.getReturnMsg());
        payResponse.setErrCode(response.getErrCode());
        payResponse.setErrCodeDes(response.getErrCodeDes());
        payResponse.setAppId(response.getAppid());
        payResponse.setTimeStamp(timeStamp);
        payResponse.setNonceStr(nonceStr);
        payResponse.setPackAge(packAge);
        payResponse.setSignType(signType);
        payResponse.setPaySign(WxPaySignature.sign(map, wxPayH5Config.getMchKey()));
        payResponse.setMwebUrl(response.getMwebUrl());
        payResponse.setPrepayId(response.getPrepayId());
        payResponse.setCodeUrl(response.getCodeUrl());
        payResponse.setTradeType(response.getTradeType());

        return payResponse;
    }


    /**
     * H5????????????????????????
     */
    public String switchH5TradeType(BestPayTypeEnum payTypeEnum){
        String tradeType = "JSAPI";
        switch (payTypeEnum){
            case WXPAY_H5:
                tradeType = "JSAPI";
                break;
            case WXPAY_MWEB:
                tradeType = "MWEB";
                break;
            case WXPAY_NATIVE:
                tradeType = "NATIVE";
                break;
        }
        return tradeType;
    }

	@Override
	public OrderQueryResponse query(OrderQueryRequest request) {
		WxOrderQueryRequest queryRequest = new WxOrderQueryRequest();
		queryRequest.setAppid(wxPayH5Config.getAppId());
		queryRequest.setMchId(wxPayH5Config.getMchId());
		queryRequest.setOutTradeNo(request.getOrderId());
		queryRequest.setNonceStr(PayRandomUtil.getRandomStr());
		queryRequest.setSign(WxPaySignature.sign(MapUtils.buildMap(queryRequest), wxPayH5Config.getMchKey()));
		
		Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WxPayConstants.WXPAY_GATEWAY)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .addInterceptor((new HttpLoggingInterceptor()
                                .setLevel(HttpLoggingInterceptor.Level.BODY)))
                        .build()
                )
                .build();
        RequestBody body = RequestBody.create(MediaType.parse("application/xml; charset=utf-8"), XmlUtil.toString(queryRequest));
        Call<WxOrderQueryResponse> call = retrofit.create(WxPayApi.class).orderquery(body);
        Response<WxOrderQueryResponse> retrofitResponse  = null;
        try{
            retrofitResponse = call.execute();
        }catch (IOException e) {
            e.printStackTrace();
        }
        if (!retrofitResponse.isSuccessful()) {
            throw new RuntimeException("????????????????????????????????????, ????????????");
        }
        WxOrderQueryResponse response = retrofitResponse.body();

        if(!response.getReturnCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("????????????????????????????????????, returnCode != SUCCESS, returnMsg = " + response.getReturnMsg());
        }
        if (!response.getResultCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("????????????????????????????????????, resultCode != SUCCESS, err_code = " + response.getErrCode() + " err_code_des=" + response.getErrCodeDes());
        }
		
		return buildOrderQueryResponse(response);
	}

	private OrderQueryResponse buildOrderQueryResponse(WxOrderQueryResponse response) {
		OrderQueryResponse queryResponse = new OrderQueryResponse();
		queryResponse.setReturnCode(response.getReturnCode());
		queryResponse.setReturnMsg(response.getReturnMsg());
		queryResponse.setResultCode(response.getResultCode());
		queryResponse.setErrCode(response.getErrCode());
		queryResponse.setErrCodeDes(response.getErrCodeDes());
		queryResponse.setAppId(response.getAppid());
		queryResponse.setTradeType(response.getTradeType());
		queryResponse.setTradeState(response.getTradeState());
		queryResponse.setTransactionId(response.getTransactionId());
		queryResponse.setOutTradeNo(response.getOutTradeNo());
		queryResponse.setTradeStateDesc(response.getTradeStateDesc());
		return queryResponse;
	}

	@Override
	public OrderRefundQueryResponse refundQuery(OrderRefundQueryRequest request) {
		WxRefundqueryRequest queryRequest = new WxRefundqueryRequest();
		queryRequest.setAppid(wxPayH5Config.getAppId());
		queryRequest.setMchId(wxPayH5Config.getMchId());
		queryRequest.setOutTradeNo(request.getOrderId());
		queryRequest.setNonceStr(PayRandomUtil.getRandomStr());
		queryRequest.setSign(WxPaySignature.sign(MapUtils.buildMap(queryRequest), wxPayH5Config.getMchKey()));
		
		Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WxPayConstants.WXPAY_GATEWAY)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .addInterceptor((new HttpLoggingInterceptor()
                                .setLevel(HttpLoggingInterceptor.Level.BODY)))
                        .build()
                )
                .build();
        RequestBody body = RequestBody.create(MediaType.parse("application/xml; charset=utf-8"), XmlUtil.toString(queryRequest));
        Call<WxRefundqueryResponse> call = retrofit.create(WxPayApi.class).refundquery(body);
        Response<WxRefundqueryResponse> retrofitResponse  = null;
        try{
            retrofitResponse = call.execute();
        }catch (IOException e) {
            e.printStackTrace();
        }
        if (!retrofitResponse.isSuccessful()) {
            throw new RuntimeException("????????????????????????????????????, ????????????");
        }
        WxRefundqueryResponse response = retrofitResponse.body();

        if(!response.getReturnCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("????????????????????????????????????, returnCode != SUCCESS, returnMsg = " + response.getReturnMsg());
        }
        if (!response.getResultCode().equals(WxPayConstants.SUCCESS)) {
            throw new RuntimeException("????????????????????????????????????, resultCode != SUCCESS, err_code = " + response.getErrCode() + " err_code_des=" + response.getErrCodeDes());
        }
		
		return buildOrderRefundQueryResponse(response);
	}

	private OrderRefundQueryResponse buildOrderRefundQueryResponse(WxRefundqueryResponse response) {
		OrderRefundQueryResponse queryResponse = new OrderRefundQueryResponse();
		queryResponse.setReturnCode(response.getReturnCode());
		queryResponse.setReturnMsg(response.getReturnMsg());
		queryResponse.setResultCode(response.getResultCode());
		queryResponse.setErrCode(response.getErrCode());
		queryResponse.setErrCodeDes(response.getErrCodeDes());
		queryResponse.setAppId(response.getAppid());
		queryResponse.setMchId(response.getMchId());
		queryResponse.setTransactionId(response.getTransactionId());
		queryResponse.setOutTradeNo(response.getOutTradeNo());
		queryResponse.setRefundStatus$n(response.getRefundStatus$n());
		queryResponse.setRefundRecvAccout$n(response.getRefundAccount$n());
		queryResponse.setRefundSuccessTime$n(response.getRefundSuccessTime$n());
		return queryResponse;
	}
}
