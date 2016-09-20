package nablarch.common.web.interceptor.sample.form;

import java.io.Serializable;
import java.util.Map;

import nablarch.core.validation.PropertyName;
import nablarch.core.validation.ValidateFor;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationUtil;
import nablarch.core.validation.validator.Required;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;

public class SampleForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId; // @Required

    private String name;

    public SampleForm() {
    }

    public SampleForm(Map<String, Object> params) {
        userId = (String) params.get("userId");
    }

    public String getUserId() {
        return userId;
    }

    @PropertyName("ユーザID")
    @Domain(DomainType.USER_ID)
    @Required
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ValidateFor("test")
    public static void validateForTest(ValidationContext<SampleForm> context) {
        ValidationUtil.validateWithout(context, new String[] {"name"});
    }

    public void setInitValues(HttpRequest request, ExecutionContext context) {
        setUserId("9999999999");
        setName("no-validation-property");
    }
}
