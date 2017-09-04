package com.dianrong.common.uniauth.server.synchronous.hr.support;

import com.dianrong.common.uniauth.server.data.entity.HrLe;
import com.dianrong.common.uniauth.server.synchronous.exp.InvalidContentException;
import com.dianrong.common.uniauth.server.synchronous.hr.bean.LegalEntityList;
import com.dianrong.common.uniauth.server.synchronous.support.AbstractFileContentAnalyzer;
import org.springframework.util.StringUtils;

import java.util.List;

public class HrLeAnalyzer extends AbstractFileContentAnalyzer<LegalEntityList> {

  public static final int ITEM_LENGTH = 5;

  @Override
  public LegalEntityList analyze(String content) throws InvalidContentException {
    LegalEntityList result = new LegalEntityList();
    if (!StringUtils.hasText(content)) {
      return result;
    }

    // 实际解析过程
    List<String> strList = anaToList(content);
    for(int i =1; i < strList.size(); i++) {
      String recordStr = strList.get(i);
      String[] items = recordStr.split(rowDelimiter);
      itemLengthCheck(items, ITEM_LENGTH);
      HrLe hrLe = new HrLe();
      hrLe.setLeId(strToLong(items[0]));
      hrLe.setLeCode(items[1]);
      hrLe.setLeName(items[2]);
      hrLe.setEffectiveStartDate(strToDate(items[3]));
      hrLe.setActiveStatus(items[4]);
      result.add(hrLe);
    }
    return result;
  }
}
