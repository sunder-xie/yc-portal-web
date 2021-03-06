package com.ai.yc.protal.web.controller;

import com.ai.opt.sdk.dubbo.util.DubboConsumerFactory;
import com.ai.opt.sdk.util.DateUtil;
import com.ai.opt.sdk.util.StringUtil;
import com.ai.paas.ipaas.i18n.ResWebBundle;
import com.ai.slp.balance.api.deposit.interfaces.IDepositSV;
import com.ai.slp.balance.api.deposit.param.DepositParam;
import com.ai.slp.balance.api.deposit.param.TransSummary;
import com.ai.yc.protal.web.constants.Constants;
import com.ai.yc.protal.web.model.pay.PayNotify;
import com.ai.yc.protal.web.service.BalanceService;
import com.ai.yc.protal.web.service.OrderService;
import com.ai.yc.protal.web.utils.*;
import com.ai.yc.user.api.userservice.interfaces.IYCUserServiceSV;
import com.ai.yc.user.api.userservice.param.SearchYCUserRequest;
import com.ai.yc.user.api.userservice.param.YCUserInfoResponse;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by liutong on 16/11/10.
 */
@Controller
@RequestMapping("/pay")
public class PayController {
    private static final Logger LOG = LoggerFactory.getLogger(PayController.class);
    @Autowired
    OrderService orderService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private ResWebBundle rb;

    /**
     * 订单支付结果
     * @return
     */
    @RequestMapping("/payResultView")
    public String orderPayResultView(PayNotify payNotify, Model uiModel){
        //订单号
        uiModel.addAttribute("orderId",payNotify.getOrderId());
        //若哈希验证不通过,则表示支付结果有问题
        if (!verifyData(payNotify)){
            payNotify.setPayStates(PayNotify.PAY_STATES_FAIL);
        }
        //支付结果
        uiModel.addAttribute("payResult",PayNotify.PAY_STATES_SUCCESS.equals(payNotify.getPayStates()));
        return "order/orderPayResult";
    }

    /**
     * 订单支付结果
     * @param orderType
     * @param userId
     * @param totalPay
     * @param companyId
     * @param couponId
     * @param payNotify
     * @param discountSum 折扣
     * @param couponFee 优惠券或优惠码优惠金额
     * @return
     */
    @RequestMapping("/payResult/{orderType}/{userId}")
    @ResponseBody
    public String orderPayResult(
            @PathVariable("orderType")String orderType, @PathVariable("userId")String userId,
            Long totalPay,String currencyUnit,PayNotify payNotify,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String couponId,
            @RequestParam(required = false)Double discountSum,
            @RequestParam(required = false)Long couponFee){
        LOG.info("The pay result.orderType:{},\r\n{}", orderType,JSON.toJSONString(payNotify));
        //若优惠券不为空，则将优惠券设置为已使用
        if(!StringUtil.isBlank(couponId)) {
            balanceService.deductionCoupon(userId, couponId,
                    Long.parseLong(payNotify.getOrderId()), 201560l,
                    currencyUnit, rb.getDefaultLocale(),orderType);
        }
        //若哈希验证不通过或支付失败,则表示支付结果有问题
        if (!verifyData(payNotify)
                || !PayNotify.PAY_STATES_SUCCESS.equals(payNotify.getPayStates())){
            LOG.error("The pay is fail.");
            return "The pay verify fail";
        }
        //获取交易时间 20161111181026
        Timestamp notifyTime = DateUtil.getTimestamp(payNotify.getNotifyTime(),"yyyyMMddHHmmss");
        //支付费用
        Double orderAmount = Double.valueOf(payNotify.getOrderAmount())*1000;
        Long paidFee = orderAmount.longValue();
        Long discountFee = totalPay - paidFee;
        BigDecimal discountBig =
                discountSum==null?null:new BigDecimal(discountSum/10000).setScale(4, RoundingMode.HALF_UP);
        orderService.orderPayProcessResult(userId,null,Long.parseLong(payNotify.getOrderId()),orderType,
                totalPay,discountFee>0?discountFee:0,paidFee,payNotify.getPayOrgCode(),
                payNotify.getOutOrderId(),notifyTime,companyId,discountBig,couponFee);
        return "OK";
    }
    /**
     * 帐户充值结果 后台
     * @return
     */
    @RequestMapping("/depositFundResult/{userId}/{currencyUnit}")
    @ResponseBody
    public String accountDepositResult(@PathVariable("userId")String userId,@PathVariable("currencyUnit")String currencyUnit,
            PayNotify payNotify){
        LOG.info("The pay result.:{},\r\n{}",JSON.toJSONString(payNotify));
        //若哈希验证不通过或支付失败,则表示支付结果有问题
        if (!verifyData(payNotify)
                || !PayNotify.PAY_STATES_SUCCESS.equals(payNotify.getPayStates())){
            LOG.error("The pay is fail.");
            return "fail";
        }
        else{
        	LOG.info("verifyData OK");
        }
        LOG.info("计算totalFee开始。。。。");
        //支付费用
        Double totalFee = Double.valueOf(payNotify.getOrderAmount())*1000;
        LOG.info("计算totalFee结束 totalFee="+totalFee);
       //后场充值
        //
        LOG.info("开始获取IDepositSV dubbo服务");
        IDepositSV iDepositSV = DubboConsumerFactory.getService(IDepositSV.class);
        LOG.info("结束获取IDepositSV dubbo服务");
        DepositParam depositParam = new DepositParam();
        TransSummary summary = new TransSummary();
        summary.setAmount(new Double(totalFee).longValue());
        //资金科目ID,从公共域查,该充值模块为预存款,科目编码100000
        summary.setSubjectId(Long.parseLong(Constants.FUNDSUBJECT_ID));
        List<TransSummary> transSummaryList = new ArrayList<TransSummary>();
        transSummaryList.add(summary);
        depositParam.setTransSummary(transSummaryList);
        LOG.info("开始获取userServiceSV dubbo服务");
        IYCUserServiceSV userServiceSV = DubboConsumerFactory.getService(IYCUserServiceSV.class);
        LOG.info("结束获取userServiceSV dubbo服务");
        SearchYCUserRequest searchYCUserReq = new SearchYCUserRequest();
        searchYCUserReq.setTenantId(Constants.DEFAULT_TENANT_ID);
//        String userId = UserUtil.getUserId();
        searchYCUserReq.setUserId(userId);
        LOG.info("开始调用userServiceSV.searchYCUserInfo服务");
        YCUserInfoResponse userInfoResponse = userServiceSV.searchYCUserInfo(searchYCUserReq);
        LOG.info("结束调用userServiceSV.searchYCUserInfo服务");
        //若没有账户信息,直接返回null
        if (userInfoResponse==null||userInfoResponse.getAccountId()==null){
            LOG.error("没有该帐户信息.请创建帐户");
            return "FAIL123";
        }
        //用户账户
        long accountId = userInfoResponse.getAccountId();
        depositParam.setAccountId(accountId);
        //业务描述
        depositParam.setBusiDesc("充值");
        depositParam.setBusiSerialNo(payNotify.getOrderId());
        depositParam.setSystemId(Constants.SYSTEM_ID);
        depositParam.setTenantId(Constants.DEFAULT_TENANT_ID);
        depositParam.setCurrencyUnit(currencyUnit);
        /*支付方式
        ZFB: 	支付宝
        YL: 	   银联
        WEIXIN: 微信
        XY ：兴业
                */
        if (payNotify.getPayOrgCode()!=null){
            if (payNotify.getPayOrgCode().equals("ZFB")){
                depositParam.setPayStyle("支付宝");
            }else if (payNotify.getPayOrgCode().equals("YL")){
                depositParam.setPayStyle("银联");
            }else if (payNotify.getPayOrgCode().equals("XY")){
                depositParam.setPayStyle("兴业");
            }
        }
        //内部系统充值
        depositParam.setBusiOperCode("300000");
        LOG.info("开始调用iDepositSV.depositFund服务");
        try {
            String result = iDepositSV.depositFund(depositParam);

        }catch (Exception e){
            LOG.error("The deposit is fail.accountID="+accountId);
            return "FAIL456";
        }
        LOG.info("结束调用iDepositSV.depositFund服务");
       /* if (result==null){
            LOG.error("The deposit is fail.");
            return "faile1";
        }else {
            LOG.debug("The deposit is success.");
        }*/
        return "OK";
    }
    /**
     * 帐户充值结果
     * @return
     */
    @RequestMapping("/depositFundResultView")
    public String accountDepositResultView(PayNotify payNotify, Model uiModel){
        //订单号
        uiModel.addAttribute("orderId",payNotify.getOrderId());
        //若哈希验证不通过,则表示支付结果有问题
        if (!verifyData(payNotify)){
            payNotify.setPayStates(PayNotify.PAY_STATES_FAIL);
        }
        //支付结果
        //如果成功,跳转到支付成功页面
        if (PayNotify.PAY_STATES_SUCCESS.equals(payNotify.getPayStates())){
            return "balance/depositResultSuccess";
        } else {
            return "balance/depositResultFailed";
        }

    }

    /**
     * 余额支付结果
     * @param orderId
     * @param payResult
     * @param uiModel
     * @return
     */
    @RequestMapping("/yePayResultView")
    public String yePayResultView(String orderId,Boolean payResult,Model uiModel){
        //订单号
        uiModel.addAttribute("orderId",orderId);
        //支付结果
        uiModel.addAttribute("payResult",payResult);
        return "order/orderPayResult";
    }
    /**
     * 验证签名是否正常
     * @param payNotify
     * @return
     */
    private boolean verifyData(PayNotify payNotify){
        String infoStr = payNotify.getOutOrderId()+ VerifyUtil.SEPARATOR
                + payNotify.getOrderId() + VerifyUtil.SEPARATOR
                + payNotify.getOrderAmount() + VerifyUtil.SEPARATOR
                + payNotify.getPayStates()+ VerifyUtil.SEPARATOR
                + payNotify.getTenantId();
        String infoMd5 = VerifyUtil.encodeParam(infoStr, ConfigUtil.getProperty("REQUEST_KEY"));
        return payNotify.getInfoMd5().equals(infoMd5);
    }
}
