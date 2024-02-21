package nablarch.common.util;


import nablarch.core.message.ApplicationException;
import nablarch.core.message.Message;
import nablarch.core.validation.ValidationResultMessage;
import nablarch.core.validation.ee.MultiLanguageMessageInterpolator;
import nablarch.core.validation.ee.Required;
import nablarch.core.validation.ee.ValidatorFactoryBuilder;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.MockHttpRequest;
import nablarch.test.support.SystemRepositoryResource;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * {@link WebRequestUtil} のテスト。
 */
public class WebRequestUtilTest {

    @ClassRule
    public static SystemRepositoryResource resource = new SystemRepositoryResource(
            "nablarch/common/util/web-request-util.xml");
    /**
     * HTTPリクエストからBeanが生成され、バリデーションエラーが発生しないこと
     */
    @Test
    public void testGetValidatedBean_Success () {
        HttpRequest req = new MockHttpRequest()
                .setParam("id", "1")
                .setParam("name", "山田太郎");
        Person person = WebRequestUtil.getValidatedBean(Person.class, req);
        assertThat(person.getId(), is(1L));
        assertThat(person.getName(), is("山田太郎"));
    }

    /**
     * HTTPリクエストからBeanが生成され、バリデーションエラーが発生すること
     */
    @Test
    public void testGetValidatedBean_FailedValidation () {
        HttpRequest req = new MockHttpRequest().setParam("id", "1");
        try {
            WebRequestUtil.getValidatedBean(Person.class, req);
            Assert.fail("とおらない");
        } catch (ApplicationException e) {
            List<Message> messages = e.getMessages();
            assertThat(messages, hasSize(1));
            ValidationResultMessage message = (ValidationResultMessage) messages.get(0);
            assertThat(message.getPropertyName(), Matchers.is("name"));
            assertThat(message.formatMessage(), Matchers.is("必須の項目です"));
        }
    }

    /**
     * テスト用のデータクラス
     */
    public static class Person {

        private Long id;

        private String name;

        @Required
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Required
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    
    public static class CustomMessageInterpolator implements MessageInterpolator {

        private static final Map<String, String> messageTable = new HashMap<String, String>() {{
            put("{nablarch.core.validation.ee.Required.message}", "必須の項目です");
        }};

        @Override
        public String interpolate(String messageTemplate, Context context) {
            System.out.println(
                    "----------------------------------------------------------------------------------------------------");
            return interpolate(messageTemplate, context, Locale.getDefault());
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale locale) {
            System.out.println(
                    "----------------------------------------------------------------------------------------------------" + messageTemplate);
            return messageTable.get(messageTemplate);
        }
    }

    public static class CustomValidationFactoryBuilder extends ValidatorFactoryBuilder {
        @Override
        protected ValidatorFactory build() {
            return Validation.byDefaultProvider()
                    .configure()
                    .messageInterpolator(new MultiLanguageMessageInterpolator())
                    .buildValidatorFactory();
        }
    }
}
