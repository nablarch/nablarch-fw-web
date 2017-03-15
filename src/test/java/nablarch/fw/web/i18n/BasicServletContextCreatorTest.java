package nablarch.fw.web.i18n;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.common.util.BasicRequestIdExtractor;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.web.servlet.MockServletContext;
import nablarch.fw.web.servlet.MockServletFilterConfig;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.WebFrontController;
import org.junit.After;
import org.junit.Test;

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
    public void testCreateFromSession(@Mocked final HttpServletRequest request, @Mocked final HttpSession session) {
        
        new Expectations() {{
            request.getSession(false); result = session;
        }};
        
        assertThat(creator.create(request), instanceOf(ServletContext.class));
    }
    
    /**
     * {@link BasicServletContextCreator#create(HttpServletRequest)}のテスト。<br>
     * リポジトリからサーブレットコンテキストを取得するケース。
     */
    @Test
    public void testCreateFromRepository(@Mocked final HttpServletRequest request) {

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

        new Expectations() {{
            request.getSession(false); result = null;
        }};

        assertThat(creator.create(request), instanceOf(ServletContext.class));
    }
    
    /**
     * {@link BasicServletContextCreator#create(HttpServletRequest)}のテスト。<br>
     * サーブレットコンテキストを取得できないケース。
     */
    @Test
    public void testCreateException(@Mocked final HttpServletRequest request) {

        new Expectations() {{
            request.getSession(false); result = null;
        }};

        try {
            creator.create(request);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("servletContext was not found."));
        }
    }
}
