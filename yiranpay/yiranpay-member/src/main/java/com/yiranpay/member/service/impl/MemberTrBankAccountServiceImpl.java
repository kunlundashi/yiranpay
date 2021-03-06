package com.yiranpay.member.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yiranpay.member.mapper.MemberTmMemberMapper;
import com.yiranpay.member.mapper.MemberTrBankAccountMapper;
import com.yiranpay.member.request.BankAccInfoRequest;
import com.yiranpay.member.request.BankAccountRequest;
import com.yiranpay.member.response.BankAccInfoResponse;
import com.yiranpay.member.response.BankAccountInfoResponse;
import com.yiranpay.member.response.BankAccountResponse;
import com.yiranpay.common.core.text.Convert;
import com.yiranpay.member.constant.MaConstant;
import com.yiranpay.member.domain.MemberTrBankAccount;
import com.yiranpay.member.domain.MemberTrVerifyEntity;
import com.yiranpay.member.domain.MemberTrVerifyRef;
import com.yiranpay.member.domain.Verify;
import com.yiranpay.member.domain.VerifyQuery;
import com.yiranpay.member.enums.BankAccountStatusEnum;
import com.yiranpay.member.enums.MethodType;
import com.yiranpay.member.enums.ResponseCode;
import com.yiranpay.member.enums.VerifyTypeEncryptMappingEnum;
import com.yiranpay.member.enums.YesNoEnum;
import com.yiranpay.member.exception.MaBizException;
import com.yiranpay.member.filter.BankAccountFilter;
import com.yiranpay.member.service.IMemberTrBankAccountService;
import com.yiranpay.member.service.IMemberTrVerifyEntityService;
import com.yiranpay.member.service.IMemberTrVerifyRefService;
import com.yiranpay.member.utils.BankAcctDomainUtil;
import com.yiranpay.member.utils.MaPatternUtil;
import com.yiranpay.member.utils.ResponseUtil;
import com.yiranpay.member.validator.BankAccountFacadeValidator;
import com.yiranpay.member.validator.BankAccountValidator;
import com.yiranpay.member.validator.MemberValidator;
import com.yiranpay.payorder.enums.EncryptType;
import com.yiranpay.payorder.service.IUesServiceClient;

/**
 * ??????????????? ???????????????
 * 
 * @author yiran
 * @date 2019-03-30
 */
@Service
public class MemberTrBankAccountServiceImpl implements IMemberTrBankAccountService 
{
	private Logger             logger = LoggerFactory.getLogger(MemberTrBankAccountServiceImpl.class);
	@Autowired
	private MemberTrBankAccountMapper memberTrBankAccountMapper;
	@Autowired
	private IMemberTrVerifyRefService memberTrVerifyRefService;
	@Autowired
	private IMemberTrVerifyEntityService memberTrVerifyEntityService;
	@Autowired
	private MemberValidator memberValidator;
	@Autowired
	private IUesServiceClient uesServiceClient;
	@Autowired
	private MemberTmMemberMapper memberTmMemberMapper;
	@Autowired
	private BankAccountFilter bankAccountFilter;
	@Autowired
	private BankAccountValidator bankAccountValidator;
	/**
     * ???????????????????????????
     * 
     * @param id ???????????????ID
     * @return ?????????????????????
     */
    @Override
	public MemberTrBankAccount selectMemberTrBankAccountById(Integer id)
	{
	    return memberTrBankAccountMapper.selectMemberTrBankAccountById(id);
	}
	
	/**
     * ???????????????????????????
     * 
     * @param memberTrBankAccount ?????????????????????
     * @return ?????????????????????
     */
	@Override
	public List<MemberTrBankAccount> selectMemberTrBankAccountList(MemberTrBankAccount memberTrBankAccount)
	{
	    return memberTrBankAccountMapper.selectMemberTrBankAccountList(memberTrBankAccount);
	}
	
    /**
     * ?????????????????????
     * 
     * @param memberTrBankAccount ?????????????????????
     * @return ??????
     */
	@Override
	public int insertMemberTrBankAccount(MemberTrBankAccount memberTrBankAccount)
	{
	    return memberTrBankAccountMapper.insertMemberTrBankAccount(memberTrBankAccount);
	}
	
	/**
     * ?????????????????????
     * 
     * @param memberTrBankAccount ?????????????????????
     * @return ??????
     */
	@Override
	public int updateMemberTrBankAccount(MemberTrBankAccount memberTrBankAccount)
	{
	    return memberTrBankAccountMapper.updateMemberTrBankAccount(memberTrBankAccount);
	}

	/**
     * ???????????????????????????
     * 
     * @param ids ?????????????????????ID
     * @return ??????
     */
	@Override
	public int deleteMemberTrBankAccountByIds(String ids)
	{
		return memberTrBankAccountMapper.deleteMemberTrBankAccountByIds(Convert.toStrArray(ids));
	}

	@Override
	public BankAccountResponse queryBankAccount(BankAccountRequest request) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]???????????????????????????????????????:request={}", request);
        }
        BankAccountResponse response = new BankAccountResponse();
        try {
            BankAccountFacadeValidator.validator(request);
            List<MemberTrBankAccount> bankAccounts = queryBankAccounts(BankAcctDomainUtil
                    .convertBankAccount(request));
            if (CollectionUtils.isEmpty(bankAccounts)) {
                response.setBankAccountInfos(null);
                response.setResponseCode(ResponseCode.NO_QUERY_RESULT.getCode());
                response.setResponseMessage(ResponseCode.NO_QUERY_RESULT.getMessage());
            } else {
                response.setBankAccountInfos(bankAccounts);
                ResponseUtil.setSuccessResponse(response);
            }

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]???????????????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "?????????????????????????????????");
        }

        return response;
	}

	private List<MemberTrBankAccount> queryBankAccounts(MemberTrBankAccount bankAccount) throws MaBizException {
		 //????????????????????? member + status=null ????????????
        List<MemberTrBankAccount> banks = memberTrBankAccountMapper.selectMemberTrBankAccountByMemberId(bankAccount.getMemberId());
        if (CollectionUtils.isEmpty(banks)) {
            return null;
        }

        fillMobileNum(banks, bankAccount.getMemberId());
        if (BankAcctDomainUtil.isQueryMemberBank(bankAccount)) {
            return banks;
        }
        //??????
        return BankAcctDomainUtil.filterBankAccount(banks, bankAccount);
	}
	

	/**
     * ???????????????
     * @param banks
     * @param memberId
     * @throws MaBizException
     */
    private void fillMobileNum(List<MemberTrBankAccount> banks, String memberId) throws MaBizException {
    	 //???????????????
        MemberTrVerifyRef memberTrVerifyRef = memberTrVerifyRefService.selectMemberTrVerifyRefByMemberId(memberId);
        if (memberTrVerifyRef == null) {
            return;
        }
        MemberTrVerifyEntity memberTrVerifyEntity = memberTrVerifyEntityService.selectMemberTrVerifyEntityById(memberTrVerifyRef.getVerifyEntityId(),VerifyTypeEncryptMappingEnum.CELL_PHONE.getCode());
        //???????????????
        String verifyEntity = memberTrVerifyEntity.getVerifyEntity();
        String[] split = verifyEntity.split(MaConstant.SECURITY_TICKET_SUMMARY_SPLIT_CHAR);
        String mobileNo = uesServiceClient.getDataByTicket(split[0], EncryptType.AES);
        for (MemberTrBankAccount item : banks) {
            if (StringUtils.isBlank(item.getMobileNo())) {
                item.setMobileNo(mobileNo);
            }
        }
    }

	@Override
	public BankAccountInfoResponse queryBankAccountDetail(String bankcardId) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]?????????????????????????????????:bankcardId={}", bankcardId);
        }
        BankAccountInfoResponse response = new BankAccountInfoResponse();
        try {
            Long id = BankAccountFacadeValidator.checkBankId(bankcardId);
            MemberTrBankAccount bankAccount = memberTrBankAccountMapper.queryBankAccountByBankcardId(bankcardId);
            if (bankAccount == null
                    || BankAccountStatusEnum.DISABLED.getCode().equals(bankAccount.getStatus())) {
                response.setResponseCode(ResponseCode.NO_QUERY_RESULT.getCode());
                response.setResponseMessage(ResponseCode.NO_QUERY_RESULT.getMessage());
            } else {
               //???????????????
            	String bankAccountNo = bankAccount.getBankAccountNo();
            	String[] split = bankAccountNo.split("-");
            	bankAccountNo = uesServiceClient.getDataByTicket(split[0], EncryptType.AES);
            	bankAccount.setBankAccountNo(bankAccountNo);
               //???????????????
            	String mobileNo = bankAccount.getMobileNo();
            	mobileNo = uesServiceClient.getDataByTicket(mobileNo, EncryptType.AES);
            	bankAccount.setMobileNo(mobileNo);
            	response.setBankAcctInfo(bankAccount);
                ResponseUtil.setSuccessResponse(response);
            }
            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]?????????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "???????????????????????????");
        }

        return response;
	}

	@Override
	public BankAccInfoResponse addBankAccount(BankAccInfoRequest request) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]???????????????????????????????????????:request={}", request);
        }
        BankAccInfoResponse response = new BankAccInfoResponse();
        try {
            BankAccountFacadeValidator.validatorAddBankAccount(request);
            MemberTrBankAccount bank = request.getBankInfo();
            memberValidator.validateMemberExistAndNotCancelled(bank.getMemberId());

            MemberTrBankAccount bankAccount = createOrUpdateBankAccount(bank,MethodType.INSERT, false);
            response.setBankcardId(String.valueOf(bankAccount.getId()));
            ResponseUtil.setSuccessResponse(response);

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]???????????????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "?????????????????????");
        }

        return response;
	}

	private MemberTrBankAccount createOrUpdateBankAccount(MemberTrBankAccount bankAccount, MethodType type, boolean deleteOther) throws Exception {
		//1.????????????????????????
        //1.1??????
        //1.1.1???????????????????????????????????????????????????????????? memberId,??????,payAttribute ?????? in ('1','2')

        //1.1.2 ????????????????????????????????????

        //1.1.3 ??????????????????????????????????????????

        //1.2??????
        //1.2.1 ???????????????????????????????????????????????????????????????????????????????????????????????????

        //1.2.2 ??????????????????????????????????????????????????????

        fillCertNo(bankAccount);

        //????????????
        memberTmMemberMapper.lockMemberById(bankAccount.getMemberId());

        if (MethodType.INSERT == type) {
            if(deleteOther){
                //??????????????????????????????????????????
                disabledAllBankAccount(bankAccount.getMemberId(), bankAccount.getPayAttribute());
            }
            return addBankAccount(bankAccount);

        } else {
            return updateBankAct(bankAccount);

        }
	}
	
	private MemberTrBankAccount addBankAccount(MemberTrBankAccount bankAccount) throws MaBizException {
        if (BankAccountStatusEnum.NORMAL.getCode().equals(bankAccount.getStatus())) {
            bankAccount.setActivateDate(new Date());
        }

        //?????????????????????signNo??????signId?????????
        if (StringUtils.isNotBlank(bankAccount.getChannelCode())
            && (StringUtils.isNotBlank(bankAccount.getSignId()) || StringUtils.isNotBlank(bankAccount
                .getSignNo()))) {
            List<MemberTrBankAccount> list = null;
            if (StringUtils.isNotBlank(bankAccount.getSignId())
                && StringUtils.isNotBlank(bankAccount.getSignNo())) {
                list = memberTrBankAccountMapper.queryBySignNo(bankAccount);
            } else {
                list = memberTrBankAccountMapper.queryBySign(bankAccount);

            }
            if (CollectionUtils.isNotEmpty(list)) {
                throw new MaBizException(ResponseCode.BANK_ACCOUNT_TOO_MANY, "???????????????????????????????????????");
            }
        }
        if (StringUtils.isBlank(bankAccount.getBankAccountNo())) {
        	int trBankAccount = memberTrBankAccountMapper.insertMemberTrBankAccount(bankAccount);
            return bankAccount;
        }


        List<MemberTrBankAccount> bankAccountList = memberTrBankAccountMapper.selectMemberTrBankAccountList(bankAccount);
        List<MemberTrBankAccount> filterResultlist = new ArrayList<MemberTrBankAccount>();
        boolean isLock = bankAccountFilter.doFilter(bankAccountList, bankAccount, filterResultlist);
        //???????????????????????????
        if (isLock) {
            //TODO ?????????????????????????????????
            throw new MaBizException(ResponseCode.BANK_ACCOUNT_LOCK, "???????????????");
        }
        if (CollectionUtils.isEmpty(filterResultlist)) {
            //???????????????????????????????????????
            if (BankAccountStatusEnum.NORMAL.getCode().equals(bankAccount.getStatus())
                && CollectionUtils.isNotEmpty(bankAccountList)) {
                disabledBankCard(bankAccount);
            }
            //??????????????????????????????
            int trBankAccount = memberTrBankAccountMapper.insertMemberTrBankAccount(bankAccount);
        } else if (filterResultlist.size() == 1) {
        	bankAccount = filterResultlist.get(0);
        } else {
            //TODO ?????????????????????????????????
            throw new MaBizException(ResponseCode.BANK_ACCOUNT_TOO_MANY, "???????????????1???");
        }
        return bankAccount;
    
	}
	/**
     * ???????????????
     * @param bnkAccount
     * @throws MaBizException
     */
    private void disabledBankCard(MemberTrBankAccount bnkAccount) throws MaBizException {
    	bnkAccount.setStatus(BankAccountStatusEnum.DISABLED.getCode());
    	memberTrBankAccountMapper.updateStatus(bnkAccount);
    }

	private void disabledAllBankAccount(String memberId, String payAttribute) {
		memberTrBankAccountMapper.disabledAllBankAccount(memberId,payAttribute);
	}

	private void fillCertNo(MemberTrBankAccount bankAccount) throws Exception {
        if (YesNoEnum.YES.getCode() .equals(bankAccount.getIsFillCertNo())) {
            //??????????????? "[0-9]{2}([0-9]*)[0-9]{2}"
        	String ticket = uesServiceClient.encryptData(bankAccount.getBankAccountNo(), EncryptType.AES);
        	String summary = MaPatternUtil.getFiledMask(bankAccount.getBankAccountNo(), "[0-9]{2}([0-9]*)[0-9]{2}");
        	bankAccount.setCertNo(ticket + MaConstant.SECURITY_TICKET_SUMMARY_SPLIT_CHAR + summary);
        }
    }
	
	
	private MemberTrBankAccount updateBankAct(MemberTrBankAccount bankAccount) throws MaBizException {

        //????????????
		MemberTrBankAccount bnkAccount = memberTrBankAccountMapper.selectMemberTrBankAccountById(bankAccount.getId());

        //???????????????????????????????????????
        if (StringUtils.isNotBlank(bnkAccount.getBankAccountNo())) {
            if (StringUtils.isNotBlank(bankAccount.getBankAccountNo())
                && !StringUtils
                    .equals(bankAccount.getBankAccountNo(), bnkAccount.getBankAccountNo())) {
                throw new MaBizException(ResponseCode.BANK_ACCOUNT_PAY_DONT_UPDATE, "???????????????????????????????????????");

            }
        }

        if (bankAccount.getPayAttribute() != null
            && bankAccount.getPayAttribute() != bnkAccount.getPayAttribute()) {
            throw new MaBizException(ResponseCode.BANK_ACCOUNT_PAY_DONT_UPDATE, "???????????????????????????????????????");
        }
        if (StringUtils.isNotBlank(bankAccount.getSignId())
            || StringUtils.isNotBlank(bankAccount.getSignNo())
            || StringUtils.isNotBlank(bankAccount.getChannelCode())) {

        	MemberTrBankAccount queryBankAccount = new MemberTrBankAccount();

            putBankAccount(queryBankAccount, bnkAccount);
            putBankAccount(queryBankAccount, bankAccount);

            if (StringUtils.isNotBlank(queryBankAccount.getChannelCode())
                && (StringUtils.isNotBlank(queryBankAccount.getSignNo()) || StringUtils
                    .isNotBlank(queryBankAccount.getSignId()))) {

                List<MemberTrBankAccount> list = memberTrBankAccountMapper.queryBySign(queryBankAccount);
                if (!CollectionUtils.isEmpty(list)) {
                    for (MemberTrBankAccount act : list) {
                        if (!StringUtils.equals(act.getId().toString(), bankAccount.getId().toString())) {
                            throw new MaBizException(ResponseCode.BANK_ACCOUNT_TOO_MANY,
                                "???????????????????????????????????????");
                        }
                    }
                }

            }
        }
        //?????????????????????
        boolean isActiveCard = isActiveCardStatus(bankAccount.getStatus(), bnkAccount.getStatus());
        if (isActiveCard) {
            //??????????????????
            bankAccount.setActivateDate(new Date());
        }

        memberTrBankAccountMapper.updateMemberTrBankAccount(bankAccount);

        if (isActiveCard) {
        	memberTrBankAccountMapper.disabledBankAccount(memberTrBankAccountMapper.selectMemberTrBankAccountById(bankAccount.getId()));
        }
        return bnkAccount;

    }
	
	/**
     * ???????????????
     * @param orgBankAccount
     * @param newBankAccount
     */
    private void putBankAccount(MemberTrBankAccount orgBankAccount, MemberTrBankAccount newBankAccount) {
        if (StringUtils.isNotBlank(newBankAccount.getSignId())) {
            orgBankAccount.setSignId(newBankAccount.getSignId());
        }
        if (StringUtils.isNotBlank(newBankAccount.getSignNo())) {
            orgBankAccount.setSignNo(newBankAccount.getSignNo());
        }
        if (StringUtils.isNotBlank(newBankAccount.getChannelCode())) {
            orgBankAccount.setChannelCode(newBankAccount.getChannelCode());
        }
    }
    
    /**
     * ????????????????????????
     * @param newStatus
     * @param orgStatus
     * @return
     */
    private boolean isActiveCardStatus(Integer newStatus, Integer orgStatus) {
        if (newStatus == null) {
            return false;
        }
        if (BankAccountStatusEnum.UNACTIVE.getCode().equals(orgStatus)
            && BankAccountStatusEnum.NORMAL.getCode().equals(newStatus)) {
            return true;
        }
        return false;

    }

	@Override
	public BankAccInfoResponse updateBankAccount(BankAccInfoRequest request) {
	 if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]???????????????????????????????????????:request={}", request);
        }
        BankAccInfoResponse response = new BankAccInfoResponse();
        try {
            BankAccountFacadeValidator.validatorUpdateBankAccount(request);
            MemberTrBankAccount bankAccount = bankAccountValidator.validateBankAccountExist(request.getBankInfo());
            createOrUpdateBankAccount(request.getBankInfo(), MethodType.UPDATE, false);
            response.setBankcardId(String.valueOf(request.getBankInfo().getId()));
            ResponseUtil.setSuccessResponse(response);
            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]???????????????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "?????????????????????????????????");
        }
        return response;
	}
	
}
