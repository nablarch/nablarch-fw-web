package nablarch.fw.web.servlet;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class UriUtilTest {

    @Test
    public void testConvertToPathFromContextRoot() {

        MockServletRequest request = new MockServletRequest();

        // コンテキストパス指定なし、コンテキストルートからのパス

        request.setContextPath("");
        request.setRequestURI("/hoge/foo/fuga");

        assertThat(UriUtil.convertToPathFromContextRoot("/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("/test", request), is("/test"));
        assertThat(UriUtil.convertToPathFromContextRoot("/test/", request), is("/test/"));
        assertThat(UriUtil.convertToPathFromContextRoot("/test/hoge", request), is("/test/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("/test/hoge/", request), is("/test/hoge/"));

        // コンテキストパス指定あり、コンテキストルートからのパス

        request.setContextPath("/apptest");
        request.setRequestURI("/hoge/foo/fuga");

        assertThat(UriUtil.convertToPathFromContextRoot("/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("/test", request), is("/test"));
        assertThat(UriUtil.convertToPathFromContextRoot("/test/", request), is("/test/"));
        assertThat(UriUtil.convertToPathFromContextRoot("/test/hoge", request), is("/test/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("/test/hoge/", request), is("/test/hoge/"));

        // コンテキストパス指定なし、絶対URL

        request.setContextPath("");
        request.setRequestURI("/hoge/foo/fuga");

        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest", request), is("/apptest"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/", request), is("/apptest/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/action/hoge", request), is("/apptest/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/action/hoge/", request), is("/apptest/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest", request), is("/apptest"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/", request), is("/apptest/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/action/hoge", request), is("/apptest/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/action/hoge/", request), is("/apptest/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest", request), is("/apptest"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/", request), is("/apptest/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/action/hoge", request), is("/apptest/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/action/hoge/", request), is("/apptest/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest", request), is("/apptest"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/", request), is("/apptest/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/action/hoge", request), is("/apptest/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/action/hoge/", request), is("/apptest/action/hoge/"));

        // コンテキストパス指定なし("/")、絶対URL

        request.setContextPath("/");
        request.setRequestURI("/hoge/foo/fuga");

        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest", request), is("/apptest"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/", request), is("/apptest/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/action/hoge", request), is("/apptest/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/action/hoge/", request), is("/apptest/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest", request), is("/apptest"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/", request), is("/apptest/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/action/hoge", request), is("/apptest/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/action/hoge/", request), is("/apptest/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest", request), is("/apptest"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/", request), is("/apptest/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/action/hoge", request), is("/apptest/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/action/hoge/", request), is("/apptest/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest", request), is("/apptest"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/", request), is("/apptest/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/action/hoge", request), is("/apptest/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/action/hoge/", request), is("/apptest/action/hoge/"));

        // コンテキストパス指定あり、絶対URL

        request.setContextPath("/apptest");
        request.setRequestURI("/hoge/foo/fuga");

        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/action/hoge", request), is("/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com/apptest/action/hoge/", request), is("/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/action/hoge", request), is("/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("http://test.com:8080/apptest/action/hoge/", request), is("/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/action/hoge", request), is("/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com/apptest/action/hoge/", request), is("/action/hoge/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/", request), is("/"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/action/hoge", request), is("/action/hoge"));
        assertThat(UriUtil.convertToPathFromContextRoot("https://test.com:8080/apptest/action/hoge/", request), is("/action/hoge/"));

        // コンテキストパス指定なし、相対パス、フォワードなし

        request.setContextPath("");
        request.setAttribute("javax.servlet.forward.request_uri", null);

        request.setRequestURI("/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/./test.jsp"));

        request.setRequestURI("/test");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/./test.jsp"));

        request.setRequestURI("/test/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/./test.jsp"));

        request.setRequestURI("/test/hoge");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/./test.jsp"));

        request.setRequestURI("/test/hoge/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/hoge/./test.jsp"));

        // コンテキストパス指定なし、相対パス、フォワードあり

        request.setContextPath("");
        request.setRequestURI("/hoge/foo/fuga");

        request.setAttribute("javax.servlet.forward.request_uri", "/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/./test.jsp"));

        request.setAttribute("javax.servlet.forward.request_uri", "/test");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/./test.jsp"));

        request.setAttribute("javax.servlet.forward.request_uri", "/test/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/./test.jsp"));

        request.setAttribute("javax.servlet.forward.request_uri", "/test/hoge");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/./test.jsp"));

        request.setAttribute("javax.servlet.forward.request_uri", "/test/hoge/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/hoge/./test.jsp"));

        // コンテキストパス指定あり、相対パス、フォワードなし

        request.setContextPath("/apptest");
        request.setAttribute("javax.servlet.forward.request_uri", null);

        request.setRequestURI("/apptest");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/./test.jsp"));

        request.setRequestURI("/apptest/test");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/./test.jsp"));

        request.setRequestURI("/apptest/test/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/./test.jsp"));

        request.setRequestURI("/apptest/test/hoge");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/./test.jsp"));

        request.setRequestURI("/apptest/test/hoge/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/hoge/./test.jsp"));

        // コンテキストパス指定あり、相対パス、フォワードあり

        request.setContextPath("/apptest");
        request.setRequestURI("/hoge/foo/fuga");

        request.setAttribute("javax.servlet.forward.request_uri", "/apptest");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/./test.jsp"));

        request.setAttribute("javax.servlet.forward.request_uri", "/apptest/test");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/./test.jsp"));

        request.setAttribute("javax.servlet.forward.request_uri", "/apptest/test/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/./test.jsp"));

        request.setAttribute("javax.servlet.forward.request_uri", "/apptest/test/hoge");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/./test.jsp"));

        request.setAttribute("javax.servlet.forward.request_uri", "/apptest/test/hoge/");
        assertThat(UriUtil.convertToPathFromContextRoot("./test.jsp", request), is("/test/hoge/./test.jsp"));
    }
}
