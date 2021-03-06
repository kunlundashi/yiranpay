/**
 *
 */
package com.yiranpay.member.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yiranpay.common.utils.DateUtils;
import com.yiranpay.member.constant.MaConstant;
import com.yiranpay.member.domain.AccountDomain;
import com.yiranpay.member.domain.CompanyMember;
import com.yiranpay.member.domain.MemberIdentity;
import com.yiranpay.member.domain.MemberIntegratedQuery;
import com.yiranpay.member.domain.MemberTmMember;
import com.yiranpay.member.domain.MemberTmMemberIdentity;
import com.yiranpay.member.domain.MemberTmOperator;
import com.yiranpay.member.domain.MemberTrCompanyMember;
import com.yiranpay.member.domain.MemberTrPersonalMember;
import com.yiranpay.member.domain.MemberTrVerifyEntity;
import com.yiranpay.member.domain.PersonalMember;
import com.yiranpay.member.domain.Verify;
import com.yiranpay.member.domain.VerifyInfo;
import com.yiranpay.member.enums.CareerEnum;
import com.yiranpay.member.enums.GenderEnum;
import com.yiranpay.member.enums.IdentityStatusEnum;
import com.yiranpay.member.enums.IdentityTypeEnum;
import com.yiranpay.member.enums.LockEnum;
import com.yiranpay.member.enums.MemberAccountStatusEnum;
import com.yiranpay.member.enums.MemberStatusEnum;
import com.yiranpay.member.enums.MemberTypeEnum;
import com.yiranpay.member.enums.PlatFormTypeEnum;
import com.yiranpay.member.enums.PositionEnum;
import com.yiranpay.member.exception.MaIllegalArgumentException;
import com.yiranpay.member.request.AccountQueryRequest;
import com.yiranpay.member.request.ActivatePersonalRequest;
import com.yiranpay.member.request.BaseMemberInfo;
import com.yiranpay.member.request.CreateMemberInfoRequest;
import com.yiranpay.member.request.IntegratedCompanyRequest;
import com.yiranpay.member.request.IntegratedPersonalRequest;
import com.yiranpay.member.request.MemberIntegratedIdRequest;
import com.yiranpay.member.request.MemberIntegratedRequest;
import com.yiranpay.member.request.PersonalMemberInfoRequest;
import com.yiranpay.member.request.PersonalMemberRequest;
import com.yiranpay.member.request.UpdateMemberLockStatusRequest;
import com.yiranpay.member.response.AccountInfo;
import com.yiranpay.member.response.IdentityInfo;
import com.yiranpay.member.response.MemberIntegratedResponse;

import cn.hutool.core.date.DateUtil;

/**
 * <p>???????????????????????????</p>
 */
public class MemberDomainUtil { 

    //******************************* ????????????????????????????????????????????????********************************//

	public static MemberTmMember convertReqToMember(CreateMemberInfoRequest request) {
		MemberTmMember member = new MemberTmMember();
        String loginName = StringUtils.trim(request.getLoginName()).toUpperCase();
        String identity = StringUtils.trim(request.getPlatformUserId()).toUpperCase();
        MemberTypeEnum memberTypeEnum = MemberTypeEnum.getByCode(request.getMemberType()
            .longValue());
        member.setMemberType(memberTypeEnum.getCode().intValue());
        member.setStatus(MemberStatusEnum.UNACTIVE.getCode().intValue());
        member.setLockStatus(LockEnum.UNLOCK.getCode().intValue());
        member.setMemberName(request.getMemberName());
        int pid = PlatFormTypeEnum.UID.getCode();
        if (StringUtils.isNotBlank(request.getPlatformType())) {
            pid = Integer.parseInt(StringUtils.trim(request.getPlatformType()));
        }

        //?????????????????????
        int lpid = PlatFormTypeEnum.DEFAULT.getCode();
        if (StringUtils.isNotBlank(request.getLoginNamePlatformType())) {
            lpid = Integer.parseInt(StringUtils.trim(request.getLoginNamePlatformType()));
        }

        MemberIdentity identity1 = new MemberIdentity();
        identity1.setIdentity(loginName);
        identity1.setIdentityType(request.getLoginNameType());
        identity1.setPlatFormType(lpid);
        identity1.setStatus(IdentityStatusEnum.VALID);
        member.addIdentity(identity1);

        if (StringUtils.isNotBlank(identity) && (!loginName.equals(identity) || lpid != pid)) {
            MemberIdentity identity2 = new MemberIdentity();
            identity2.setIdentity(identity);
            identity2.setIdentityType(IdentityTypeEnum.PUID.getCode());
            identity2.setPlatFormType(pid);
            identity2.setStatus(IdentityStatusEnum.VALID);
            member.addIdentity(identity2);
        }
        
        member.setRegisterSource(Integer.parseInt(request.getRegisterSource()));

        String registerSource = getResisterSource(request.getExtention());
        if(StringUtils.isNotEmpty(registerSource)) {
            member.setRegisterSource(Integer.parseInt(registerSource));
        }
        member.setMemo(request.getExtention());
        return member;

    }
	
	
	/**
     * ??????????????????
     * @param extension
     *          ????????????  
     * @return
     */
    public static String getResisterSource(String extension) {
        if(StringUtils.isNotEmpty(extension)) {
            JSONObject json = JSONObject.parseObject(extension);
            String registerSource = json.getString(MaConstant.REGISTER_SOURCE);
            return registerSource;
        } else {
            return null;
        }
    }
    
    
    /**
     * ???????????????--?????????-->DO ??????
     * @param member
     * @return
     */
    public static List<MemberTmMemberIdentity> convertToMemberIdentityDO(MemberTmMember member) {
        if (member == null || member.getIdentitys() == null || member.getIdentitys().isEmpty()) {
            return null;
        }
        List<MemberIdentity> identitys = member.getIdentitys();
        List<MemberTmMemberIdentity> result = new ArrayList<MemberTmMemberIdentity>(identitys.size());
        for (MemberIdentity identity : identitys) {
        	MemberTmMemberIdentity item = new MemberTmMemberIdentity();
            item.setCreateTime(identity.getCreateTime());
            item.setIdentity(StringUtils.lowerCase(identity.getIdentity()));
            item.setIdentityType(identity.getIdentityType());
            item.setIsRecvAddr(1);
            item.setMemberId(member.getMemberId());
            item.setMemo(identity.getMemo());
            item.setStatus(identity.getStatus().getCode());
            item.setUpdateTime(identity.getUpdateTime());
            item.setPid(identity.getPlatFormType());
            result.add(item);
        }

        return result;
    }
    
    /**
     * ??????????????????????????????--?????????-->????????????
     * @param request
     * @return
     */
    public static MemberIntegratedQuery convertReqToMemberIntegratedQuery(MemberIntegratedRequest request) {
        MemberIntegratedQuery query = new MemberIntegratedQuery();
        AccountQueryRequest accReq = request.getAccountRequest();
        if (accReq != null) {
            query.setAccountTypes(accReq.getAccountTypes());
            query.setRequireAccountInfos(accReq.isRequireAccountInfos());
        } else {
            query.setRequireAccountInfos(false);
        }

        query.setMemberIdentity(StringUtils.trim(request.getMemberIdentity()).toUpperCase());

        int pid = PlatFormTypeEnum.DEFAULT.getCode();
        if (StringUtils.isNotBlank(request.getPlatformType())) {
            pid = Integer.parseInt(StringUtils.trim(request.getPlatformType()));
        }
        query.setPlatformType(pid);
        query.setRequireVerifyInfos(request.isRequireVerifyInfos());
        query.setRequireDefaultOperator(request.isRequireDefaultOperator());
        return query;
    }
    
    /**
     * ??????????????????????????????--?????????-->????????????
     * @param request
     * @return
     */
    public static MemberIntegratedQuery convertReqToMemberIntegratedQuery(MemberIntegratedIdRequest request) {
        MemberIntegratedQuery query = new MemberIntegratedQuery();
        AccountQueryRequest accReq = request.getAccountRequest();
        if (accReq != null) {
            query.setAccountTypes(accReq.getAccountTypes());
            query.setRequireAccountInfos(accReq.isRequireAccountInfos());
        } else {
            query.setRequireAccountInfos(false);
        }
        query.setMemberId(request.getMemberId());
        query.setRequireVerifyInfos(request.isRequireVerifyInfos());
        query.setRequireDefaultOperator(request.isRequireDefaultOperator());
        return query;
    }
    
    /**
     * ??????????????????--?????????-->??????????????????????????????
     * @param member
     * @return
     */
    public static MemberIntegratedResponse convertToMemberIntegratedResponse(MemberTmMember member) {
        MemberIntegratedResponse response = new MemberIntegratedResponse();
        BaseMemberInfo baseMemberInfo = new BaseMemberInfo();
    	fillBaseMemberInfo(baseMemberInfo, member);
    	response.setBaseMemberInfo(baseMemberInfo);

        List<AccountDomain> accounts = member.getAccounts();
        if (!(accounts == null || accounts.isEmpty())) {
            List<AccountInfo> accountInfos = new ArrayList<AccountInfo>(accounts.size());
            for (AccountDomain item : accounts) {
                accountInfos.add(AccountDomainUtil.convertToAccountRef(item));
            }
            response.setAccountInfos(accountInfos);
        }

        List<MemberTrVerifyEntity> verifys = member.getVerifys();
        if (!(verifys == null || verifys.isEmpty())) {
            response.setVerifyInfos(verifys);
        }

        MemberTmOperator operator = member.getDefaultOperator();
        if (operator != null) {
            response.setDefaultOperator(operator);
        }

        return response;
    }
    
    public static void fillBaseMemberInfo(BaseMemberInfo info, MemberTmMember member) {
        info.setCreateTime(member.getCreateTime());
        Long lock = member.getLockStatus().longValue();
        info.setLockStatus(lock == null ? null : lock);
        info.setMemberId(member.getMemberId());
        info.setMemberName(member.getMemberName());
        info.setRegisterSource(String.valueOf(member.getRegisterSource()));

        Long statusEnum = member.getStatus().longValue();
        info.setStatus(statusEnum == null ? null : statusEnum);
        Long type = member.getMemberType().longValue();
        info.setMemberType(type == null ? null : type);
        info.setActiveTime(member.getActiveTime());

        //??????????????????
        Map<String,String> ext = new HashMap<String, String>();
        if(StringUtils.isNotBlank(member.getMemo())){
        	ext.put("memo", member.getMemo());
        }
        if(StringUtils.isNotBlank(member.getRegisterSourceExt())){
        	ext.put("registerSourceExt", member.getRegisterSourceExt());
        }
        info.setExtention(JSON.toJSONString(ext));
        
        List<MemberIdentity> identitys = member.getIdentitys();
        if (!(identitys == null || identitys.isEmpty())) {
            List<IdentityInfo> list = new ArrayList<IdentityInfo>(identitys.size());
            for (MemberIdentity identity : identitys) {
                IdentityInfo item = new IdentityInfo();
                item.setIdentity(StringUtils.lowerCase(identity.getIdentity()));
                item.setPlatformType(String.valueOf(identity.getPlatFormType()));
                item.setIdentityType(identity.getIdentityType());
                item.setIsUnionAccount(identity.getIsUnionAccount());
                item.setUnionAccountStatus(identity.getUnionAccountStatus());
                list.add(item);
            }
            //?????????????????????uid ??????????????????????????????????????????uid ????????????????????? ???
            /*//??????????????????????????????
            if (identitys.size() == 1 && member instanceof PersonalMember) {
                IdentityInfo identityInfo = list.get(0);
                if (identityInfo.getIdentityType() == IdentityTypeEnum.PUID.getCode()) {
                    IdentityInfo _item = new IdentityInfo();
                    _item.setIdentity(identityInfo.getIdentity());
                    _item.setPlatformType(identityInfo.getPlatformType());
                    _item.setIdentityType(IdentityTypeEnum.COMMON_CHAR.getCode());
                    list.add(_item);
                }
            }*/
            info.setIdentitys(list);
        }
    }
    
    /**
     * ????????????????????????????????????--?????????-->?????????????????????
     * @param request ????????????????????????????????????
     * @return ?????????????????????
     */
    public static PersonalMember convertReqToPersonalMember(IntegratedPersonalRequest request) {
    	PersonalMember pm = new PersonalMember();
        PersonalMemberRequest input = request.getPersonalRequest();
        String loginName = StringUtils.trim(input.getLoginName()).toUpperCase();
        String identity = StringUtils.trim(request.getPlatformUserId()).toUpperCase();
        pm.setMemberName(StringUtils.trim(input.getMemberName()));
        pm.setDefaultLoginName(loginName);
        pm.setMemberType(MemberTypeUtil.getPersonMemberType().getCode().intValue());
        if(MemberAccountStatusEnum.getByCode(request.getMemberAccountFlag()) == MemberAccountStatusEnum.ACTIVATED_ALL ||
                MemberAccountStatusEnum.getByCode(request.getMemberAccountFlag()) == MemberAccountStatusEnum.ACTIVATED ) {
            pm.setStatus(MemberStatusEnum.NORMAL.getCode().intValue());
        } else {
            pm.setStatus(MemberStatusEnum.UNACTIVE.getCode().intValue());
        }
        pm.setLockStatus(LockEnum.UNLOCK.getCode().intValue());
        pm.setTrueName(input.getRealName());
        pm.setBirthDay(input.getBirthDay());
        if (input.getCareer() == null) {
            pm.setCareer(CareerEnum.DEFAULT);
        } else {
            CareerEnum career = CareerEnum.getByCode(input.getCareer());
            if (career == null) {
                throw new MaIllegalArgumentException("?????????????????????:" + input.getCareer());
            }
            pm.setCareer(career);
        }
        if (input.getGender() == null) {
            pm.setGender(GenderEnum.UNKOWN);
        } else {
            GenderEnum gender = GenderEnum.getByCode(input.getGender());
            if (gender == null) {
                throw new MaIllegalArgumentException("?????????????????????:" + input.getGender());
            }
            pm.setGender(gender);
        }
        if (input.getPosition() == null) {
            pm.setPosition(PositionEnum.DEFAULT);
        } else {
            PositionEnum position = PositionEnum.getByCode(input.getPosition());
            if (position == null) {
                throw new MaIllegalArgumentException("?????????????????????:" + input.getPosition());
            }
            pm.setPosition(position);
        }
        //uid ????????????
        int pid = PlatFormTypeEnum.DEFAULT.getCode();
        if (StringUtils.isNotBlank(request.getPlatformType())) {
            pid = Integer.parseInt(StringUtils.trim(request.getPlatformType()));
        }
        //?????????????????????
        int lpid = PlatFormTypeEnum.DEFAULT.getCode();
        if (StringUtils.isNotBlank(input.getLoginNamePlatformType())) {
            lpid = Integer.parseInt(StringUtils.trim(input.getLoginNamePlatformType()));
        }

        //????????????uid
        MemberIdentity uidIdentity = new MemberIdentity();
        if (StringUtils.isNotEmpty(identity)) {
            uidIdentity.setIdentity(identity);
            uidIdentity.setIdentityType(IdentityTypeEnum.PUID.getCode());
            uidIdentity.setPlatFormType(pid);
            uidIdentity.setStatus(IdentityStatusEnum.VALID);
            pm.addIdentity(uidIdentity);
        }

        if (!loginName.equals(identity) || lpid != pid) {
            MemberIdentity loginIdentity = new MemberIdentity();
            loginIdentity.setIdentity(loginName);
            loginIdentity.setIdentityType(input.getLoginNameType());
            loginIdentity.setPlatFormType(lpid);
            loginIdentity.setStatus(IdentityStatusEnum.VALID);
            pm.addIdentity(loginIdentity);
        }

        List<VerifyInfo> verifyInfos = request.getVerifys();
        if (!(verifyInfos == null || verifyInfos.isEmpty())) {
            List<MemberTrVerifyEntity> verifys = new ArrayList<MemberTrVerifyEntity>(verifyInfos.size());
            for (VerifyInfo item : verifyInfos) {
                verifys.add(VerifyDomainUtil.converReqToMemberTrVerifyEntity(item));
            }
            pm.setVerifys(verifys);
        }
        
        String registerSource = getResisterSource(request.getExtention());
        
        if (StringUtils.isNotBlank(request.getRegisterSource()))
    		pm.setRegisterSource(Integer.parseInt(request.getRegisterSource()));
        if(StringUtils.isNotEmpty(registerSource)) {
            pm.setRegisterSource(Integer.parseInt(registerSource));
        }
        pm.setMemo(input.getExtention());
        pm.setInvitCode(request.getInvitCode());
        pm.setRegisterSourceExt(request.getRegisterSourceExt());
        return pm;
    }
    //******************************????????????????????????????????????????????????**************************************************//


	public static MemberTmMember convertToMemberDO(PersonalMember member) {
		MemberTmMember tmMember = new MemberTmMember();
		tmMember.setMemberId(member.getMemberId());
        tmMember.setMemberName(member.getMemberName());
        tmMember.setMemberShortName(member.getMemberShortName());
        if(null!=member.getMemberType()){
        	tmMember.setMemberType(member.getMemberType());
        }
        if(null!=member.getStatus()){
        	tmMember.setStatus(member.getStatus());
        }
        if(null!=member.getLockStatus()){
        	tmMember.setLockStatus(member.getLockStatus());
        }
        tmMember.setFromIp(member.getFromIp());
        tmMember.setCreateUser(member.getCreateUser());
        if(null != member.getRegisterSource()) {
            tmMember.setRegisterSource(member.getRegisterSource());
        }
        if (member.getVerifyLevel() != null)
        	tmMember.setVerifyLevel(member.getVerifyLevel());
        tmMember.setMemo(member.getMemo());
        tmMember.setInvitCode(member.getInvitCode());
        tmMember.setRegisterSourceExt(member.getRegisterSourceExt());
		return tmMember;
	}

	public static MemberTmMember convertToMemberDO(MemberTrCompanyMember member) {
		MemberTmMember tmMember = new MemberTmMember();
		tmMember.setMemberId(member.getMemberId());
        tmMember.setMemberName(member.getMemberName());
        tmMember.setMemberShortName(member.getMemberShortName());
        if(null!=member.getMemberType()){
        	tmMember.setMemberType(member.getMemberType());
        }
        if(null!=member.getStatus()){
        	tmMember.setStatus(member.getStatus());
        }
        if(null!=member.getLockStatus()){
        	tmMember.setLockStatus(member.getLockStatus());
        }
        tmMember.setFromIp(member.getFromIp());
        tmMember.setCreateUser(member.getCreateUser());
        if(null != member.getRegisterSource()) {
            tmMember.setRegisterSource(member.getRegisterSource());
        }
        if (member.getVerifyLevel() != null)
        	tmMember.setVerifyLevel(member.getVerifyLevel());
        tmMember.setMemo(member.getMemo());
        tmMember.setInvitCode(member.getInvitCode());
        tmMember.setRegisterSourceExt(member.getRegisterSourceExt());
		return tmMember;
	}

	public static MemberTrPersonalMember convertToPersonalMemberDO(PersonalMember pm) {

		MemberTrPersonalMember trPersonalMember = new MemberTrPersonalMember();
        trPersonalMember.setMemberId(pm.getMemberId());
        trPersonalMember.setBirthday(pm.getBirthDay());
        trPersonalMember.setCareer(pm.getCareer().getCode().intValue());
        //trPersonalMember.setCertType(null);
        trPersonalMember.setDefaultLoginName(pm.getDefaultLoginName());
        trPersonalMember.setGender(pm.getGender().getCode().intValue());
        //trPersonalMember.setIdNo();
        trPersonalMember.setPostition(pm.getPosition().getCode().intValue());
        trPersonalMember.setTrueName(pm.getTrueName());
        trPersonalMember.setCreateUser(pm.getCreateUser());
		return trPersonalMember;
	}
	
	

    /**
     * ??????????????????????????????--?????????-->?????????????????????
     * @param request ??????????????????????????????
     * @return ?????????????????????
     */
    public static PersonalMember convertReqToPersonalMember(PersonalMemberInfoRequest request) {
        PersonalMember member = new PersonalMember();

        member.setMemberId(request.getMemberId());
        member.setMemberName(StringUtils.trim(request.getMemberName()));
        member.setMemberType(MemberTypeUtil.getPersonMemberType().getCode().intValue());
        member.setStatus(MemberStatusEnum.UNACTIVE.getCode().intValue());
        member.setTrueName(request.getRealName());
        member.setBirthDay(request.getBirthDay());
        if (request.getCareer() == null) {
            member.setCareer(CareerEnum.DEFAULT);
        } else {
            CareerEnum career = CareerEnum.getByCode(request.getCareer());
            if (career == null) {
                throw new MaIllegalArgumentException("?????????????????????:" + request.getCareer());
            }
            member.setCareer(career);
        }
        if (request.getGender() == null) {
            member.setGender(GenderEnum.UNKOWN);
        } else {
            GenderEnum gender = GenderEnum.getByCode(request.getGender());
            if (gender == null) {
                throw new MaIllegalArgumentException("?????????????????????:" + request.getGender());
            }
            member.setGender(gender);
        }
        if (request.getPosition() == null) {
            member.setPosition(PositionEnum.DEFAULT);
        } else {
            PositionEnum position = PositionEnum.getByCode(request.getPosition());
            if (position == null) {
                throw new MaIllegalArgumentException("?????????????????????:" + request.getPosition());
            }
            member.setPosition(position);
        } 
        member.setInvitCode(request.getInvitCode());
        return member;
    }
    
    /**
     * ??????????????????????????????--?????????-->?????????????????????
     * @param request ??????????????????????????????
     * @return ?????????????????????
     */
    public static PersonalMember convertReqToPersonalMember(ActivatePersonalRequest request) {
        PersonalMemberInfoRequest req = request.getPersonalMemberInfo();
        return convertReqToPersonalMember(req);
    }
    
    /**
     * ??????????????????????????????---?????????--->????????????
     * @param request
     * @return
     */
    public static MemberTmMember convertReqToMember(UpdateMemberLockStatusRequest request) {
    	MemberTmMember member = new MemberTmMember();
        member.setMemberId(request.getMemberId());
        member.setLockStatus(request.getLockStatus().intValue());
        return member;
    }
    
    /**
     * ????????????????????????--?????????-->???????????????
     * @param request ????????????????????????
     * @return ???????????????
     */
    public static MemberTrCompanyMember convertReqToMember(IntegratedCompanyRequest request) {
    	MemberTrCompanyMember member = new MemberTrCompanyMember();
        String loginName = StringUtils.trim(request.getLoginName()).toLowerCase();
        String identity = StringUtils.trim(request.getPlatformUserId()).toLowerCase();
        MemberTypeEnum memberTypeEnum = MemberTypeEnum.getByCode(request.getMemberType()
            .longValue());
        member.setMemberType(request.getMemberType().intValue());
        member.setStatus(MemberStatusEnum.UNACTIVE.getCode().intValue());
        member.setLockStatus(LockEnum.UNLOCK.getCode().intValue());
        member.setMemberName(request.getMemberName());
        int pid = PlatFormTypeEnum.UID.getCode();
        if (StringUtils.isNotBlank(request.getPlatformType())) {
            pid = Integer.parseInt(StringUtils.trim(request.getPlatformType()));
        }

        //?????????????????????
        int lpid = PlatFormTypeEnum.DEFAULT.getCode();
        if (StringUtils.isNotBlank(request.getLoginNamePlatformType())) {
            lpid = Integer.parseInt(StringUtils.trim(request.getLoginNamePlatformType()));
        }

        MemberIdentity identity1 = new MemberIdentity();
        identity1.setIdentity(loginName);
        identity1.setIdentityType(request.getLoginNameType());
        identity1.setPlatFormType(lpid);
        identity1.setStatus(IdentityStatusEnum.VALID);
        member.addIdentity(identity1);

        if (StringUtils.isNotBlank(identity) && (!loginName.equals(identity) || lpid != pid)) {
            MemberIdentity identity2 = new MemberIdentity();
            identity2.setIdentity(identity);
            identity2.setIdentityType(IdentityTypeEnum.PUID.getCode());
            identity2.setPlatFormType(pid);
            identity2.setStatus(IdentityStatusEnum.VALID);
            member.addIdentity(identity2);
        }
        
        String registerSource = getResisterSource(request.getExtention());
        if(StringUtils.isNotEmpty(registerSource)) {
            member.setRegisterSource(Integer.parseInt(registerSource));
        }
        return member;
    }


	public static MemberTrCompanyMember convertReqToCompayMember(CompanyMember companyMember) {
		MemberTrCompanyMember member = new MemberTrCompanyMember();
        String loginName = StringUtils.trim(companyMember.getLoginName()).toLowerCase();
        String identity = StringUtils.trim(companyMember.getPlatformUserId()).toLowerCase();
        MemberTypeEnum memberTypeEnum = MemberTypeEnum.getByCode(Long.parseLong(companyMember.getMemberType()));
        member.setMemberType(Integer.parseInt(companyMember.getMemberType()));
        member.setStatus(MemberStatusEnum.UNACTIVE.getCode().intValue());
        member.setLockStatus(LockEnum.UNLOCK.getCode().intValue());
        member.setMemberName(companyMember.getMemberName());
        int pid = PlatFormTypeEnum.UID.getCode();
        if (StringUtils.isNotBlank(companyMember.getPlatformType())) {
            pid = Integer.parseInt(StringUtils.trim(companyMember.getPlatformType()));
        }
        
        //????????????
        //??????????????????
        member.setLicenseNo(companyMember.getLicenseNo());
        //??????????????????????????????(????????????)
        member.setLicenseExpireDate(DateUtils.dateTime("yyyy-MM-dd HH:mm:ss", companyMember.getLicenseExpireDate()));
        //????????????
        member.setCompanyNo(companyMember.getCompanyNo());
        //????????????
        member.setLegalPerson(companyMember.getLegalPerson());
        //????????????
        member.setScale(Integer.parseInt(companyMember.getScale()));
        //????????????
        member.setWebsite(companyMember.getWebsite());
        //????????????
        member.setCompanyName(companyMember.getCompanyName());
        //????????????
        member.setAddress(companyMember.getAddress());
        //?????????????????????
        member.setLicenseAddress(companyMember.getLicenseAddress());
        //????????????
        member.setBusinessScope(companyMember.getBusinessScope());
        //????????????
        member.setSummary(companyMember.getSummary());
        //??????????????????
        member.setLegalPersonPhone(companyMember.getLegalPersonPhone());
        //????????????
        member.setLicenseName(companyMember.getLicenseName());
        //????????????????????????
        member.setLegalPersonIdValidDate(DateUtils.dateTime("yyyy-MM-dd", companyMember.getLegalPersonIdValidDate()));
        //????????????
        member.setCompanyType(Integer.parseInt(companyMember.getCompanyType()));
        //????????????
        member.setShortName(companyMember.getShortName());
        
        //?????????????????????
        int lpid = PlatFormTypeEnum.DEFAULT.getCode();
        if (StringUtils.isNotBlank(companyMember.getLoginNamePlatformType())) {
            lpid = Integer.parseInt(StringUtils.trim(companyMember.getLoginNamePlatformType()));
        }

        MemberIdentity identity1 = new MemberIdentity();
        identity1.setIdentity(loginName);
        identity1.setIdentityType(Integer.parseInt(companyMember.getLoginNameType()));
        identity1.setPlatFormType(lpid);
        identity1.setStatus(IdentityStatusEnum.VALID);
        member.addIdentity(identity1);

        if (StringUtils.isNotBlank(identity) && (!loginName.equals(identity) || lpid != pid)) {
            MemberIdentity identity2 = new MemberIdentity();
            identity2.setIdentity(identity);
            identity2.setIdentityType(IdentityTypeEnum.PUID.getCode());
            identity2.setPlatFormType(pid);
            identity2.setStatus(IdentityStatusEnum.VALID);
            member.addIdentity(identity2);
        }
        
        String registerSource = getResisterSource(companyMember.getExtention());
        if(StringUtils.isNotEmpty(registerSource)) {
            member.setRegisterSource(Integer.parseInt(registerSource));
        }
        return member;
	}

    

}
