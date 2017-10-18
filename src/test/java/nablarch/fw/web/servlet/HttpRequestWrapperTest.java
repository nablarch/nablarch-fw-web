package nablarch.fw.web.servlet;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.upload.PartInfo;
import nablarch.fw.web.useragent.UserAgent;
import nablarch.fw.web.useragent.UserAgentParser;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link HttpRequestWrapper}のテストクラス。
 *
 * @author Naoki Yamamoto
 */
public class HttpRequestWrapperTest {

    @Mocked
    public NablarchHttpServletRequestWrapper nablarchHttpServletRequestWrapper;

    private HttpRequestWrapper sut;

    @Before
    public void setUp() throws Exception {
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getMethod();
            minTimes = 0;
            result = "GET";
            nablarchHttpServletRequestWrapper.getContextPath();
            minTimes = 0;
            result = "www.example.com";
            nablarchHttpServletRequestWrapper.getRequestURI();
            minTimes = 0;
            result = "www.example.com/index.html";
            nablarchHttpServletRequestWrapper.getProtocol();
            minTimes = 0;
            result = "HTTP/1.1";
            nablarchHttpServletRequestWrapper.getCookies();
            minTimes = 0;
            result = null;
        }};
    }

    /**
     * コンストラクタの実行時、リクエストURIが設定されていることを確認。
     *
     * @throws Exception
     */
    @Test
    public void testConstructor() throws Exception {
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("リクエストURIが正しく設定されていること", sut.getRequestUri(), is("/index.html"));
    }

    /**
     * リクエストのメソッドを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetMethod() throws Exception {

        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("メソッドを取得できること", sut.getMethod(), is("GET"));

        // メソッドの前後に空白が含まれるケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getMethod();
            result = " POST ";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("トリムされたメソッドを取得できること", sut.getMethod(), is("POST"));
    }

    /**
     * リクエストURIを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetRequestUri() throws Exception {

        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("リクエストURIを取得できること", sut.getRequestUri(), is("/index.html"));

        // リクエストURIの前後に空白が含まれるケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getRequestURI();
            result = "www.example.com /test.html ";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("トリムされたリクエストURIを取得できること", sut.getRequestUri(), is("/test.html"));
    }

    /**
     * リクエストURIを設定できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testSetRequestUri() throws Exception {

        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        HttpRequest result = sut.setRequestUri("/test.html");
        assertThat("同一インスタンスが返却されること", sut, sameInstance(result));
        assertThat("リクエストURIが設定されていること", sut.getRequestUri(), is("/test.html"));

        // リクエストURIの前後に空白が含まれるケース
        result = sut.setRequestUri(" /trim.html ");
        assertThat("同一インスタンスが返却されること", sut, sameInstance(result));
        assertThat("トリムされたリクエストURIが設定されていること", sut.getRequestUri(), is("/trim.html"));
    }

    /**
     * リクエストパスを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetRequestPath() throws Exception {

        // クエリストリングが設定されていないケース
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("リクエストパスを取得できること", sut.getRequestPath(), is("/index.html"));

        // クエリストリングが設定されているケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getRequestURI();
            result = "www.example.com/test.html?key=value";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("クエリストリングが除外されていること", sut.getRequestPath(), is("/test.html"));
    }

    /**
     * リクエストパスを設定できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testSetRequestPath() throws Exception {

        // クエリストリングが設定されていないケース
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        HttpRequest result = sut.setRequestPath("/test.html");
        assertThat("同一インスタンスが返却されること", sut, sameInstance(result));
        assertThat("リクエストパスが設定されていること", sut.getRequestUri(), is("/test.html"));

        // クエリストリングが設定されているケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getRequestURI();
            result = "www.example.com/index.html?key=value";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        result = sut.setRequestPath("/test.html");
        assertThat("同一インスタンスが返却されること", sut, sameInstance(result));
        assertThat("クエリストリングが変更されていないこと", sut.getRequestUri(), is("/test.html?key=value"));

        // リクエストパスに$が含まれるケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getRequestURI();
            result = "www.example.com/index.html?key=value";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        result = sut.setRequestPath("/t$est$.html");
        assertThat("同一インスタンスが返却されること", sut, sameInstance(result));
        assertThat("リクエストパスに$が含まれていても問題なく設定できること", sut.getRequestUri(), is("/t$est$.html?key=value"));
    }

    /**
     * HTTPバージョンを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetHttpVersion_success() throws Exception {
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("HTTPバージョンを取得できること", sut.getHttpVersion(), is("HTTP/1.1"));
    }

    /**
     * リクエストパラメータのMapを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetParamMap() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getParameterMap();
            result = new HashMap<String, String[]>() {{
                put("key", new String[]{"value1", "value2"});
            }};
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        Map<String, String[]> map = sut.getParamMap();
        assertThat("リクエストパラメータのMapを取得できること", map.get("key"), is(notNullValue()));
        assertThat("リクエストパラメータのMapに設定された値を取得できること", map.get("key")[0], is("value1"));
        assertThat("リクエストパラメータのMapに設定された値を取得できること", map.get("key")[1], is("value2"));
    }

    /**
     * リクエストパラメータを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetParam() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getParameterMap();
            result = new HashMap<String, String[]>() {{
                put("key", new String[]{"value1", "value2"});
            }};
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("リクエストパラメータの値を取得できること", sut.getParam("key")[0], is("value1"));
        assertThat("リクエストパラメータの値を取得できること", sut.getParam("key")[1], is("value2"));
    }

    /**
     * リクエストパラメータを設定できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testSetParam() throws Exception {

        final Map<String, String[]> paramMap = new HashMap<String, String[]>() {{
            put("key", new String[]{"value"});
        }};

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getParameterMap();
            result = paramMap;

        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        HttpRequest result = sut.setParam("test_key", "test_value1", "test_value2", "test_value3");

        String[] values = paramMap.get("test_key");

        assertThat("同一インスタンスが返却されること", sut, sameInstance(result));
        assertThat("リクエストパラメータが設定されていること", values, is(notNullValue()));
        assertThat("リクエストパラメータの1番目の値が正しいこと", values[0], is("test_value1"));
        assertThat("リクエストパラメータの2番目の値が正しいこと", values[1], is("test_value2"));
        assertThat("リクエストパラメータの3番目の値が正しいこと", values[2], is("test_value3"));

        values = paramMap.get("key");
        assertThat("元々リクエストパラメータに設定されていた値が保持されていること", values, is(notNullValue()));
        assertThat("元々リクエストパラメータに設定されていた値が変更されていないこと", values[0], is("value"));
    }

    /**
     * リクエストパラメータのMapを設定できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testSetParamMap() throws Exception {

        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        final Map<String, String[]> map = new HashMap<String, String[]>() {{
            put("test_key", new String[]{"test_value1", "test_value2", "test_value3"});
        }};
        HttpRequest result = sut.setParamMap(map);

        assertThat("同一インスタンスが返却されること", sut, sameInstance(result));
        new Verifications() {{
            // リクエストパラメータMapを設定していること
            nablarchHttpServletRequestWrapper.setParamMap(map);
        }};
    }

    /**
     * ヘッダのMapを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetHeaderMap() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getHeaderMap();
            result = new HashMap<String, String>() {{
                put("key", "value");
            }};
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        Map<String, String> map = sut.getHeaderMap();
        assertThat("ヘッダのMapを取得できること", map.get("key"), is(notNullValue()));
        assertThat("ヘッダのMapに設定された値を取得できること", map.get("key"), is("value"));
    }

    /**
     * ヘッダを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetHeader() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getHeader("key");
            result = "value";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        assertThat("ヘッダに設定された値を取得できること", sut.getHeader("key"), is("value"));
    }

    /**
     * Hostを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetHost() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getHeader("Host");
            result = "host_value";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        assertThat("Hostを取得できること", sut.getHost(), is("host_value"));
    }

    /**
     * Cookieを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetCookie() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getCookies();
            result = new Cookie[]{new Cookie("key1", "value1"), new Cookie("key2", "value2")};
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        HttpCookie result = sut.getCookie();
        assertThat("Cookieを取得できること",result, is(notNullValue()));
        assertThat("取得したCookieに想定した値が設定されていること", result.get("key1"), is("value1"));
        assertThat("取得したCookieに想定した値が設定されていること", result.get("key2"), is("value2"));
    }

    /**
     * マルチパートの一部を取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetPart() throws Exception {

        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        assertThat("空のListを取得できること", sut.getPart("key").isEmpty(), is(true));

        Deencapsulation.setField(sut, "multipart", new HashMap<String, List<PartInfo>>() {{
            put("key", new ArrayList<PartInfo>() {{
                add(PartInfo.newInstance("name"));
            }});
        }});
        List<PartInfo> result = sut.getPart("key");
        assertThat("Listが空ではないこと", result.isEmpty(), is(false));
        assertThat("取得したListにマルチパート情報が設定されていること", result.get(0).getName(), is("name"));
    }

    /**
     * マルチパート情報を設定できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testSetMultipart() throws Exception {

        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        sut.setMultipart(null);

        Map<String, List<PartInfo>> result = sut.getMultipart();
        assertThat("Mapを取得できること", result, is(notNullValue()));
        assertThat("Mapの中身がを空であること", result.isEmpty(), is(true));

        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        Map<String, List<PartInfo>> multipart = new HashMap<String, List<PartInfo>>() {{
            put("key", new ArrayList<PartInfo>() {{
                add(PartInfo.newInstance("name"));
            }});
        }};
        sut.setMultipart(multipart);

        result = sut.getMultipart();
        assertThat("Mapを取得できること", result, is(notNullValue()));
        assertThat("Mapが空ではないこと", result.isEmpty(), is(false));
        assertThat("指定したキーでListを取得できること", result.get("key"), is(notNullValue()));
        assertThat("Listが空ではないこと", result.get("key").isEmpty(), is(false));
        assertThat("Listにマルチパート情報が設定されていること", result.get("key").get(0).getName(), is("name"));
    }

    /**
     * マルチパートを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetMultipart() throws Exception {

        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        Deencapsulation.setField(sut, "multipart"
                , new HashMap<String, List<PartInfo>>() {{
            put("key", new ArrayList<PartInfo>() {{
                add(PartInfo.newInstance("name"));
            }});
        }});
        Map<String, List<PartInfo>> result = sut.getMultipart();
        assertThat("Mapを取得できること", result, is(notNullValue()));
        assertThat("Mapが空ではないこと", result.isEmpty(), is(false));
        assertThat("指定したキーでListを取得できること", result.get("key"), is(notNullValue()));
        assertThat("Listが空ではないこと", result.get("key").isEmpty(), is(false));
        assertThat("Listに設定されたマルチパート情報に想定した値が設定されていること", result.get("key").get(0).getName(), is("name"));
    }

    /**
     * デフォルトのパーサを使用してUserAgent情報を取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetUserAgent_default() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getHeader("User-Agent");
            result = "test";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        UserAgent result = sut.getUserAgent();
        assertThat("UserAgentを取得できること", result, is(notNullValue()));
        assertThat("UserAgentに想定した値が設定されていること", result.getText(), is("test"));
    }

    /**
     * {@link SystemRepository}で定義されたパーサを使用してUserAgent情報を取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetUserAgent_custom() throws Exception {

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                return new HashMap<String, Object>() {{
                    put("userAgentParser", new CustomUserAgentParser());
                }};
            }
        });

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getHeader("User-Agent");
            result = "test";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        UserAgent result = sut.getUserAgent();
        assertThat("UserAgentを取得できること", result, is(notNullValue()));
        assertThat("UserAgentに想定した値が設定されていること", result.getText(), is("custom"));
    }

    /**
     * 入力ストリームを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetInputStream_success() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getInputStream();
            result = new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return 0;
                }
            };
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        ServletInputStream result = sut.getInputStream();
        assertThat("入力ストリームを取得できること", result, is(notNullValue()));
        assertThat("取得した入力ストリームから想定した値を取得できること", result.read(), is(0));
    }

    /**
     * 入力ストリーム取得時にエラーが発生した場合、{@link nablarch.fw.results.InternalError}を送出することを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetInputStream_error() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getInputStream();
            result = new IOException();
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        try {
            sut.getInputStream();
            fail("入力ストリームの取得時にInternalErrorが送出されること");
        } catch (nablarch.fw.results.InternalError e) {
            assertThat("CauseがIOExceptionであること", e.getCause(), instanceOf(IOException.class));
        }
    }

    /**
     * Content-Typeをリクエストのヘッダから取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetContentType_header() throws Exception {

        // ヘッダにのみ設定されているケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getHeader("Content-Type");
            result = "type";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        assertThat("Content-Typeを取得できること", sut.getContentType(), is("type"));

        // ヘッダとContentType両方に設定されているケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getHeader("Content-Type");
            result = "type";
            nablarchHttpServletRequestWrapper.getContentType();
            result = "a";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        assertThat("Content-Typeを取得できること", sut.getContentType(), is("type"));
    }

    /**
     * Content-Typeをリクエストから取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetContentType_contentType() throws Exception {

        // ContentTypeにのみ設定されているケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getContentType();
            result = "type";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        assertThat("Content-Typeを取得できること", sut.getContentType(), is("type"));

        // ヘッダとContentType両方に設定されているケース
        new Expectations() {{
            nablarchHttpServletRequestWrapper.getContentType();
            result = "type";
            nablarchHttpServletRequestWrapper.getHeader("Content-Type");
            result = "a";
        }};
        assertThat("Content-Typeを取得できること", sut.getContentType(), is("type"));
    }

    /**
     * Content-Typeが未設定の場合にnullが返されることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetContentType_null() throws Exception {
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);
        assertThat("Content-Typeを取得できること", sut.getContentType(), is(nullValue()));
    }

    /**
     * Content-Lengthを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetContentLength() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getContentLength();
            result = 10;
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        assertThat("Content-Lengthを取得できること", sut.getContentLength(), is(10));
    }

    /**
     * エンコーディングを取得できることを確認
     *
     * @throws Exception
     */
    @Test
    public void testGetCharacterEncoding() throws Exception {

        new Expectations() {{
            nablarchHttpServletRequestWrapper.getCharacterEncoding();
            result = "UTF-8";
        }};
        sut = new HttpRequestWrapper(nablarchHttpServletRequestWrapper);

        assertThat("エンコーディングを取得できること", sut.getCharacterEncoding(), is("UTF-8"));
    }

    /**
     * {@link UserAgentParser}のカスタム実装。
     */
    public static class CustomUserAgentParser implements UserAgentParser {
        @Override
        public UserAgent parse(String userAgentText) {
            return new UserAgent("custom");
        }
    }
}