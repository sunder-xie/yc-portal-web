package com.ai.yc.protal.web.controller.balance;

import com.ai.opt.base.exception.BusinessException;
import com.ai.opt.base.exception.SystemException;
import com.ai.opt.base.vo.BaseListResponse;
import com.ai.opt.base.vo.BaseResponse;
import com.ai.opt.base.vo.PageInfo;
import com.ai.opt.sdk.dubbo.util.DubboConsumerFactory;
import com.ai.opt.sdk.util.CollectionUtil;
import com.ai.opt.sdk.util.UUIDUtil;
import com.ai.opt.sdk.web.model.ResponseData;
import com.ai.paas.ipaas.i18n.ResWebBundle;
import com.ai.slp.balance.api.incomeoutquery.interfaces.IncomeOutQuerySV;
import com.ai.slp.balance.api.incomeoutquery.param.FundBookQueryResponse;
import com.ai.slp.balance.api.incomeoutquery.param.IncomeDetail;
import com.ai.slp.balance.api.incomeoutquery.param.IncomeQueryRequest;
import com.ai.slp.balance.api.sendcoupon.interfaces.ISendCouponSV;
import com.ai.slp.balance.api.sendcoupon.param.DeductionCouponRequest;
import com.ai.slp.balance.api.sendcoupon.param.DeductionCouponResponse;
import com.ai.slp.balance.api.sendcoupon.param.FreezeCouponRequest;
import com.ai.yc.protal.web.constants.BalanceConstants;
import com.ai.yc.protal.web.constants.Constants;
import com.ai.yc.protal.web.model.pay.AccountBalanceInfo;
import com.ai.yc.protal.web.service.BalanceService;
import com.ai.yc.protal.web.utils.*;
import com.ai.yc.user.api.usercompany.interfaces.IYCUserCompanySV;
import com.ai.yc.user.api.usercompany.param.UserCompanyInfoRequest;
import com.ai.yc.user.api.usercompany.param.UserCompanyInfoResponse;
import com.ai.yc.user.api.usercompany.param.UsrCompanyInfo;
import com.ai.yc.user.api.usercompanyrelation.interfaces.IYCUserCompanyRelationSV;
import com.ai.yc.user.api.usercompanyrelation.param.ManagerResponse;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 我的帐户
 * Created by lixiaokui on 16/11/14.
 */
@Controller
@RequestMapping("/p/companybalance")
public class CompanyAccountController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyAccountController.class);
    @Autowired
    BalanceService balanceService;
    @Autowired
    ResWebBundle rb;

    /**
     * 显示我的帐户页面
     * @return
     */
    @RequestMapping("/companyAccount")
    public String toCompanyAccount(Model uiModel){
        //帐户余额
        AccountBalanceInfo balanceInfo =  balanceService.queryOfUser(UserUtil.getUserId());
        uiModel.addAttribute("currencyUnit",balanceInfo.getCurrencyUnit());
        uiModel.addAttribute("balance",String.valueOf(AmountUtil.changeLiToYuan(balanceInfo.getBalance())));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date beginDate = new Date();
        beginDate.setMonth(beginDate.getMonth()-1);
        String date1 = formatter.format(beginDate);
        Date endDate = new Date();
        endDate.setMonth(endDate.getMonth());
        String date2 = formatter.format(endDate);

        uiModel.addAttribute("beginTime",date1);
        uiModel.addAttribute("endTime",date2);
        return "companyAccount/companyBalanceAccount";
    }

    @RequestMapping("/accountList")
    @ResponseBody
    public ResponseData<PageInfo<IncomeDetail>> accountList(IncomeQueryRequest incomeQueryRequest,
                                                            @RequestParam(value = "pageNo")int pageNO,
                                                            @RequestParam(value = "pageSize")int pageSize){
        ResponseData<PageInfo<IncomeDetail>> resData =
                new ResponseData<PageInfo<IncomeDetail>>(ResponseData.AJAX_STATUS_SUCCESS,"OK");
        //通过用户id查询企业id
        IYCUserCompanyRelationSV iycUserCompanyRelationSV = DubboConsumerFactory
                .getService(IYCUserCompanyRelationSV.class);
        ManagerResponse userIsManager = iycUserCompanyRelationSV.getUserIsManager(UserUtil.getUserId());
        //如果返回值为空,或返回信息中包含错误信息,则抛出异常
        if (userIsManager==null||(userIsManager.getResponseHeader()!=null && (!userIsManager.getResponseHeader().isSuccess()))||
                ("0001".equals(userIsManager.getResponseHeader().getResultCode()))){
            LOGGER.info("该用户不是企业管理员=====用户id:"+UserUtil.getUserId());
            resData = new ResponseData<>(ResponseData.AJAX_STATUS_FAILURE,"FAIL");
        }
        //如果用户是管理员,通过企业id查询帐户id
        UserCompanyInfoResponse userCompanyInfoResponse =null;
        if (userIsManager.getIsManagement()==1){
            IYCUserCompanySV iycUserCompanySV = DubboConsumerFactory.getService(IYCUserCompanySV.class);
            UserCompanyInfoRequest usrCompanyInfoRequest = new UserCompanyInfoRequest();
            usrCompanyInfoRequest.setCompanyId(userIsManager.getCompanyId());
            userCompanyInfoResponse = iycUserCompanySV.queryCompanyInfo(usrCompanyInfoRequest);
        }
        if (null==userCompanyInfoResponse||(userCompanyInfoResponse.getResponseHeader()!=null&&
                (!userCompanyInfoResponse.getResponseHeader().getIsSuccess()))){
            LOGGER.info("企业帐户查询失败++++++++++++企业id:"+userCompanyInfoResponse.getCompanyId());
            resData = new ResponseData<>(ResponseData.AJAX_STATUS_FAILURE,"FAIL");
        }
        IncomeOutQuerySV incomeOutQuerySV =  DubboConsumerFactory.getService(IncomeOutQuerySV.class);
        incomeQueryRequest.setAccountId(userCompanyInfoResponse.getAccountId());
        incomeQueryRequest.setTenantId(Constants.DEFAULT_TENANT_ID);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            if(StringUtils.isBlank(incomeQueryRequest.getBeginDate())){
                Date beginDate = new Date();
                beginDate.setMonth(beginDate.getMonth()-1);
                String date1 = formatter.format(beginDate);
                incomeQueryRequest.setBeginDate(date1+" 00:00:00");
            } else {
                incomeQueryRequest.setBeginDate(incomeQueryRequest.getBeginDate().toString()+" 00:00:00");
            }
            if(StringUtils.isBlank(incomeQueryRequest.getEndDate())){
                Date endDate = new Date();
                endDate.setMonth(endDate.getMonth());
                String date1 = formatter.format(endDate);
                incomeQueryRequest.setEndDate(date1+" 23:59:59");
            }else {
                incomeQueryRequest.setEndDate(incomeQueryRequest.getEndDate().toString()+" 23:59:59");
            }
            FundBookQueryResponse fundBookQueryResponse = new FundBookQueryResponse();
            PageInfo<IncomeDetail> pageInfo = new  PageInfo<IncomeDetail>();
            pageInfo.setPageNo(pageNO);
            pageInfo.setPageSize(pageSize);
            incomeQueryRequest.setPageInfo(pageInfo);
            fundBookQueryResponse = incomeOutQuerySV.incomeOutQuery(incomeQueryRequest);
            //总收支计算
            IncomeQueryRequest incomeQueryByDate = new IncomeQueryRequest();
            incomeQueryByDate.setTenantId(incomeQueryRequest.getTenantId());
            incomeQueryByDate.setAccountId(incomeQueryRequest.getAccountId());
            incomeQueryByDate.setBeginDate(incomeQueryRequest.getBeginDate());
            incomeQueryByDate.setEndDate(incomeQueryRequest.getEndDate());
            FundBookQueryResponse fundBookQueryResponseByDate = new FundBookQueryResponse();
            incomeQueryRequest.setPageInfo(pageInfo);
            fundBookQueryResponseByDate = incomeOutQuerySV.incomeOutQuery(incomeQueryByDate);
            Long incomeAmount = 0l;
            long outAmount = 0l;
            if (fundBookQueryResponse.getPageInfo()!=null){
                List<IncomeDetail> incomeDetails = fundBookQueryResponseByDate.getPageInfo().getResult();
                for (IncomeDetail incomeDetail:incomeDetails){
                    if (incomeDetail.getIncomeFlag().equals("1")){
                        incomeAmount = incomeAmount + incomeDetail.getTotalAmount();
                    }
                    if (incomeDetail.getIncomeFlag().equals("0")){
                        outAmount = outAmount + incomeDetail.getTotalAmount();
                    }
                }
                for (int i = 0;i<fundBookQueryResponse.getPageInfo().getResult().size();i++){
                    fundBookQueryResponse.getPageInfo().getResult().get(i).setIncomeBalance(incomeAmount);
                    fundBookQueryResponse.getPageInfo().getResult().get(i).setOutBalance(Math.abs(outAmount));
                }

            }else {
                IncomeDetail incomeDetail = new IncomeDetail();
                incomeDetail.setIncomeBalance(incomeAmount);
                incomeDetail.setOutBalance(outAmount);
                List<IncomeDetail> list = new ArrayList<>();
                list.add(incomeDetail);
                pageInfo.setResult(list);
                fundBookQueryResponse.setPageInfo(pageInfo);
            }
            resData.setData(fundBookQueryResponse.getPageInfo());
        } catch (Exception e) {
            LOGGER.error("查询收支分页失败:",e);
            resData = new ResponseData<PageInfo<IncomeDetail>>(ResponseData.AJAX_STATUS_FAILURE, "查询收支失败");
        }
        return resData;
    }

    //充值页面
    @RequestMapping("/depositCompanyFund")
    public String toDepositFund(){
        return "companyAccount/depositCompanyFund";
    }

    /**
     * 跳转第三方支付页面,需要登录后才能进行支付
     * @param
     * @param orderAmount
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/gotoPay")
    public String gotoPay(
            double orderAmount,String currencyUnit,String merchantUrl,String payOrgCode,
            HttpServletResponse response,Model uiModel)
            throws Exception {
        //租户
        String tenantId= ConfigUtil.getProperty("TENANT_ID");
        //服务异步通知地址
        String notifyUrl= ConfigUtil.getProperty("NOTIFY_DEPOSIT_URL")+"/"+ UserUtil.getUserId()+"/"+currencyUnit;
        //商户交易单号(要求商户每次提交的支付请求交易单号不能重复，用于唯一标识每个提交给支付中心的支付请求)
        String orderId = UUIDUtil.genId32();
        //异步通知地址,默认为用户
        String amount = String.valueOf(orderAmount);
        Map<String, String> map = new HashMap<String, String>();
        map.put("tenantId", tenantId);//租户ID
        map.put("orderId", orderId);//请求单号
        map.put("subject","账户充值");
        map.put("returnUrl", ConfigUtil.getProperty("RETURN_DEPOSIT_URL"));//页面跳转地址
        map.put("notifyUrl", notifyUrl);//服务异步通知地址
        map.put("merchantUrl",merchantUrl);//用户付款中途退出返回商户的地址
        map.put("requestSource", Constants.SELF_SOURCE);//终端来源
        map.put("currencyUnit",currencyUnit);//币种
        map.put("orderAmount", amount);//金额
        map.put("subject", "账号充值");//订单名称
        map.put("payOrgCode",payOrgCode);
        // 加密
        String infoStr = orderId+ VerifyUtil.SEPARATOR
                + amount + VerifyUtil.SEPARATOR
                + notifyUrl + VerifyUtil.SEPARATOR
                + tenantId;
        String infoMd5 = VerifyUtil.encodeParam(infoStr, ConfigUtil.getProperty("REQUEST_KEY"));
        map.put("infoMd5", infoMd5);
        LOGGER.info("开始前台通知:" + map);
        String htmlStr = PaymentUtil.generateAutoSubmitForm(ConfigUtil.getProperty("ACTION_URL"), map);
        LOGGER.info("发起支付申请:" + htmlStr);
        uiModel.addAttribute("paramsMap",map);
        uiModel.addAttribute("actionUrl",ConfigUtil.getProperty("ACTION_URL"));
//        response.setCharacterEncoding("UTF-8");
//        response.setStatus(HttpServletResponse.SC_OK);
//        response.getWriter().write(htmlStr);
//        response.getWriter().flush();
        return "gotoPay";
    }

}
