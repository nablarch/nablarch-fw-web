package nablarch.common.web.handler.threadcontext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nablarch.core.ThreadContext;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link TimeZoneAttributeInHttpUtilTest} のテスト。
 *
 */
public class TimeZoneAttributeInHttpUtilTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final HttpRequest mockHttpRequest = mock(HttpRequest.class);

    private final HttpServletRequest mockServletRequest = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

    private final HttpServletResponse mockServletResponse = mock(HttpServletResponse.class);

    private final ServletContext mockServletContext = mock(ServletContext.class);

    @Before
    public void setUp(){
        when(mockServletRequest.getContextPath()).thenReturn("/");
        when(mockServletRequest.getRequestURI()).thenReturn("/");
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
