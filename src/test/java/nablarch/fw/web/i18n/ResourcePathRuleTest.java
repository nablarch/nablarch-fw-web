package nablarch.fw.web.i18n;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import nablarch.core.ThreadContext;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FileUtil;
import nablarch.fw.web.servlet.MockServletContext;
import nablarch.fw.web.servlet.MockServletFilterConfig;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.WebFrontController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ResourcePathRule}のテスト。
 * @author Kiyohito Itoh
 */
public class ResourcePathRuleTest {

    ResourcePathRule rule;
    MockServletRequest request;

    @Before
    public void setUp() {

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("webFrontController", new WebFrontController());
                return data;
            }
        });

        WebFrontController servletFilter = SystemRepository.get("webFrontController");
        servletFilter.setServletFilterConfig(new MockServletFilterConfig().setServletContext(new MockServletContext() {
            @Override
            public URL getResource(String arg0) throws MalformedURLException {
                return FileUtil.getResourceURL("classpath:" + arg0.substring(1));
            }
        }));

        request = new MockServletRequest();
        rule = new ResourcePathRule() {
            @Override
            protected String createPathForLanguage(String pathFromContextRoot, String language) {
                return pathFromContextRoot + "_" + language;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        SystemRepository.clear();
    }

    @Test
    public void testConvertToPathFromContextRoot() {

        String path;

        // コンテキストパス指定なし

        request.setContextPath("/");

        request.setRequestURI("/");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/./test.jsp"));

        request.setRequestURI("/test");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/./test.jsp"));

        request.setRequestURI("/test/");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/test/./test.jsp"));

        request.setRequestURI("/test/hoge");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/test/./test.jsp"));

        request.setRequestURI("/test/hoge/");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/test/hoge/./test.jsp"));

        // コンテキストパス指定あり

        request.setContextPath("/apptest");

        request.setRequestURI("/apptest");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/./test.jsp"));

        request.setRequestURI("/apptest/test");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/./test.jsp"));

        request.setRequestURI("/apptest/test/");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/test/./test.jsp"));

        request.setRequestURI("/apptest/test/hoge");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/test/./test.jsp"));

        request.setRequestURI("/apptest/test/hoge/");
        path = "./test.jsp";
        assertThat(rule.convertToPathFromContextRoot(path, request), is("/test/hoge/./test.jsp"));
    }

    @Test
    public void testGetPathForLanguage() {

        String path;

        // コンテキストパス指定なし

        request.setContextPath("/");
        request.setRequestURI("/nablarch/fw/web/R001");

        // 言語がnullの場合。
        path = "i18n/test.jsp";
        ThreadContext.setLanguage(null);
        assertThat(rule.getPathForLanguage(path, request), is("i18n/test.jsp"));

        // 拡張子を含まない場合。
        path = "i18n/test";
        ThreadContext.setLanguage(new Locale("ja"));
        assertThat(rule.getPathForLanguage(path, request), is("i18n/test"));

        // 言語対応のリソースファイルが存在する場合。(相対パス)
        path = "i18n/test.jsp";
        ThreadContext.setLanguage(new Locale("ja"));
        assertThat(rule.getPathForLanguage(path, request), is("i18n/test.jsp_ja"));

        // 言語対応のリソースファイルが存在しない場合。(相対パス)
        path = "i18n/test.jsp";
        ThreadContext.setLanguage(new Locale("en"));
        assertThat(rule.getPathForLanguage(path, request), is("i18n/test.jsp"));

        // 言語対応のリソースファイルが存在する場合。(コンテキストルートからのパス)
        path = "/nablarch/fw/web/i18n/test.jsp";
        ThreadContext.setLanguage(new Locale("ja"));
        assertThat(rule.getPathForLanguage(path, request), is("/nablarch/fw/web/i18n/test.jsp_ja"));

        // 言語対応のリソースファイルが存在しない場合。(コンテキストルートからのパス)
        path = "/nablarch/fw/web/i18n/test.jsp";
        ThreadContext.setLanguage(new Locale("en"));
        assertThat(rule.getPathForLanguage(path, request), is("/nablarch/fw/web/i18n/test.jsp"));

        request.setRequestURI("/nablarch");

        // 言語対応のリソースファイルが存在する場合。(相対パス)
        path = "./nablarch/fw/web/i18n/foo/test2.jsp";
        ThreadContext.setLanguage(new Locale("ja"));
        assertThat(rule.getPathForLanguage(path, request), is("./nablarch/fw/web/i18n/foo/test2.jsp_ja"));

        // コンテキストパス指定あり

        request.setContextPath("/apptest");
        request.setRequestURI("/apptest/nablarch/fw/web/R001");

        // 言語がnullの場合。
        path = "i18n/test.jsp";
        ThreadContext.setLanguage(null);
        assertThat(rule.getPathForLanguage(path, request), is("i18n/test.jsp"));

        // 拡張子を含まない場合。
        path = "i18n/test";
        ThreadContext.setLanguage(new Locale("ja"));
        assertThat(rule.getPathForLanguage(path, request), is("i18n/test"));

        // 言語対応のリソースファイルが存在する場合。(相対パス)
        path = "i18n/test.jsp";
        ThreadContext.setLanguage(new Locale("ja"));
        assertThat(rule.getPathForLanguage(path, request), is("i18n/test.jsp_ja"));

        // 言語対応のリソースファイルが存在しない場合。(相対パス)
        path = "i18n/test.jsp";
        ThreadContext.setLanguage(new Locale("en"));
        assertThat(rule.getPathForLanguage(path, request), is("i18n/test.jsp"));

        // 言語対応のリソースファイルが存在する場合。(コンテキストルートからのパス)
        path = "/nablarch/fw/web/i18n/test.jsp";
        ThreadContext.setLanguage(new Locale("ja"));
        assertThat(rule.getPathForLanguage(path, request), is("/nablarch/fw/web/i18n/test.jsp_ja"));

        // 言語対応のリソースファイルが存在しない場合。(コンテキストルートからのパス)
        path = "/nablarch/fw/web/i18n/test.jsp";
        ThreadContext.setLanguage(new Locale("en"));
        assertThat(rule.getPathForLanguage(path, request), is("/nablarch/fw/web/i18n/test.jsp"));
    }

    @Test
    public void testGetPathForLanguageForMalformedURLException() {

        String path;

        // コンテキストパス指定なし

        request.setContextPath("/");
        request.setRequestURI("/nablarch/fw/web/R001");

        // 不正なパス指定の場合。
        WebFrontController filter = SystemRepository.get("webFrontController");
        filter.setServletFilterConfig(new MockServletFilterConfig().setServletContext(new MockServletContext() {
            @Override
            public URL getResource(String arg0) throws MalformedURLException {
                return new URL("hoge://" + arg0);
            }
        }));

        path = "i18n:/test.jsp";
        ThreadContext.setLanguage(new Locale("ja"));
        try {
            rule.getPathForLanguage(path, request);
            fail("IllegalArgumentExceptionが発生する。");
        } catch (IllegalArgumentException e) {
            assertThat(e.getCause().getClass().getName(), is(MalformedURLException.class.getName()));
        }
    }
    
    @Test
    public void testGetServletContextCreatorDefault() {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("webFrontController", null);
                return data;
            }
        });
        
        when(request.getSession(false)).thenReturn(null);
        
        try {
            rule.existsResource("test", request);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("servletContext was not found."));
        }
    }
    
    @Test
    public void testGetServletContextCreatorBasic() {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("webFrontController", null);
                return data;
            }
        });
        
        when(request.getSession(false)).thenReturn(null);
        
        rule.setServletContextCreator(new BasicServletContextCreator());
        
        try {
            rule.existsResource("test", request);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("servletContext was not found."));
        }
    }
    
    @Test
    public void testGetServletContextCreatorTest() {
        final HttpServletRequest request = mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("webFrontController", null);
                return data;
            }
        });
        
        rule.setServletContextCreator(new ServletContextCreator() {
            @Override
            public ServletContext create(HttpServletRequest request) {
                return request.getSession(true).getServletContext();
            }
        });
        
        // 例外が発生しないこと
        rule.existsResource("test", request);
    }
}
