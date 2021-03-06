package com.yiranpay.member.service.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.yiranpay.member.mapper.MemberTmMemberIdentityMapper;
import com.yiranpay.member.mapper.MemberTmMemberMapper;
import com.yiranpay.member.mapper.MemberTmMerchantMapper;
import com.yiranpay.member.mapper.MemberTmOperatorMapper;
import com.yiranpay.member.mapper.MemberTrCompanyMemberMapper;
import com.yiranpay.member.mapper.MemberTrPersonalMemberMapper;
import com.yiranpay.member.request.ActivatePersonalRequest;
import com.yiranpay.member.request.CreateMemberInfoRequest;
import com.yiranpay.member.request.IntegratedCompanyRequest;
import com.yiranpay.member.request.IntegratedPersonalRequest;
import com.yiranpay.member.request.MemberIntegratedIdRequest;
import com.yiranpay.member.request.MemberIntegratedRequest;
import com.yiranpay.member.request.PersonalMemberInfoRequest;
import com.yiranpay.member.request.UpdateMemberLockStatusRequest;
import com.yiranpay.member.response.ActivatePersonalResponse;
import com.yiranpay.member.response.CreateMemberInfoResponse;
import com.yiranpay.member.response.IntegratedCompanyResponse;
import com.yiranpay.member.response.IntegratedPersonalResponse;
import com.yiranpay.member.response.MemberIntegratedResponse;
import com.yiranpay.common.core.text.Convert;
import com.yiranpay.common.utils.StrUtils;
import com.yiranpay.member.base.Response;
import com.yiranpay.member.constant.FieldLength;
import com.yiranpay.member.constant.MaConstant;
import com.yiranpay.member.domain.AccountDomain;
import com.yiranpay.member.domain.MemberAndAccount;
import com.yiranpay.member.domain.MemberIntegratedQuery;
import com.yiranpay.member.domain.MemberTmMember;
import com.yiranpay.member.domain.MemberTmMemberIdentity;
import com.yiranpay.member.domain.MemberTmMerchant;
import com.yiranpay.member.domain.MemberTmOperator;
import com.yiranpay.member.domain.MemberTrCompanyMember;
import com.yiranpay.member.domain.MemberTrMemberAccount;
import com.yiranpay.member.domain.MemberTrPersonalMember;
import com.yiranpay.member.domain.MemberTrVerifyEntity;
import com.yiranpay.member.domain.PayPassWord;
import com.yiranpay.member.domain.PersonalMember;
import com.yiranpay.member.domain.Verify;
import com.yiranpay.member.enums.AccountCategoryEnum;
import com.yiranpay.member.enums.ActivateStatusEnum;
import com.yiranpay.member.enums.LockEnum;
import com.yiranpay.member.enums.MemberAccountStatusEnum;
import com.yiranpay.member.enums.MemberStatusEnum;
import com.yiranpay.member.enums.MemberTypeEnum;
import com.yiranpay.member.enums.ResponseCode;
import com.yiranpay.member.exception.MaBizException;
import com.yiranpay.member.service.DataEncryptService;
import com.yiranpay.member.service.IMemberSequenceService;
import com.yiranpay.member.service.IMemberTmMemberService;
import com.yiranpay.member.service.IMemberTmOperatorService;
import com.yiranpay.member.service.IMemberTrMemberAccountService;
import com.yiranpay.member.service.IMemberTrPasswordService;
import com.yiranpay.member.service.IMemberTrVerifyEntityService;
import com.yiranpay.member.service.IMemberTrVerifyRefService;
import com.yiranpay.member.utils.AccountDomainUtil;
import com.yiranpay.member.utils.MemberDomainUtil;
import com.yiranpay.member.utils.MemberTypeUtil;
import com.yiranpay.member.utils.OperatorDomainUtil;
import com.yiranpay.member.utils.ResponseUtil;
import com.yiranpay.member.utils.SQLExceptionUtil;
import com.yiranpay.member.utils.Utils;
import com.yiranpay.member.validator.MemberFacadeValidator;
import com.yiranpay.member.validator.MemberValidator;
import com.yiranpay.payorder.enums.EncryptType;
import com.yiranpay.payorder.service.IUesServiceClient;

import cn.hutool.core.util.RandomUtil;

import com.alibaba.fastjson.JSON;

/**
 * ?????? ???????????????
 * 
 * @author yiran
 * @date 2019-03-30
 */
@Service
public class MemberTmMemberServiceImpl implements IMemberTmMemberService 
{
	private Logger        logger = LoggerFactory.getLogger(MemberTmMemberServiceImpl.class);
	@Autowired
	private MemberTmMemberMapper memberTmMemberMapper;
	@Autowired
	private MemberTmMemberIdentityMapper memberTmMemberIdentityMapper;
	@Autowired
	private IMemberTmOperatorService memberTmOperatorService;
	@Autowired
	private IUesServiceClient uesServiceClient;
	@Autowired
	private IMemberTrMemberAccountService memberTrMemberAccountService;
	@Autowired
	IMemberTrVerifyEntityService memberTrVerifyEntityService;
	@Autowired
	private DataEncryptService dataEncryptService;
	@Autowired
	private MemberTrPersonalMemberMapper memberTrPersonalMemberMapper;
	@Autowired
	private MemberValidator memberValidator;
	@Autowired
	private IMemberTrPasswordService memberTrPasswordService;
	@Autowired
	private MemberTmMerchantMapper memberTmMerchantMapper;
	@Autowired
	private MemberTrCompanyMemberMapper memberTrCompanyMemberMapper;
	@Autowired
	private IMemberSequenceService memberSequenceService;
	/**
     * ??????????????????
     * 
     * @param memberId ??????ID
     * @return ????????????
     */
    @Override
	public MemberTmMember selectMemberTmMemberById(String memberId)
	{
	    return memberTmMemberMapper.selectMemberTmMemberById(memberId);
	}
	
	/**
     * ??????????????????
     * 
     * @param memberTmMember ????????????
     * @return ????????????
     */
	@Override
	public List<MemberTmMember> selectMemberTmMemberList(MemberTmMember memberTmMember)
	{
	    return memberTmMemberMapper.selectMemberTmMemberList(memberTmMember);
	}
	
    /**
     * ????????????
     * 
     * @param memberTmMember ????????????
     * @return ??????
     */
	@Override
	public int insertMemberTmMember(MemberTmMember memberTmMember)
	{
	    return memberTmMemberMapper.insertMemberTmMember(memberTmMember);
	}
	
	/**
     * ????????????
     * 
     * @param memberTmMember ????????????
     * @return ??????
     */
	@Override
	public int updateMemberTmMember(MemberTmMember memberTmMember)
	{
	    return memberTmMemberMapper.updateMemberTmMember(memberTmMember);
	}

	/**
     * ??????????????????
     * 
     * @param ids ?????????????????????ID
     * @return ??????
     */
	@Override
	public int deleteMemberTmMemberByIds(String ids)
	{
		return memberTmMemberMapper.deleteMemberTmMemberByIds(Convert.toStrArray(ids));
	}

	@Override
	public CreateMemberInfoResponse createMemberInfo(CreateMemberInfoRequest request) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]??????????????????:request={}", request);
        }
        CreateMemberInfoResponse response = new CreateMemberInfoResponse();
        try {
            //??????????????????
            MemberFacadeValidator.validator(request);
            //????????????
            MemberTmMember member = MemberDomainUtil.convertReqToMember(request);
            logger.info("????????????????????????:{}",JSON.toJSONString(member));
            MemberTmOperator operator = OperatorDomainUtil.convertReqToDefaultOperator(request);
            if (StringUtils.isNotBlank(request.getLoginPassword())) {
                String ticket = uesServiceClient.encryptData(StringUtils.trim(request.getLoginPassword()), EncryptType.AES);
                operator.setPassword(ticket);
            } else {
                operator.setPassword(null);
            }
            MemberAndAccount ma = createMember(member, operator);
            
            response.setMemberId(ma.getMemberId());
            response.setOperatorId(ma.getOperatorId());
            ResponseUtil.setSuccessResponse(response);

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]??????????????????:response={}", response);
            }
        } catch (Exception e) {
        	ResponseUtil.fillResponse(response, e, "????????????");
        }
        return response;
	}

	/**
	 * ????????????
	 * @param member
	 * @param operator
	 * @return
	 */
	private MemberAndAccount createMember(MemberTmMember member, MemberTmOperator operator) {
		String memberId = genMemberId(member.getMemberType());
        member.setMemberId(memberId);
        operator.setMemberId(memberId);
        
        try {
            MemberAndAccount ma = store(member, operator);
            return ma;
        } catch (MaBizException e) {
            /*if (ResponseCode.MEMBER_IDENTITY_EXIST.equals(e.getCode())) {
                //memberRepository.checkIdentityException(member.getIdentitys());
                //?????????modify 2013-12-09
                return reStore(member, operator);
            }else{
                throw e;
            }*/
        }
        
		return null;
	}

	private MemberAndAccount store(MemberTmMember member, MemberTmOperator operator) throws MaBizException {
		MemberAndAccount ma = new MemberAndAccount();
        //??????????????????
        try {
            this.createIdentity(member);
        } catch (Exception e) {
            logger.warn("????????????????????????", e);
            if (SQLExceptionUtil.isUniqueException(e)) {
                throw new MaBizException(ResponseCode.MEMBER_IDENTITY_EXIST);
            } else {
                throw new MaBizException(ResponseCode.MEMBER_CREATE_FAIL, e.getMessage());
            }
        }
        //????????????
        memberTmMemberMapper.insertMemberTmMember(member);
        //????????????????????????????????????
        memberTmOperatorService.store(operator);
 
        ma.setMemberId(member.getMemberId());
        ma.setOperatorId(operator.getOperatorId());
        return ma;
	}

	private void createIdentity(MemberTmMember member) {
		List<MemberTmMemberIdentity> identitys = MemberDomainUtil.convertToMemberIdentityDO(member);
        for (MemberTmMemberIdentity item : identitys) {
        	memberTmMemberIdentityMapper.insertMemberTmMemberIdentity(item);
        }
		
	}

	/**
	 * ????????????????????????ID
	 * @param memberType
	 * @return
	 */
	private String genMemberId(Integer memberType) {

        String pre = null;
        String seq = memberSequenceService.getMenberSequenceNo("SEQ_MEMBER_ID");

        if (MemberTypeUtil.isCompanyMemberType(memberType)) {
            pre = MaConstant.PRE_MEMBER_COMPANY_ID;
        } else if (MemberTypeUtil.isPersonMemberType(memberType)) {
            pre = MaConstant.PRE_MEMBER_PERSONAL_ID;
        } else if (MemberTypeUtil.isVirtualMemberType(memberType)){
        	pre = MaConstant.PRE_MEMBER_VIRUTLMERCHANT_ID;
        }else if (MemberTypeUtil.isVirtualMemberType(memberType)){
        	pre = MaConstant.PRE_MERCHANT_ID;
        }else {
            pre = MaConstant.PRE_MEMBER_INSTITUTION_ID;
        }
        String memberId = pre
                          + StrUtils.alignRight(seq, MaConstant.MEMBER_ID_SEQ_LENGTH,
                              MaConstant.ID_FIX_CHAR);

        return memberId;
    }

	private String genMerchantId() {
		String prefix = MaConstant.PRE_MERCHANT_ID;
		int seqLen = FieldLength.MERCHANT_ID - prefix.length();
		String merchantId = prefix
				+ StrUtils.alignRight(
						String.valueOf(memberSequenceService.getMenberSequenceNo("SEQ_MERCHANT_ID")),
						seqLen, MaConstant.ID_FIX_CHAR);
		return merchantId;
	}
	
	@Override
	public MemberIntegratedResponse queryMemberIntegratedInfo(MemberIntegratedRequest request) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]??????????????????????????????:request={},environment={}", request);
        }
        MemberIntegratedResponse response = new MemberIntegratedResponse();
        try {
            //??????????????????
            MemberFacadeValidator.validator(request);
            MemberTmMember member = queryMember(MemberDomainUtil.convertReqToMemberIntegratedQuery(request));
            if (member == null) {
                response.setResponseCode(ResponseCode.MEMBER_NOT_EXIST.getCode());
                response.setResponseMessage(ResponseCode.MEMBER_NOT_EXIST.getMessage());
            } else {
                response = MemberDomainUtil.convertToMemberIntegratedResponse(member);
                ResponseUtil.setSuccessResponse(response);
            }

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]??????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "????????????????????????");

        }
        return response;
	}

	private MemberTmMember queryMember(MemberIntegratedQuery query) {
		// query ??????????????????
		MemberTmMember member = null;
        if (StringUtils.isNotEmpty(query.getMemberId())) {
            member = memberTmMemberMapper.selectMemberTmMemberById(query.getMemberId());
        } else {
            String memberId = memberTmMemberIdentityMapper.queryMemberId(query.getMemberIdentity(), query.getPlatformType());
            if (StringUtils.isNotEmpty(memberId)) {
                member = memberTmMemberMapper.selectMemberTmMemberById(memberId);
            }
        }
        if (member == null) {
            return null;
        }
        if (query.isRequireAccountInfos()) {
            List<AccountDomain> accounts = null;
            if (CollectionUtils.isEmpty(query.getAccountTypes())) {
                accounts = memberTrMemberAccountService.getAccounts(
                		member.getMemberId(), 
                		null, 
                		AccountCategoryEnum.DPM
                );
            } else if (query.getAccountTypes().size() == 1) {
                String type = String.valueOf(query.getAccountTypes().get(0));
                accounts 	= memberTrMemberAccountService.getAccounts(
                		member.getMemberId(), 
                		type,
                		AccountCategoryEnum.DPM
                );
            } else {
                accounts = memberTrMemberAccountService.getAccounts(member.getMemberId(), query.getAccountTypes());
            }
            member.setAccounts(accounts);
        }
        if (query.isRequireVerifyInfos()) {
            //??????????????????????????????
            List<MemberTrVerifyEntity> verifys = memberTrVerifyEntityService.queryByMember(member.getMemberId(),null);
            member.setVerifys(verifys);
        }
        if (query.isRequireDefaultOperator()) {
            member.setDefaultOperator(memberTmOperatorService.selectMemberTmOperatorByMemberId(member.getMemberId()));
        }
        return member;
	}

	@Override
	public MemberIntegratedResponse queryMemberIntegratedInfoById(MemberIntegratedIdRequest request) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]??????????????????????????????:request={},environment={}", request);
        }
        MemberIntegratedResponse response = new MemberIntegratedResponse();
        try {
            //??????????????????
            MemberFacadeValidator.validator(request);
            MemberTmMember member = queryMember(MemberDomainUtil.convertReqToMemberIntegratedQuery(request));
            if (member == null) {
                response.setResponseCode(ResponseCode.MEMBER_NOT_EXIST.getCode());
                response.setResponseMessage(ResponseCode.MEMBER_NOT_EXIST.getMessage());
            } else {
                response = MemberDomainUtil.convertToMemberIntegratedResponse(member);
                ResponseUtil.setSuccessResponse(response);
            }

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]??????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "????????????????????????");
        }
        return response;
	}

	@Override
	public IntegratedPersonalResponse createIntegratedPersonalMember(IntegratedPersonalRequest request) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]??????????????????????????????:request={}", request);
        }
        IntegratedPersonalResponse response = new IntegratedPersonalResponse();

        try {
            //1 ??????????????????
            MemberFacadeValidator.validator(request);
            //2 ??????????????????????????????
            PersonalMember pm = MemberDomainUtil.convertReqToPersonalMember(request);
            MemberTmOperator op = OperatorDomainUtil.convertReqToDefaultOperator(request);
            MemberAccountStatusEnum status = MemberAccountStatusEnum.getByCode(request.getMemberAccountFlag());
            status = status == null ? MemberAccountStatusEnum.NOTACTIVATED : status;
            //3 ??????????????????????????????
            MemberAndAccount ma = integratedCreatePersonalMember(pm, op, status);
            response.setAccountId(ma.getAccountId());
            response.setCreateTime(new Date());
            response.setMemberId(ma.getMemberId());
            response.setOperatorId(ma.getOperatorId());
            ResponseUtil.setSuccessResponse(response);

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]??????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "????????????????????????");
        }
        return response;
	}

	/**
	 * ????????????????????????
	 * @param pm
	 * @param op
	 * @param status
	 * @return
	 * @throws Exception 
	 */
	private MemberAndAccount integratedCreatePersonalMember(PersonalMember member, MemberTmOperator operator,
			MemberAccountStatusEnum statusEnum) throws Exception {
		 //1 ??????????????????
        dataEncryptService.encrypt(member, operator);
        //2 ??????????????????
        String memberId = genMemberId(member.getMemberType());
        member.setMemberId(memberId);
        operator.setMemberId(memberId);
        //???????????????????????????
        if (needBasicAccount(statusEnum)) {
            generateBasicAccount(member,statusEnum);
        }

        MemberAndAccount ma = new MemberAndAccount();
        ma = personalMemberStore(member, operator);
        //??????????????????
        if (needBasicAccount(statusEnum)) {
            doOpenBasicAccount(member, ma);
        }
        return ma;
	}
	private void doOpenBasicAccount(MemberTmMember member, MemberAndAccount ma) {
        //3 ????????????
        //String accountId = memberTrMemberAccountService.openAccount(member.getBaseAccount());
        //4 ????????????????????????????????????????????????
        //ma.setAccountId(accountId);
        member.getBaseAccount().setAccountId(ma.getAccountId());
        reStoreBaseAccount(member.getBaseAccount());
    }

	private void reStoreBaseAccount(AccountDomain baseAccount) {
		
		int i = memberTmMemberMapper.updateActiveTime(MemberStatusEnum.NORMAL.getCode().intValue(), baseAccount.getMemberId());
		 if (i == 0) {
	            try {
					throw new MaBizException(ResponseCode.MEMBER_ACTIVE_FAIL);
				} catch (MaBizException e) {
					e.printStackTrace();
				}
	        }
		/*int j= memberTrMemberAccountService.updateAccountId(baseAccount.getAccountId(),
				baseAccount.getAccountName(), baseAccount.getMemberId());
	        if (j == 0) {
	            try {
					throw new MaBizException(ResponseCode.MEMBER_ACTIVE_FAIL);
				} catch (MaBizException e) {
					e.printStackTrace();
				}
	        }*/
	}

	private MemberAndAccount personalMemberStore(PersonalMember member, MemberTmOperator operator) {
		MemberAndAccount ma = new MemberAndAccount();
        //??????????????????
        try {
            this.createIdentity(member);
        } catch (Exception e) {
            if (SQLExceptionUtil.isUniqueException(e)) {
                try {
					throw new MaBizException(ResponseCode.MEMBER_IDENTITY_EXIST);
				} catch (MaBizException e1) {
					e1.printStackTrace();
				}
            } else {
                logger.warn("????????????????????????", e);
                try {
					throw new MaBizException(ResponseCode.MEMBER_CREATE_FAIL, e.getMessage());
				} catch (MaBizException e1) {
					e1.printStackTrace();
				}
            }
        }
        //????????????
        memberTmMemberMapper.insertMemberTmMember(MemberDomainUtil.convertToMemberDO(member));
        MemberTrPersonalMember personalMemberDO = MemberDomainUtil.convertToPersonalMemberDO(member);
        //??????????????????
        memberTrPersonalMemberMapper.insertMemberTrPersonalMember(personalMemberDO);
        //????????????????????????????????????
        memberTmOperatorService.store(operator);
        //??????????????????
        memberTrVerifyEntityService.addVerifys(member.getVerifys(), member.getMemberId());
        /*//??????????????????
        if ((member.getAccounts() != null || member.getAccounts().size() > 0)) {
        	//???????????????????????????
        	memberTrMemberAccountService.insertMemberTrMemberAccount(member.getBaseAccount());
        }*/
        ma.setMemberId(member.getMemberId());
        ma.setOperatorId(operator.getOperatorId());
        return ma;
	}

	private boolean needBasicAccount(MemberAccountStatusEnum statusEnum) {
        return (MemberAccountStatusEnum.ACTIVATED == statusEnum) || (MemberAccountStatusEnum.ACTIVATED_ALL == statusEnum);
    }
	
	 private void generateBasicAccount(PersonalMember member, MemberAccountStatusEnum statusEnum) {
	        ActivateStatusEnum activate = null;
	        if(MemberAccountStatusEnum.ACTIVATED_ALL == statusEnum){
	            activate = ActivateStatusEnum.ACTIVATED;
	        }else{
	            activate = ActivateStatusEnum.NOTACTIVATED;
	        }
	        AccountDomain accountDomain = AccountDomainUtil.buildOpenBaseAccountRequest(member,activate);
	        member.addAccount(accountDomain);
	    }
	 
	 private void generateBasicAccount(MemberTrCompanyMember member, MemberAccountStatusEnum statusEnum) {
	        ActivateStatusEnum activate = null;
	        if(MemberAccountStatusEnum.ACTIVATED_ALL == statusEnum){
	            activate = ActivateStatusEnum.ACTIVATED;
	        }else{
	            activate = ActivateStatusEnum.NOTACTIVATED;
	        }
	        AccountDomain accountDomain = AccountDomainUtil.buildOpenBaseAccountRequest(member,activate);
	        member.addAccount(accountDomain);
	    }

	@Override
	public Response setPersonalMemberInfo(PersonalMemberInfoRequest request) {

        if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]??????????????????????????????:request={}", request);
        }
        Response response = new Response();
        try {
            //??????????????????
            MemberFacadeValidator.validator(request);
            PersonalMember member = MemberDomainUtil.convertReqToPersonalMember(request);
            boolean rest = setPersonalMember(member);
            if (rest) {
                ResponseUtil.setSuccessResponse(response);
            } else {
                response.setResponseCode(ResponseCode.MEMBER_UPDATE_FAIL.getCode());
                response.setResponseMessage(ResponseCode.MEMBER_UPDATE_FAIL.getMessage());
            }

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]??????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "????????????????????????");
        }
        return response;
	}

	private boolean setPersonalMember(PersonalMember member) throws MaBizException {
		 //??????????????????
		MemberTmMember m = memberValidator.validateMemberExist(member.getMemberId());
        if (!MemberTypeUtil.isPersonMemberType(m.getMemberType())) {
        	String str = "";
        	if(m.getMemberType() == 1){//1?????? 2 ?????? 3 ?????? 9 ????????????
        		str = "??????";
        	}else if(m.getMemberType() == 2){
        		str = "??????";
        	}else if(m.getMemberType() == 3){
        		str = " ??????";
        	}else if(m.getMemberType() == 9){
        		str = "????????????";
        	}
            throw new MaBizException(ResponseCode.MEMBER_TYPE_FAIL, "??????????????????"
                                                                    + m.getMemberId()
                                                                    + "??????????????????"
                                                                    + str);
        }
        int row = updatePersonMemberInfo(member);
        return row >= 1 ? true : false;
	}

	private int updatePersonMemberInfo(PersonalMember member) {
		 int row = 0;
        MemberTrPersonalMember personalMember = memberTrPersonalMemberMapper.selectMemberTrPersonalMemberById(member.getMemberId());
        MemberTmMember tmMemberDO = new MemberTmMember();
        tmMemberDO.setMemberId(member.getMemberId());
        tmMemberDO.setMemberName(member.getMemberName());
        tmMemberDO.setInvitCode(member.getInvitCode());
        
        row = memberTmMemberMapper.updateMember(tmMemberDO);
        MemberTrPersonalMember trPersonalMember = MemberDomainUtil.convertToPersonalMemberDO(member);
        if (personalMember == null) {
            //??????
            int memberId = memberTrPersonalMemberMapper.insertMemberTrPersonalMember(trPersonalMember);
            if (member.getMemberId().equals(memberId)) {
                row++;
            }
        } else {
            //??????
            row += memberTrPersonalMemberMapper.updateMemberTrPersonalMember(trPersonalMember);
        }
        return row;
	}

	/**
	 * ??????????????????
	 */
	@Override
	public ActivatePersonalResponse activatePersonalMemberInfo(ActivatePersonalRequest request) {
		 if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]????????????????????????:request={}", request);
        }
        ActivatePersonalResponse response = new ActivatePersonalResponse();
        try {
            //??????????????????
            MemberFacadeValidator.validator(request);
            //????????????
            PersonalMember member = MemberDomainUtil.convertReqToPersonalMember(request);
            MemberTmOperator operator = new MemberTmOperator();
            operator.setMemberId(member.getMemberId());
            boolean isActivate = request.isActivateAccount();
            if (StringUtils.isNotBlank(request.getPayPassword())) {
                String ticket = uesServiceClient.encryptData(StringUtils.trim(request
                    .getPayPassword()),EncryptType.AES);
                PayPassWord payPassWord = new PayPassWord();
                payPassWord.setPassWord(ticket);
                operator.addPayPwd(payPassWord);
                isActivate = true;
            }
            MemberAndAccount ma = activatePersonalMember(member, operator, isActivate);
            response.setAccountId(ma.getAccountId());
            ResponseUtil.setSuccessResponse(response);

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "??????????????????");
        }

        return response;
	}

	/**
	 * ??????????????????
	 * @param member
	 * @param operator
	 * @param isActivate
	 * @return
	 * @throws MaBizException 
	 */
	private MemberAndAccount activatePersonalMember(PersonalMember member, MemberTmOperator operator,
			boolean isActivate) throws MaBizException {

        MemberAndAccount ma = new MemberAndAccount();
        //????????????????????????
        MemberTmMember m = memberValidator.validateMemberExist(member.getMemberId());
        //????????????????????????????????????
        if (MemberStatusEnum.UNACTIVE.getCode().intValue() != m.getStatus()) {
        	String str = "";
        	if(m.getStatus() == 0){//UNACTIVE(0L, "?????????"), NORMAL(1L, "??????"), SLEEP(2L, "??????"), CANCEL(3L, "??????");
        		str = "?????????";
        	}else if(m.getStatus() == 1){
        		str = "??????";
        	}else if(m.getStatus() == 2){
        		str = "??????";
        	}else if(m.getStatus() == 3){
        		str = "??????";
        	}
            throw new MaBizException(ResponseCode.MEMBER_ALREADY_ACTIVE, "???????????????"
                                                                         + member.getMemberId()
                                                                         + "?????????"
                                                                         +str);
        }
        MemberTypeEnum memberType = MemberTypeEnum.getByCode(m.getMemberType().longValue());
        if (MemberTypeUtil.isCompanyMemberType(memberType.getCode().intValue())) {
            throw new MaBizException(ResponseCode.MEMBER_TYPE_FAIL);
        } else if (MemberTypeUtil.isPersonMemberType(m.getMemberType()) && !(member instanceof PersonalMember)) {
            throw new MaBizException(ResponseCode.MEMBER_TYPE_FAIL);
        }

        PayPassWord paypwd = (operator == null ? null : operator.getBasePayPassword());
        ActivateStatusEnum status = null;
        if (!isActivate) {
            //???????????????????????????
            status = ActivateStatusEnum.NOTACTIVATED;
        } else {
            status = ActivateStatusEnum.ACTIVATED;
        }
        AccountDomain account = AccountDomainUtil.buildOpenBaseAccountRequest(member, status);
        //???1?????????????????????????????????
        storeForUpdate(account);
        if (StringUtils.isBlank(account.getAccountId())) {
            //???2?????????????????????????????????
            String accountId = memberTrMemberAccountService.openAccount(account);
            account.setAccountId(accountId);
        }
        if (member.getAccounts() != null) {
            member.getAccounts().clear();
        }
        member.addAccount(account);
        //???3???1.??????????????????????????????????????????????????????????????????,??????????????????
        //     2.??????????????????????????????????????????????????????????????????????????????,??????????????????
        MemberTmOperator defaultOperator = memberTmOperatorService.selectMemberTmOperatorByMemberId(member.getMemberId());
        if (!((paypwd == null) || (paypwd.getPassWord() == null))) {
            operator.getBasePayPassword().setAccountId(account.getAccountId());
            operator.setOperatorId(defaultOperator.getOperatorId());
            ma.setOperatorId(defaultOperator.getOperatorId());
        } else {
            operator = null;
        }
        activeMember(member, operator);
        ma.setAccountId(account.getAccountId());
        ma.setMemberId(member.getMemberId());
        ma.setOperatorId(defaultOperator.getOperatorId());
        return ma;
	}

	private void activeMember(PersonalMember member, MemberTmOperator operator) {
		reStoreBaseAccount(member.getBaseAccount());
        if (operator != null && operator.getBasePayPassword() != null) {
        	memberTrPasswordService.store(operator);
        }
	}

	public void storeForUpdate(AccountDomain account) throws MaBizException {
        //????????????
		memberTmMemberMapper.getMemberByIdForUpdate(account.getMemberId());

        Long accountType = account.getAccountType();
        // ?????????????????????????????????????????????
        List<MemberTrMemberAccount> accountList = memberTrMemberAccountService.queryAllByMemberAndTypeId(
            account.getMemberId(), String.valueOf(accountType), AccountCategoryEnum.DPM.getCode());
        // ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (null != accountList && accountList.size() > 0) {
            for (MemberTrMemberAccount item : accountList) {
                /*
                if (StringUtil.isEmpty(item.getAccountId())) {
                    account.setOriginalRequestNo(item.getOriginalRequestNo());
                    return;
                }
                */
                //modify 20130619,??????????????????????????????????????????????????????
                if (account.getAccountName().equals(item.getAlias())) {
                    account.setOriginalRequestNo(item.getOriginalRequestNo());
                    account.setAccountId(item.getAccountId());
                    return;
                }
            }
        }
        account.setOriginalRequestNo(String.valueOf(RandomUtil.randomInt(10000000)));
        // ??????????????????????????????????????????
        memberTrMemberAccountService.insertMemberTrMemberAccount(account);
    }

	/**
	 * ????????????????????????
	 */
	@Override
	public Response updateMemberLockStatus(UpdateMemberLockStatusRequest request) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]??????????????????????????????:request={}", request);
        }
        Response response = new Response();
        try {
            //??????????????????
            MemberFacadeValidator.validator(request);
            MemberTmMember member = MemberDomainUtil.convertReqToMember(request);
            boolean rest = updateMemberLockStatus(member);
            if (rest) {
                ResponseUtil.setSuccessResponse(response);
            } else {
                if(LockEnum.LOCKED.getCode().intValue() == member.getLockStatus()){
                    response.setResponseCode(ResponseCode.MEMBER_LOCK_FAIL.getCode());
                    response.setResponseMessage(ResponseCode.MEMBER_LOCK_FAIL.getMessage());
                }else{
                    response.setResponseCode(ResponseCode.MEMBER_UNLOCK_FAIL.getCode());
                    response.setResponseMessage(ResponseCode.MEMBER_UNLOCK_FAIL.getMessage());
                }
            }

            if (logger.isInfoEnabled()) {
                logger.info("[APP<-MA_1]??????????????????????????????:response={}", response);
            }
        } catch (Exception e) {
            ResponseUtil.fillResponse(response, e, "????????????????????????");
        }
        return response;
	}

	private boolean updateMemberLockStatus(MemberTmMember member) throws MaBizException {
		MemberTmMember memberDo = memberValidator.validateMemberExist(member.getMemberId());
        if (memberDo.getLockStatus().equals(member.getLockStatus())) {
            return true;
        }
        int row = memberTmMemberMapper.updateLockStatus(member);
        return row == 1 ? true : false;
	}

	@Override
	public IntegratedCompanyResponse createIntegratedCompanyMember(IntegratedCompanyRequest request) {
		if (logger.isInfoEnabled()) {
            logger.info("[APP->MA_1]??????????????????????????????:request={}", request);
        }
         IntegratedCompanyResponse response = new IntegratedCompanyResponse();
         try {
	         /* ????????????????????????
	         * 1.??????????????????-???????????????????????? createMemberInfo
	         * 2.??????????????????
	         * 3.???????????????
	         * */
	         //??????????????????
	         MemberFacadeValidator.validator(request);
	         
	         //????????????
	         //????????????
	         MemberTrCompanyMember companyMember = MemberDomainUtil.convertReqToMember(request);
	         logger.info("??????????????????--->????????????????????????:"+JSON.toJSONString(companyMember));
	         //?????????
	         MemberTmOperator operator = OperatorDomainUtil.convertReqToDefaultOperator(request);
	         logger.info("??????????????????--->?????????:"+JSON.toJSONString(operator));
	         if (StringUtils.isNotBlank(request.getLoginPassword())) {
	             String ticket = uesServiceClient.encryptData(StringUtils.trim(request
	                 .getLoginPassword()),EncryptType.AES);
	             operator.setPassword(ticket);
	         } else {
	             operator.setPassword(null);
	         } 
	         //??????????????????
	         MemberAccountStatusEnum accountStatus = MemberAccountStatusEnum.getByCode(request
	             .getMemberAccountFlag());
	         accountStatus = accountStatus == null ? MemberAccountStatusEnum.NOTACTIVATED : accountStatus;
	         //??????????????????-??????????????????
	         MemberTmMerchant merchant = null;
	         if(null != request.getMerchantInfo()){
	             merchant = request.getMerchantInfo();
	         }
	         logger.info("??????????????????--->??????????????????:"+JSON.toJSONString(merchant));
	         //??????????????????-??????????????????setCompanyMember
	         boolean haveCompanyInfo = false;
	         if(null != request.getCompanyInfo()){
	            haveCompanyInfo = true;
	         }
	         MemberAndAccount ma  = integratedCreateCompanyMember(companyMember, operator, merchant, accountStatus, haveCompanyInfo);
	         response.setAccountId(ma.getAccountId());
	         response.setMemberId(ma.getMemberId());
	         response.setOperatorId(ma.getOperatorId());
	         response.setMerchantId(ma.getMerchantId());
	         ResponseUtil.setSuccessResponse(response);
	 
	         if (logger.isInfoEnabled()) {
	             logger.info("[APP<-MA_1]??????????????????:response={}", response);
	         }
         } catch (Exception e) {
             ResponseUtil.fillResponse(response, e, "????????????????????????");
         }
         return response;
	}

	private MemberAndAccount integratedCreateCompanyMember(MemberTrCompanyMember member,
			MemberTmOperator operator, MemberTmMerchant merchant, MemberAccountStatusEnum statusEnum,
			boolean haveCompanyInfo) throws MaBizException {
		String memberId = genMemberId(member.getMemberType());
		String merchantId = genMerchantId();
        member.setMemberId(memberId);
        operator.setMemberId(memberId);
        merchant.setMerchantId(merchantId);
        //???????????????????????????
        if (needBasicAccount(statusEnum)) {
           generateBasicAccount(member,statusEnum);
        }
        MemberAndAccount ma = new MemberAndAccount();
        ma = integratedStore(member, operator, merchant,haveCompanyInfo);
        if (needBasicAccount(statusEnum)) {
            //???????????????????????????
            doOpenBasicAccount(member, ma);
        }
        return ma;
	}

	private MemberAndAccount integratedStore(MemberTrCompanyMember member, MemberTmOperator defaultOperator,
			MemberTmMerchant merchant, boolean haveCompanyInfo) throws MaBizException {
		MemberAndAccount ma = new MemberAndAccount();
        //??????????????????
        try {
            this.createIdentity(member);
        } catch (Exception e) {
            logger.warn("????????????????????????", e);
            if (SQLExceptionUtil.isUniqueException(e)) {
                throw new MaBizException(ResponseCode.MEMBER_IDENTITY_EXIST);
            } else {
                throw new MaBizException(ResponseCode.MEMBER_CREATE_FAIL, e.getMessage());
            }
        }
        //????????????
        memberTmMemberMapper.insertMemberTmMember(MemberDomainUtil.convertToMemberDO(member));
        //????????????????????????????????????
        memberTmOperatorService.store(defaultOperator);
        //??????????????????
        if ((member.getAccounts() != null || member.getAccounts().size() > 0)) {
        	//???????????????????????????
        	int accountId = memberTrMemberAccountService.insertMemberTrMemberAccount(member.getBaseAccount());
        	ma.setAccountId(String.valueOf(accountId));
        }
        //????????????
        if(haveCompanyInfo){
            //??????????????????
           logger.info("??????????????????--->????????????:"+JSON.toJSONString(member));
        	memberTrCompanyMemberMapper.insertMemberTrCompanyMember(member);
        }
        //????????????
        if(null != merchant){
        	logger.info("??????????????????--->????????????:"+JSON.toJSONString(merchant));
            merchant.setMemberId(member.getMemberId());
            memberTmMerchantMapper.insertMemberTmMerchant(merchant);
            ma.setMerchantId(merchant.getMerchantId());
        }
        ma.setMemberId(member.getMemberId());
        ma.setOperatorId(defaultOperator.getOperatorId());
        return ma;
	}

	
	
}
