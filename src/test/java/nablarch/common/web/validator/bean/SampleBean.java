package nablarch.common.web.validator.bean;

import java.io.Serializable;

import jakarta.validation.constraints.AssertTrue;

import nablarch.core.util.StringUtil;
import nablarch.core.validation.ee.Domain;
import nablarch.core.validation.ee.Length;
import nablarch.core.validation.ee.SystemChar;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;

/**
 * Bean Validationのテストで使用するクラス.
 */
public class SampleBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Domain("demoDomain")
    private String userId;

    @SystemChar(charsetDef = "英大文字")
    @Length(min = 5, max = 10)
    private String userName;

    @SystemChar(charsetDef = "英大文字")
    @Length(min = 5, max = 7)
    private String customizedGroupItem;

    @SystemChar(charsetDef = "数字")
    @Length(max = 5)
    private String correlationCheckItem1;
    
    @SystemChar(charsetDef = "数字")
    @Length(max = 4)
    private String correlationCheckItem2;

    @SystemChar(charsetDef = "英大文字", groups = Test1.class)
    @Length(max = 4, min = 4, groups = Test2.class)
    private String validationGroupCheckItem;

    public interface Test1 {}
    public interface Test2 {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCustomizedGroupItem() {
        return customizedGroupItem;
    }

    public void setCustomizedGroupItem(String customizedGroupItem) {
        this.customizedGroupItem = customizedGroupItem;
    }

    public String getCorrelationCheckItem1() {
        return correlationCheckItem1;
    }

    public void setCorrelationCheckItem1(String correlationCheckItem1) {
        this.correlationCheckItem1 = correlationCheckItem1;
    }

    public String getCorrelationCheckItem2() {
        return correlationCheckItem2;
    }

    public void setCorrelationCheckItem2(String correlationCheckItem2) {
        this.correlationCheckItem2 = correlationCheckItem2;
    }

    public String getValidationGroupCheckItem() {
        return validationGroupCheckItem;
    }

    public void setValidationGroupCheckItem(String validationGroupCheckItem) {
        this.validationGroupCheckItem = validationGroupCheckItem;
    }

    public void setInitValues(HttpRequest request, ExecutionContext context) {
        this.userId = "0000000001";
        this.userName = "XXXXX";
    }

// TODO ネストした構成のBeanにおける検証
//  @Valid
//  private SampleSubBean subBeans;
//
//  public SampleSubBean getSubBeans() {
//      return subBeans;
//  }
//
//  public void setSubBeans(SampleSubBean subBeans) {
//      this.subBeans = subBeans;
//  }

    @AssertTrue(message = "項目間バリデーションエラーです。")
    public boolean isCorrelationCheckValid() {
        return (StringUtil.isNullOrEmpty(correlationCheckItem1, correlationCheckItem2)
                || (StringUtil.hasValue(correlationCheckItem1) && StringUtil.hasValue(correlationCheckItem2)));
    }

}
