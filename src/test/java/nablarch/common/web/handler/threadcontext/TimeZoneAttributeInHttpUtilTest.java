package nablarch.common.web.handler.threadcontext;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.core.ThreadContext;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * {@link TimeZoneAttributeInHttpUtilTest} のテスト。
 *
 */
@Ignore("jacoco と jmockit が競合してエラーになるため")
public class TimeZoneAttributeInHttpUtilTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mocked
    private HttpRequest mockHttpRequest;

    @Mocked
    private HttpServletRequest mockServletRequest;

    @Mocked
    private HttpServletResponse mockServletResponse;

    @Mocked
    private ServletContext mockServletContext;

    @Before
    public void setUp(){
        new Expectations() {{
            mockServletRequest.getContextPath();
            result = "/";
            mockServletRequest.getRequestURI();
            result = "/";
        }};
    }

    private void setUpRepository(final TimeZoneAttributeInHttpSupport support){
        // リポジトリにTimeZoneAttributeInHttpSupportを定義
        SystemRepository.load(new ObjectLoader() {
            @SuppressWarnings("serial")
            @Override
            public Map<String, Object> load() {
                return new HashMap<String, Object>() {{
                    put("timeZoneAttribute", support);
                }};
            }
        });
    }

    /**
     * {@link TimeZoneAttributeInHttpUtil#keepTimeZone(HttpRequest, ExecutionContext, String)}のテスト。
     * <p/>
     * リポジトリに値が無い場合、例外を送出するか。
     */
    @Test
    public void testNotRegisteredInRepository() {

        SystemRepository.clear();
        ServletExecutionContext ctx = new ServletExecutionContext(mockServletRequest, mockServletResponse, mockServletContext);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("specified timeZoneAttribute is not registered in SystemRepository.");

        TimeZoneAttributeInHttpUtil.keepTimeZone(mockHttpRequest, ctx, "ja");
    }

    /**
     * {@link TimeZoneAttributeInHttpUtil#keepTimeZone(HttpRequest, ExecutionContext, String)}のテスト。
     * <p/>
     * 設定したタイムゾーンがサポート外だった場合、リターンするか。
     */
    @Test
    public void testNoSupportTimeZone(){

        final TimeZoneAttributeInHttpSupport support = new TimeZoneAttributeInHttpSupport() {
            @Override
            protected void keepTimeZone(HttpRequest req, ServletExecutionContext ctx, String timeZone) {}
            @Override
            protected String getKeepingTimeZone(HttpRequest req, ServletExecutionContext ctx) {
                return null;
            }
        };
        support.setSupportedTimeZones("");
        setUpRepository(support);

        ServletExecutionContext ctx = new ServletExecutionContext(mockServletRequest, mockServletResponse, mockServletContext);

        ThreadContext.clear();
        TimeZoneAttributeInHttpUtil.keepTimeZone(mockHttpRequest, ctx, "Asia/Tokyo");

        //サポート外によってリターンするため、スレッドコンテキストが保持するタイムゾーンはnullのはず。
        assertNull(ThreadContext.getTimeZone());

    }

    /**
     * {@link TimeZoneAttributeInHttpUtil#keepTimeZone(HttpRequest, ExecutionContext, String)}のテスト。
     * <p/>
     * 正常系。スレッドコンテキストに指定したタイムゾーンが設定されているか。
     */
    @Test
    public void testKeepTimeZone() {

        final TimeZoneAttributeInHttpSupport support = new TimeZoneAttributeInHttpSupport() {
            @Override
            protected void keepTimeZone(HttpRequest req, ServletExecutionContext ctx, String timeZone) {
            }

            @Override
            protected String getKeepingTimeZone(HttpRequest req, ServletExecutionContext ctx) {
                return null;
            }
        };
        support.setSupportedTimeZones("Asia/Tokyo");
        setUpRepository(support);

        ServletExecutionContext ctx = new ServletExecutionContext(mockServletRequest, mockServletResponse, mockServletContext);
        ThreadContext.clear();
        TimeZoneAttributeInHttpUtil.keepTimeZone(mockHttpRequest, ctx, "Asia/Tokyo");

        assertEquals("日本標準時", ThreadContext.getTimeZone().getDisplayName());
    }
}
