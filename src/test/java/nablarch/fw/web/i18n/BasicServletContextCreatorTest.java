package nablarch.fw.web.i18n;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import nablarch.common.util.BasicRequestIdExtractor;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.web.servlet.MockServletContext;
import nablarch.fw.web.servlet.MockServletFilterConfig;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.WebFrontController;
import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link BasicRequestIdExtractor}のテスト。
 * @author Naoki Yamamoto
 */
public class BasicServletContextCreatorTest {
    
    
    /** テスト対象 */
    ServletContextCreator creator = new BasicServletContextCreator();

    MockServletRequest request;

    @After
    public void tearDown() throws Exception {
        SystemRepository.clear();
    }


    /**
     * {@link BasicServletContextCreator#create(HttpServletRequest)}のテスト。<br>
     * セッションからサーブレットコンテキストを取得するケース。
     */
    @Test
    public void testCreateFromSession() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class, RETURNS_DEEP_STUBS);
        
        when(request.getSession(false)).thenReturn(session);
        
        assertThat(creator.create(request), instanceOf(ServletContext.class));
    }
    
    /**
     * {@link BasicServletContextCreator#create(HttpServletRequest)}のテスト。<br>
     * リポジトリからサーブレットコンテキストを取得するケース。
     */
    @Test
    public void testCreateFromRepository() {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                WebFrontController controller = new WebFrontController();
                controller.setServletFilterConfig(new MockServletFilterConfig().setServletContext(new MockServletContext()));
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("webFrontController", controller);
                return data;
            }
        });

        when(request.getSession(false)).thenReturn(null);

        assertThat(creator.create(request), instanceOf(ServletContext.class));
    }
    
    /**
     * {@link BasicServletContextCreator#create(HttpServletRequest)}のテスト。<br>
     * サーブレットコンテキストを取得できないケース。
     */
    @Test
    public void testCreateException() {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getSession(false)).thenReturn(null);

        try {
            creator.create(request);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("servletContext was not found."));
        }
    }
}
