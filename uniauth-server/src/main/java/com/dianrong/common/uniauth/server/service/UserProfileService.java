package com.dianrong.common.uniauth.server.service;

import com.dianrong.common.uniauth.common.bean.InfoName;
import com.dianrong.common.uniauth.common.bean.UserIdentityType;
import com.dianrong.common.uniauth.common.bean.dto.ProfileDefinitionDto;
import com.dianrong.common.uniauth.common.util.ObjectUtil;
import com.dianrong.common.uniauth.server.data.entity.ExtendVal;
import com.dianrong.common.uniauth.server.data.entity.User;
import com.dianrong.common.uniauth.server.exp.AppException;
import com.dianrong.common.uniauth.server.model.AttributeValModel;
import com.dianrong.common.uniauth.server.service.cache.ProfileCache;
import com.dianrong.common.uniauth.server.service.common.TenancyBasedService;
import com.dianrong.common.uniauth.server.service.inner.UserExtendValInnerService;
import com.dianrong.common.uniauth.server.service.inner.UserInnerService;
import com.dianrong.common.uniauth.server.service.inner.UserProfileInnerService;
import com.dianrong.common.uniauth.server.service.support.ProfileSupport;
import com.dianrong.common.uniauth.server.service.support.QueryProfileDefinition;
import com.dianrong.common.uniauth.server.util.CheckEmpty;
import com.dianrong.common.uniauth.server.util.UniBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户Profile操作的service实现.
 */
@Slf4j
@Service
public class UserProfileService extends TenancyBasedService {

  @Autowired
  private ProfileService profileService;

  @Autowired
  private UserExtendValInnerService userExtendValInnerService;

  @Autowired
  private ProfileCache profileCache;

  @Autowired
  private UserInnerService userInnerService;
  
  @Autowired
  private UserProfileInnerService userProfileInnerService;

  /**
   * 实现类似于getUserProfile.判定用户的身份是通过identity来判断.
   * 
   * @param identity 用户的身份识别标识. 比如:Email, Phone, Staff_no,Ldap_dn, User_guid等.
   * @param profileId Profile定义的Id.
   * @param identityType 登陆类型,对应于枚举:UserIdentityType
   * @return 用户的属性集合.
   */
  public Map<String, Object> getUserProfileByIdentity(String identity, Long profileId,
      UserIdentityType identityType) {
    CheckEmpty.checkEmpty(profileId, "profileId");
    User user = userInnerService.getUserByIdentity(identity, identityType);
    if (user == null) {
      throw new AppException(InfoName.BAD_REQUEST, UniBundle.getMsg(
          "common.profile.parameter.user.identity.not.found", identityType.getType(), identity));
    }
    Long uniauthId = user.getId();
    log.debug("find one user by {} = {}", identityType.getType(), identity);
    return getUserProfile(uniauthId, profileId);
  }

  /**
   * 根据UniauthId和profileId获取用户的属性.
   * 
   * @param uniauthId uniauthId
   * @param profileId profileId
   * @return 用户的属性集合
   */
  public Map<String, Object> getUserProfile(Long uniauthId, Long profileId) {
    CheckEmpty.checkEmpty(uniauthId, "uniauthId");
    CheckEmpty.checkEmpty(profileId, "profileId");
    ProfileDefinitionDto pdDto = profileService.getProfileDefinition(profileId);
    if (pdDto == null) {
      throw new AppException(InfoName.BAD_REQUEST,
          UniBundle.getMsg("common.entity.not.found", "profileId", profileId));
    }
    Set<Long> profileIds = ProfileSupport.collectSubProfileId(pdDto);
    if (ObjectUtil.collectionIsEmptyOrNull(profileIds)) {
      return Collections.emptyMap();
    }

    // 根据extend_attribute_id 获取所有的属性.
    Map<String, ExtendVal> extendValMap =
        userExtendValInnerService.queryAttributeVal(uniauthId, new ArrayList<>(profileIds));
    if (extendValMap.isEmpty()) {
      return Collections.emptyMap();
    }
    final Long tenancyId = tenancyService.getTenancyIdWithCheck();
    return ProfileSupport.getProfileAttributes(pdDto, extendValMap, new QueryProfileDefinition() {
      @Override
      public ProfileDefinitionDto querySimpleProfileDefinition(Long id) {
        return profileCache.getSimpleProfileDefinition(id, tenancyId);
      }
    });
  }

  /**
   * 更新用户的扩展属性.并根据ProfileId返回更新后的Profile.
   */
  public Map<String, Object> addOrUpdateUserProfile(Long uniauthId, Long profileId,
      Map<String, AttributeValModel> attributes) {
    userProfileInnerService.addOrUpdateUserProfile(uniauthId, attributes);
    return getUserProfile(uniauthId, profileId);
  }
}
