package nablarch.common.web.validator;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.hamcrest.collection.IsIterableWithSize;

import nablarch.common.web.interceptor.InjectForm;
import nablarch.common.web.validator.bean.SampleBean;
import nablarch.common.web.validator.bean.UserForm;
import nablarch.common.web.validator.bean.WithArrayBean;
import nablarch.core.ThreadContext;
import nablarch.core.message.ApplicationException;
import nablarch.core.message.Message;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.validation.ValidationResultMessage;
import nablarch.core.validation.ee.ItemNamedConstraintViolationConverterFactory;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpMethodBinding;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.NormalizationHandler;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import mockit.Expectations;
import mockit.Mocked;

/**
 * {@link InjectForm}のテスト(Bean Validationによるバリデーション).
 *
 * @author sumida
 */
public class BeanValidationStrategyTest {

    @Rule
    public SystemRepositoryResource repositoryResource
            = new SystemRepositoryResource("nablarch/common/web/validator/inject-bean-test.xml");

    @Mocked
    private HttpServletRequest mockHttpServletRequest;

    @Mocked
    private HttpServletResponse mockHttpServletResponse;

    @Mocked
    private ServletContext mockServletContext;

    private ServletExecutionContext context;

    @Before
    public void setUp() throws Exception {
        ThreadContext.setLanguage(Locale.JAPANESE);

        new Expectations() {{
            mockHttpServletRequest.getContextPath();
            result = "";
            minTimes = 0;

            mockHttpServletRequest.getRequestURI();
            result = "/dummy";
            minTimes = 0;

        }};

        context = new ServletExecutionContext(mockHttpServletRequest,
                mockHttpServletResponse, mockServletContext);

        new Expectations(context) {{
            Map<String, Object> requestScope = new HashMap<String, Object>();
            context.getRequestScopeMap();
            result = requestScope;
            minTimes = 0;
        }};
    }

    /**
     * [Bean Validation] プレフィックス指定あり、初期化メソッド指定なし、グループ指定なし、バリデーションOKの場合、
     * Beanが生成され、リクエストスコープに設定される。
     * グループ指定なしの場合、Test1グループ、Test2グループは検証されない。
     */
    @Test
    public void testValidateWithValidParametersUsingBeanValidator() {

        Object action = new Object() {
            @InjectForm(form = SampleBean.class, prefix = "sample")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleBean bean = ctx.getRequestScopedVar("form");
                assertNotNull(bean);
                assertThat(bean.getUserId(), is("1234567"));
                assertThat(bean.getUserName(), is("ABCDEFG"));
                assertThat(bean.getValidationGroupCheckItem(), is("0123456789"));
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                .setParam("sample.userId", "1234567")
                .setParam("sample.userName", "ABCDEFG")
                .setParam("sample.validationGroupCheckItem", "0123456789")
                .setParam("correlationCheckItem2", "12345")     // prefixがついていないのでコピー対象外
        );
    }

    /**
     * [Bean Validation] プレフィックス指定あり、初期化メソッド指定なし、グループ指定あり、バリデーションOKの場合、
     * Beanが生成され、リクエストスコープに設定される。
     * グループは、Test1のみ検証され、Test2は検証されない。
     */
    @Test
    public void testValidateByGroupWithValidParametersUsingBeanValidator() {
        Object action = new Object() {
            @InjectForm(form = SampleBean.class, prefix = "sample", validationGroup = SampleBean.Test1.class)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleBean bean = ctx.getRequestScopedVar("form");
                assertNotNull(bean);
                assertThat(bean.getValidationGroupCheckItem(), is("ABCDEFGHIJKLMN"));
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                .setParam("sample.validationGroupCheckItem", "ABCDEFGHIJKLMN")
        );
    }

    /**
     * [Bean Validation] プレフィックス指定あり、初期化メソッド指定なし、グループ指定あり、バリデーションNGの場合、
     * Beanが生成され、リクエストスコープに設定される。
     * グループは、Test2のみ検証され、Test1は検証されない。
     */
    @Test
    public void testValidateByGroupWithInValidParametersUsingBeanValidator() {
        Object action = new Object() {
            @InjectForm(form = SampleBean.class, prefix = "sample", validationGroup = SampleBean.Test2.class)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        try {
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                    .setParam("sample.validationGroupCheckItem", "ABCDEFGHIJKLMN")
            );
            fail("must be thrown ApplicationException");
        } catch (ApplicationException e) {
            assertThat(e.getMessages()
                        .size(), is(1));
            assertThat(e.getMessages()
                        .get(0)
                        .formatMessage(), is("4文字で入力してください。"));
            // バリデーションエラー時、リクエストスコープにBeanが設定されること
            // (InjectFormのnameが指定されていないので、キーはデフォルトの"form")
            SampleBean sampleBean = context.getRequestScopedVar("form");
            assertThat(sampleBean.getValidationGroupCheckItem(), is("ABCDEFGHIJKLMN"));
        }

    }

    /**
     * [Bean Validation] プレフィックス指定あり、初期化メソッド指定なし、グループ複数指定、バリデーションOKの場合、
     * Beanが生成され、リクエストスコープに設定される。
     * グループは、Test1とTest2両方とも検証される。
     */
    @Test
    public void testValidateByMultiGroupWithValidParametersUsingBeanValidator() {
        Object action = new Object() {
            @InjectForm(form = SampleBean.class, prefix = "sample", validationGroup = {SampleBean.Test1.class, SampleBean.Test2.class})
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleBean bean = ctx.getRequestScopedVar("form");
                assertNotNull(bean);
                assertThat(bean.getValidationGroupCheckItem(), is("ABCD"));
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                .setParam("sample.validationGroupCheckItem", "ABCD")
        );
    }

    /**
     * [Bean Validation] プレフィックス指定なし、初期化メソッド指定なし、バリデーションOKの場合、
     * Beanが生成され、リクエストスコープに設定される。(項目名にもprefixがない場合)
     */
    @Test
    public void testValidateWithValidNoPrefixParametersUsingBeanValidator() {

        Object action = new Object() {
            @InjectForm(form = SampleBean.class)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleBean bean = ctx.getRequestScopedVar("form");
                assertNotNull(bean);
                assertThat(bean.getUserId(), is("1234567"));
                assertThat(bean.getUserName(), is("ABCDEFG"));
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                .setParam("userId", "1234567")
                .setParam("userName", "ABCDEFG"));
    }

    /**
     * [Bean Validation] プレフィックス指定あり、初期化メソッド指定あり、バリデーションOKの場合、
     * 初期化メソッドで値が設定されても、バリデーション済の値があれば、上書きコピーされた状態の
     * Beanがリクエストスコープに設定される。
     */
    @Test
    public void testInitializeAndValidateWithValidParametersUsingBeanValidator() {

        Object action = new Object() {
            @InjectForm(form = SampleBean.class, prefix = "sample", initialize = "setInitValues")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleBean bean = ctx.getRequestScopedVar("form");
                assertNotNull(bean);
                assertThat(bean.getUserId(), is("1234567"));   // override "0000000001"
                assertThat(bean.getUserName(), is("ABCDEFG")); // override "XXXXX"
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                .setParam("sample.userId", "1234567")
                .setParam("sample.userName", "ABCDEFG"));
    }

    /**
     * [Bean Validation] プレフィックス指定があるものの、パラメータのプレフィクスと一致しない場合、
     * Beanは生成されるが、値は設定されない。
     */
    @Test
    public void testValidateWithInValidParameterNamesUsingBeanValidator() {

        Object action = new Object() {
            @InjectForm(form = SampleBean.class, prefix = "xxx")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleBean bean = ctx.getRequestScopedVar("form");
                assertNotNull(bean);
                assertNull(bean.getUserId());
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                .setParam("sample.userId", "1234567"));
    }

    /**
     * [Bean Validation] プレフィックス指定あり、初期化メソッド指定なし、バリデーションＮＧの場合、
     * エラーメッセージが設定されたアプリ例外が送出される。
     */
    @Test
    public void testValidateWithInValidParametersUsingBeanValidator() {

        Object action = new Object() {
            @InjectForm(form = SampleBean.class, prefix = "sample")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        try {
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                    .setParam("sample.userId", "abcdef"));
            fail("must be thrown ApplicationException");
        } catch (ApplicationException e) {
            assertThat(e.getMessages()
                        .size(), is(1));
            assertThat(e.getMessages()
                        .get(0)
                        .formatMessage(), is("数字でないですよ。"));
            // バリデーションエラー時、リクエストスコープにBeanが設定されること
            // (InjectFormのnameが指定されていないので、キーはデフォルトの"form")
            SampleBean sampleBean = context.getRequestScopedVar("form");
            assertThat(sampleBean.getUserId(), is("abcdef"));
        }
    }

    /**
     * [Bean Validation] プレフィックス指定あり(空文字列を指定)、初期化メソッド指定なし、バリデーションＮＧの場合、
     * エラーメッセージ及びプレフィックスの付かないプロパティ名が設定されたアプリ例外が送出される。
     */
    @Test
    public void testValidateWithInValidParametersAndUnusedPrefixUsingBeanValidator() {

        Object action = new Object() {
            @InjectForm(form = SampleBean.class)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        try {
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                    .setParam("userId", "abcdef"));
            fail("must be thrown ApplicationException");
        } catch (ApplicationException e) {
            assertThat(e, hasProperty(
                    "messages", hasItems(hasProperty("propertyName", is("userId")))));

            // バリデーションエラー時、リクエストスコープにBeanが設定されること
            SampleBean sampleBean = context.getRequestScopedVar("form");
            assertThat(sampleBean.getUserId(), is("abcdef"));
        }
    }

    /**
     * [Bean Validation] プレフィックス指定あり、初期化メソッド指定なし、バリデーションＮＧの場合、
     * エラーメッセージが設定されたアプリ例外が送出される。
     * デフォルトロケールを使用する。
     */
    @Test
    public void testValidateWithInValidParametersUsingBeanValidatorWithDefaultLocale() {
        ThreadContext.setLanguage(null); // ThreadContextの言語をnullにする。
        testValidateWithInValidParametersUsingBeanValidator();
        ThreadContext.setLanguage(Locale.getDefault()); // 元に戻す。
    }

    /**
     * [Bean Validation] リクエストパラメータの値がなく、初期化メソッドも指定されなかった場合、
     * 各プロパティの値がnullのBeanが生成され、リクエストスコープに設定される。
     */
    @Test
    public void testNoInitializeAndNoValidation() {
        Object action = new Object() {
            @InjectForm(form = SampleBean.class)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleBean form = ctx.getRequestScopedVar("form");
                assertNotNull(form);
                assertNull(form.getUserId());
                assertNull(form.getUserName());
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1"));
    }
    
    /**
     * メッセージが、{@link ServletRequest#getParameterNames()}の順にソートされること。
     *
     * @throws Exception
     */
    @Test
    public void testSortMessages() throws Exception {
        new Expectations() {{
            final List<String> paramNames = new ArrayList<String>();
            paramNames.add("form.name");
            paramNames.add("form.sub.sub3");
            paramNames.add("form.birthday");
            paramNames.add("form.age");
            mockHttpServletRequest.getParameterNames();
            result = Collections.enumeration(paramNames);
            minTimes = 0;
        }};

        final Object action = new Object() {
            @InjectForm(form = UserForm.class, prefix = "form")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext context) {
                return new HttpResponse();
            }
        };

        context.addHandler("//", new HttpMethodBinding(action));

        final MockHttpRequest request = new MockHttpRequest("GET /index.html HTTP/1.1");
        request.setParam("form.age", "10");
        request.setParam("form.name", "");
        request.setParam("form.birthday", "");
        request.setParam("form.sub.sub3", "");

        try {
            context.handleNext(request);
            fail();
        } catch (ApplicationException e) {
            final List<Message> messages = e.getMessages();
            assertThat(messages, IsIterableWithSize.<Message>iterableWithSize(4));
            assertThat(messages, IsIterableContainingInOrder.contains(
                    MessageMatcher.is("必須項目です。", "form.name"),
                    MessageMatcher.is("必須項目です。", "form.sub.sub3"),
                    MessageMatcher.is("必須項目です。", "form.birthday"),
                    MessageMatcher.is("項目間のバリデーションエラー", "form.sub.multiItemValidation")
            ));

            // バリデーションエラー時、リクエストスコープにBeanが設定されること
            UserForm userForm = context.getRequestScopedVar("form");
            assertThat(userForm.getAge(), is("10"));
            assertThat(userForm.getBirthday(), is(""));
            assertThat(userForm.getSub().getSub3(), is(""));
        }
    }

    /**
     * メッセージが、{@link ServletRequest#getParameterNames()}の順にソートされること。
     * 項目名を付加するコンバータを使った場合、メッセージに項目名が付加されること。
     *
     * @throws Exception
     */
    @Test
    public void testSortMessagesWithItemName() throws Exception {
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                final HashMap<String, Object> result = new HashMap<String, Object>();
                result.put("constraintViolationConverterFactory", new ItemNamedConstraintViolationConverterFactory());
                return result;
            }
        });
        new Expectations() {{
            final List<String> paramNames = new ArrayList<String>();
            paramNames.add("form.name");
            paramNames.add("form.sub.sub3");
            paramNames.add("form.birthday");
            paramNames.add("form.age");
            mockHttpServletRequest.getParameterNames();
            result = Collections.enumeration(paramNames);
            minTimes = 0;
        }};

        final Object action = new Object() {
            @InjectForm(form = UserForm.class, prefix = "form")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext context) {
                return new HttpResponse();
            }
        };

        context.addHandler("//", new HttpMethodBinding(action));

        final MockHttpRequest request = new MockHttpRequest("GET /index.html HTTP/1.1");
        request.setParam("form.age", "10");
        request.setParam("form.name", "");
        request.setParam("form.birthday", "");
        request.setParam("form.sub.sub3", "");

        try {
            context.handleNext(request);
            fail();
        } catch (ApplicationException e) {
            final List<Message> messages = e.getMessages();
            assertThat(messages, IsIterableWithSize.<Message>iterableWithSize(4));
            assertThat(messages, IsIterableContainingInOrder.contains(
                    MessageMatcher.is("[名前]必須項目です。", "form.name"),
                    MessageMatcher.is("[項目3]必須項目です。", "form.sub.sub3"),
                    MessageMatcher.is("[誕生日]必須項目です。", "form.birthday"),
                    MessageMatcher.is("項目間のバリデーションエラー", "form.sub.multiItemValidation")
            ));

            // バリデーションエラー時、リクエストスコープにBeanが設定されること
            UserForm userForm = context.getRequestScopedVar("form");
            assertThat(userForm.getAge(), is("10"));
            assertThat(userForm.getBirthday(), is(""));
            assertThat(userForm.getSub().getSub3(), is(""));
        }
    }

    /**
     * 配列項目のコピーテスト
     *
     * @throws Exception
     */
    @Test
    public void testArrayItem() throws Exception {
        Object action = new Object() {
            @InjectForm(form = WithArrayBean.class, prefix = "form")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                WithArrayBean bean = ctx.getRequestScopedVar("form");
                assertThat(bean.getId(), is("100"));
                assertThat(bean.getNumbers(), is(new String[] {"1", null, "3"}));
                return new HttpResponse();
            }
        };
        context.addHandler(new NormalizationHandler());
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                .setParam("form.id", "100")
                .setParam("form.numbers", "1", "    ", "3"));
    }


    /**
     * バリデーションエラー時に、{@link InjectForm#name()}で指定されたキー名で、
     * リクエストスコープにBeanが格納されること。
     */
    @Test
    public void testCopyBeanToRequestScopeOnErrorWithName() {

        Object action = new Object() {
            // InjectFormのname属性を明示的に指定する.
            @InjectForm(form = SampleBean.class, prefix = "sample", name = "keyOfForm")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        try {
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                       .setParam("sample.userId", "abcdef"));
            fail("must be thrown ApplicationException");
        } catch (ApplicationException e) {
            // バリデーションエラー時、リクエストスコープにBeanが設定されること
            // (InjectFormのnameが指定されているので、キー"keyOfForm")
            SampleBean sampleBean = context.getRequestScopedVar("keyOfForm");
            assertThat(sampleBean.getUserId(), is("abcdef"));
        }
    }

    /**
     * {@link BeanValidationStrategy#setCopyBeanToRequestScopeOnError(boolean)}が偽のとき、
     * リクエストスコープに値がコピーされないこと。
     */
    @Test
    public void testCopyBeanToRequestScopeOnErrorFalse() {
        // コピーされない設定で、SystemRepositoryに登録する
        BeanValidationStrategy sut = new BeanValidationStrategy();
        sut.setCopyBeanToRequestScopeOnError(false);
        repositoryResource.addComponent("validationStrategy", sut);

        Object action = new Object() {
            @InjectForm(form = SampleBean.class, prefix = "sample")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        try {
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                       .setParam("sample.userId", "abcdef"));
            fail("must be thrown ApplicationException");
        } catch (ApplicationException e) {
            assertThat(e.getMessages()
                        .size(), is(1));
            assertThat(e.getMessages()
                        .get(0)
                        .formatMessage(), is("数字でないですよ。"));
            // バリデーションエラー時、リクエストスコープにBeanが設定され*ない*こと
            SampleBean sampleBean = context.getRequestScopedVar("form");
            assertThat(sampleBean, nullValue());
        }

    }

    private static class MessageMatcher extends TypeSafeMatcher<Message> {

        private final String message;

        private final String propertyName;

        public MessageMatcher(final String message, final String propertyName) {
            this.message = message;
            this.propertyName = propertyName;
        }

        public static MessageMatcher is(String message, final String propertyName) {
            return new MessageMatcher(message, propertyName);
        }

        @Override
        protected boolean matchesSafely(final Message item) {
            return item.formatMessage()
                       .equals(message) && ((ValidationResultMessage) item).getPropertyName().equals(propertyName);
        }

        @Override
        protected void describeMismatchSafely(final Message item, final Description mismatchDescription) {
            mismatchDescription.appendText("was ")
                               .appendValue(((ValidationResultMessage) item).getPropertyName())
                               .appendText(":")
                               .appendValue(item.formatMessage());
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(propertyName)
                       .appendText(":")
                       .appendValue(message);
        }
    }
}
