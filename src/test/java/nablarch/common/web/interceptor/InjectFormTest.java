package nablarch.common.web.interceptor;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nablarch.common.web.interceptor.sample.form.SampleForm;
import nablarch.common.web.validator.NablarchValidationStrategy;
import nablarch.common.web.validator.bean.SampleBean;
import nablarch.core.ThreadContext;
import nablarch.core.message.ApplicationException;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpMethodBinding;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.message.MockStringResourceHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * {@link InjectForm}のテスト。
 * @author Kiyohito Itoh
 */
public class InjectFormTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("nablarch/common/web/interceptor/sample/inject-form-test.xml");

    private final HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

    private final HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);

    private final ServletContext mockServletContext = mock(ServletContext.class);

    private ServletExecutionContext context;

    private static final String[][] MESSAGES;

    static {
        int length = 15;
        String[][] messages = new String[length][];
        for (int i = 0; i < length; i++) {
            String id = "M" + StringUtil.lpad(String.valueOf(i + 1), 3, '0');
            // {"M001", "ja", "M001-ja", "en", "M001-en"}
            messages[i] = new String[] {id, "ja", id + "-ja", "en", id + "-en"};
        }
        MESSAGES = messages;
    }

    @Before
    public void setUp() {

        MockStringResourceHolder resourceHolder = repositoryResource.getComponent("stringResourceHolder");
        resourceHolder.setMessages(MESSAGES);

        ThreadContext.setLanguage(Locale.JAPANESE);

        when(mockHttpServletRequest.getContextPath()).thenReturn("");
        when(mockHttpServletRequest.getRequestURI()).thenReturn("/dummy");
        
        context = spy(new ServletExecutionContext(mockHttpServletRequest,
                mockHttpServletResponse, mockServletContext));

        doReturn(new HashMap<>()).when(context).getRequestScopeMap();
    }

    /**
     * 初期化メソッドとバリデーション対象メソッドがともに指定され、バリデーションOKの場合、
     * バリデーション、単純にフォーム生成、初期化メソッド呼び出し、バリデーション済みのフォームから単純に生成したフォームに値コピーの順に処理され、
     * 単純に生成したフォームがリクエストスコープに設定されること。
     * プレフィックスありの場合。
     */
    @Test
    public void testInitializationAndValidationWithValidParameters() {
        
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, prefix="sample", validate="test", initialize="setInitValues")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleForm form = ctx.getRequestScopedVar("form");
                assertNotNull(form);
                assertThat(form.getUserId(), is("1234567890")); // override "9999999999"
                assertThat(form.getName(), is("no-validation-property"));
                return new HttpResponse();
            }
        };

        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                .setParam("sample.userId", "1234567890"));
    }

    /**
     * 初期化メソッドとバリデーション対象メソッドがともに指定され、バリデーションOKの場合、
     * バリデーション、単純にフォーム生成、初期化メソッド呼び出し、バリデーション済みのフォームから単純に生成したフォームに値コピーの順に処理され、
     * 単純に生成したフォームがリクエストスコープに設定されること。
     * プレフィックスなしの場合。
     */
    @Test
    public void testInitializationAndValidationWithValidNoPrefixParameters() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, validate="test", initialize="setInitValues")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleForm form = ctx.getRequestScopedVar("form");
                assertNotNull(form);
                assertThat(form.getUserId(), is("0987654321")); // override "9999999999"
                assertThat(form.getName(), is("no-validation-property"));
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                .setParam("userId", "0987654321"));
    }


    /**
     * 初期化メソッドとバリデーション対象メソッドがともに指定され、バリデーションNGの場合、
     * エラーメッセージが設定されたアプリ例外が送出されること。
     */
    @Test
    public void testInitializationAndValidationWithInvalidParameters() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, prefix="sample", validate="test", initialize="setInitValues")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        try {
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                    .setParam("sample.userId", "123456789"));
            fail("must be thrown ApplicationException");
        } catch (ApplicationException e) {
            assertThat(e.getMessages().size(), is(1));
            assertThat(e.getMessages().get(0).formatMessage(), is("M013-ja"));
        }
    }

    /**
     * 初期化メソッドのみが指定された場合、
     * フォーム生成、初期化メソッド呼び出しの順に処理され、生成されたフォームがリクエストスコープに設定されること。
     */
    @Test
    public void testInitialization() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, initialize="setInitValues")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleForm form = ctx.getRequestScopedVar("form");
                assertNotNull(form);
                assertThat(form.getUserId(), is("9999999999"));
                assertThat(form.getName(), is("no-validation-property"));
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                .setParam("sample.userId", "1234567890"));
    }

    /**
     * バリデーション対象メソッドのみが指定され、バリデーションOKの場合、
     * バリデーション済みのフォームがリクエストスコープに設定されること。
     */
    @Test
    public void testValidationWithValidParameters() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, prefix="sample", validate="test")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleForm form = ctx.getRequestScopedVar("form");
                assertNotNull(form);
                assertThat(form.getUserId(), is("1234567890"));
                assertNull(form.getName()); // no validation property
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                .setParam("sample.userId", "1234567890"));
    }

    /**
     * バリデーション対象メソッドのみが指定され、バリデーションNGの場合、
     * エラーメッセージが設定されたアプリ例外が送出されること。
     */
    @Test
    public void testValidationWithInvalidParameters() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, prefix="sample", validate="test")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        try {
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                    .setParam("sample.userId", "123456789"));
            fail("must be thrown ApplicationException");
        } catch (ApplicationException e) {
            assertThat(e.getMessages().size(), is(1));
            assertThat(e.getMessages().get(0).formatMessage(), is("M013-ja"));
        }
    }

    /**
     * 初期化メソッドとバリデーション対象メソッドがともに指定されなかった場合、
     * 生成したフォームがリクエストスコープに設定されること。
     */
    @Test
    public void testNoInitializationAndNoValidation() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleForm form = ctx.getRequestScopedVar("form");
                assertNotNull(form);
                assertNull(form.getUserId());
                assertNull(form.getName());
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1"));
    }

    /**
     * name属性を指定した場合、
     * 生成したフォームがname属性に指定した名前を変数名としてリクエストスコープに設定されること。
     */
    @Test
    public void testNameAttribute() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, name="test")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                assertNull(ctx.getRequestScopedVar("form"));
                assertNotNull(ctx.getRequestScopedVar("test"));
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1"));
    }

    @SuppressWarnings("serial")
    private static final class NoInstance implements Serializable {
        private NoInstance() {}
    }

    /**
     * フォームの生成に失敗した場合、実行時例外が送出されること。
     */
    @Test
    public void testFormInstantiationFailed() {
        Object action = new Object() {
            @InjectForm(form=NoInstance.class)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        try {
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1"));
            fail("must be thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("class instantiation failed. class = [nablarch.common.web.interceptor.InjectFormTest$NoInstance]"));
            assertNotNull(e.getCause());
        }
    }

    /**
     * フォームの初期化に失敗した場合、実行時例外が送出されること。
     */
    @Test
    public void testFormInitializationFailed() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, initialize="init")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        try {
            context.addHandler("//", new HttpMethodBinding(action));
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1"));
            fail("must be thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("form initialization failed. form = [nablarch.common.web.interceptor.sample.form.SampleForm], method = [init]"));
            assertNotNull(e.getCause());
        }
    }

    /**
     * validationGroupが指定されていた場合、実行時例外が送出されること。
     */
    @Test
    public void testUnexpectedAttributeValidationGroup() {
        Object action = new Object() {
            @InjectForm(form=SampleForm.class, validationGroup = SampleBean.Test1.class)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse();
            }
        };
        try {
            context.addHandler("//", new HttpMethodBinding(action));
            context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1"));
            fail("must be thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("validationGroup attribute cannot be specified when using NablarchValidationStrategy"));
            assertNull(e.getCause());
        }
    }

    /**
     * validationStrategyのコンポーネントが定義されていない場合には、<br/>
     * デフォルトのStrategyとして{@link NablarchValidationStrategy}が<br/>
     * 使用されることを確認する.
     */
    @Test
    public void testNoValidationStrategyDef() {
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(
            "nablarch/common/web/interceptor/sample/inject-form-test-no-validation-strategy-def.xml")));

        // NablarchValidationStrategyを使用するテストケースが成功することで確認とする
        // 本テストクラスのtestValidationWithValidParameters()のケースを実行する
        Object action = new Object() {
            @InjectForm(form = SampleForm.class, prefix = "sample", validate = "test")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                SampleForm form = ctx.getRequestScopedVar("form");
                assertNotNull(form);
                assertThat(form.getUserId(), is("1234567890"));
                assertNull(form.getName()); // no validation property
                return new HttpResponse();
            }
        };
        context.addHandler("//", new HttpMethodBinding(action));
        context.handleNext(new MockHttpRequest("GET /index.html HTTP/1.1")
                                .setParam("sample.userId", "1234567890"));

    }

}
