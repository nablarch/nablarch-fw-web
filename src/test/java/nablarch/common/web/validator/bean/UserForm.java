package nablarch.common.web.validator.bean;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import nablarch.core.util.StringUtil;
import nablarch.core.validation.ee.Required;

public class UserForm implements Serializable {
    
    @Required
    private String name;

    @Required
    private String birthday;

    @Required
    private String age;
    
    @Valid
    private UserSubForm sub = new UserSubForm();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(final String birthday) {
        this.birthday = birthday;
    }

    public String getAge() {
        return age;
    }

    public void setAge(final String age) {
        this.age = age;
    }

    public UserSubForm getSub() {
        return sub;
    }

    public void setSub(final UserSubForm sub) {
        this.sub = sub;
    }

    public static class UserSubForm implements Serializable {
        private String sub1;
        private String sub2;
        @Required
        private String sub3;

        public String getSub1() {
            return sub1;
        }

        public void setSub1(final String sub1) {
            this.sub1 = sub1;
        }

        public String getSub2() {
            return sub2;
        }

        public void setSub2(final String sub2) {
            this.sub2 = sub2;
        }

        @AssertTrue(message = "項目間のバリデーションエラー")
        public boolean isMultiItemValidation() {
            return !(StringUtil.isNullOrEmpty(sub1) && StringUtil.isNullOrEmpty(sub2));
        }
    }
    
}
