package nablarch.fw.web;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.hamcrest.text.IsEmptyString;
import org.junit.Ignore;
import org.junit.Test;

/**
 * {@link ResourceLocator}のテストクラス
 */
public class ResourceLocatorFunctionalTest {

    /**
     * コンテンツパスがhttpを示すパスの場合のテスト
     */
    @Test
    public void httpContent() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf("http://yourhost.com/hoge/fuga/image.png");

        assertThat(sut, allOf(
                hasProperty("scheme", is("http")),
                hasProperty("resourceName", is("image.png")),
                hasProperty("redirect", is(true)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", is("yourhost.com")),
                hasProperty("directory", is("/hoge/fuga/")),
                hasProperty("path", is("/hoge/fuga/image.png"))
        ));
        assertThat("httpでは存在チェックが無効(false)", sut.exists(), is(false));
        assertThat(sut.toString(), is("http://yourhost.com/hoge/fuga/image.png"));


        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * コンテンツパスがhttpsを示すパスの場合のテスト
     */
    @Test
    public void httpsContent() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf("https://yourhost.com/hoge/fuga/content?aa=bb#aa=bb");

        assertThat(sut, allOf(
                hasProperty("scheme", is("https")),
                hasProperty("resourceName", is("content?aa=bb#aa=bb")),
                hasProperty("redirect", is(true)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", is("yourhost.com")),
                hasProperty("directory", is("/hoge/fuga/")),
                hasProperty("path", is("/hoge/fuga/content?aa=bb#aa=bb"))
        ));
        assertThat("httpsでは存在チェックが無効(false)", sut.exists(), is(false));
        assertThat(sut.toString(), is("https://yourhost.com/hoge/fuga/content?aa=bb#aa=bb"));

        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * コンテンツパスがhttpの場合で、形式が不正な場合のテスト
     * @throws Exception
     */
    @Test
    public void invalidHttpContent() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf("http://");

        assertThat(sut, allOf(
                hasProperty("scheme", is("http")),
                hasProperty("resourceName", is("")),
                hasProperty("redirect", is(true)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", is("")),
                hasProperty("directory", is("")),
                hasProperty("path", is(""))
        ));
        
        assertThat("httpでは存在チェックが無効(false)", sut.exists(), is(false));
        assertThat(sut.toString(), is("http://"));
        
        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * デフォルトのSchemeの場合のテスト
     *
     * @throws Exception
     */
    @Test
    public void defaultSchemeContent() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf("/WEB-INF/users/index.jsp");

        assertThat(sut, allOf(
                hasProperty("scheme", is("servlet")),
                hasProperty("resourceName", is("index.jsp")),
                hasProperty("path", is("/WEB-INF/users/index.jsp")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("/WEB-INF/users/"))
        ));
        assertThat("servletの場合は常にtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("servlet:///WEB-INF/users/index.jsp"));

        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * servlet schemeで絶対パスの場合
     */
    @Test
    public void servletContentWithAbsolute() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf("servlet:///WEB-INF/users/index.jsp");

        assertThat(sut, allOf(
                hasProperty("scheme", is("servlet")),
                hasProperty("resourceName", is("index.jsp")),
                hasProperty("path", is("/WEB-INF/users/index.jsp")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("/WEB-INF/users/"))
        ));
        assertThat("servletの場合は常にtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("servlet:///WEB-INF/users/index.jsp"));
        
        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * fowrard schemeの場合
     */
    @Test
    public void forwardContent() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf("forward://index");

        assertThat(sut, allOf(
                hasProperty("scheme", is("forward")),
                hasProperty("resourceName", is("index")),
                hasProperty("path", is("index")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(true)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", isEmptyString())
        ));
        assertThat("forwardの場合は常にtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("forward://index"));

        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * redirect schemeの場合
     *
     * @throws Exception
     */
    @Test
    public void redirectContent() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf("redirect:///action/menu");

        assertThat(sut, allOf(
                hasProperty("scheme", is("redirect")),
                hasProperty("resourceName", is("menu")),
                hasProperty("path", is("/action/menu")),
                hasProperty("redirect", is(true)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("/action/"))
        ));
        assertThat("redirectの場合は常にfalse", sut.exists(), is(false));
        assertThat(sut.toString(), is("redirect:///action/menu"));

        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * servlet shemeで相対パスの場合
     */
    @Test
    public void servletContentWithRelative() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf("servlet://users/index.jsp");

        assertThat(sut, allOf(
                hasProperty("scheme", is("servlet")),
                hasProperty("resourceName", is("index.jsp")),
                hasProperty("path", is("users/index.jsp")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(true)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("users/"))
        ));
        assertThat("デフォルト(servlet)の場合は常にtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("servlet://users/index.jsp"));

        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * classpath schemeの場合
     */
    @Test
    public void classpathContent() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf(
                "classpath:///nablarch/fw/web/resourceLocator/classpathResource.txt");

        assertThat(sut, allOf(
                hasProperty("scheme", is("classpath")),
                hasProperty("resourceName", is("classpathResource.txt")),
                hasProperty("path", is("/nablarch/fw/web/resourceLocator/classpathResource.txt")),
                hasProperty("realPath", containsString("nablarch/fw/web/resourceLocator/classpathResource.txt")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("/nablarch/fw/web/resourceLocator/"))
        ));
        assertThat("存在しているのでtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("classpath:///nablarch/fw/web/resourceLocator/classpathResource.txt"));

        assertThat(readAll(sut.getReader()), is("1"));
        assertThat(readAll(sut.getInputStream()), is("1"));
    }

    /**
     * classpath schemeで存在しないリソース
     */
    @Test
    public void classpathContentNotExists() throws Exception {
        final ResourceLocator sut = ResourceLocator.valueOf(
                "classpath:///nablarch/fw/web/resourceLocator/not_exists.txt");

        assertThat(sut, allOf(
                hasProperty("scheme", is("classpath")),
                hasProperty("resourceName", is("not_exists.txt")),
                hasProperty("path", is("/nablarch/fw/web/resourceLocator/not_exists.txt")),
                hasProperty("realPath", is(nullValue())),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("/nablarch/fw/web/resourceLocator/"))
        ));
        assertThat("存在していないのでfalse", sut.exists(), is(false));
        assertThat(sut.toString(), is("classpath:///nablarch/fw/web/resourceLocator/not_exists.txt"));

        // ファイルが存在していないので例外が発生する
        try {
            sut.getReader();
            fail("");
        } catch (FileNotFoundException ignore) {
        }
        try {
            sut.getInputStream();
            fail("");
        } catch (FileNotFoundException ignore) {
        }
    }

    /**
     * file schemeの場合
     */
    @Test
    public void fileContent() throws Exception {

        final ResourceLocator sut = ResourceLocator.valueOf(
                "file://src/test/resources/nablarch/fw/web/resourceLocator/classpathResource.txt");

        assertThat(sut, allOf(
                hasProperty("scheme", is("file")),
                hasProperty("resourceName", is("classpathResource.txt")),
                hasProperty("path", is("src/test/resources/nablarch/fw/web/resourceLocator/classpathResource.txt")),
                hasProperty("realPath", containsString("classpathResource.txt")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(true)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("src/test/resources/nablarch/fw/web/resourceLocator/"))
        ));
        assertThat("存在している", sut.exists(), is(true));
        assertThat(sut.toString(),
                is("file://src/test/resources/nablarch/fw/web/resourceLocator/classpathResource.txt"));

        assertThat(readAll(sut.getReader()), is("1"));
        assertThat(readAll(sut.getInputStream()), is("1"));
    }

    /**
     * file schemeでディレクトリを指定した場合
     */
    @Test
    public void directoryContent() throws Exception {

        final ResourceLocator sut = ResourceLocator.valueOf(
                "file://src/test/resources/nablarch/fw/web/resourceLocator");

        assertThat(sut, allOf(
                hasProperty("scheme", is("file")),
                hasProperty("resourceName", is("resourceLocator")),
                hasProperty("path", is("src/test/resources/nablarch/fw/web/resourceLocator")),
                hasProperty("realPath", containsString("resourceLocator")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(true)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("src/test/resources/nablarch/fw/web/"))
        ));
        assertThat("ディレクトリはファイルとして扱えないのでfalse", sut.exists(), is(false));
        assertThat(sut.toString(),
                is("file://src/test/resources/nablarch/fw/web/resourceLocator"));

        // ディレクトリなのでアクセスできない
        try {
            sut.getReader();
            fail("");
        } catch (FileNotFoundException ignore) {
        }
        try {
            sut.getInputStream();
            fail("");
        } catch (FileNotFoundException ignore) {
        }

    }

    /**
     * file schemeで存在しないファイルを指定した場合
     */
    @Test
    public void fileContentNotExists() throws Exception {

        final ResourceLocator sut = ResourceLocator.valueOf(
                "file://src/test/resources/nablarch/fw/web/resourceLocator/notExists");

        assertThat(sut, allOf(
                hasProperty("scheme", is("file")),
                hasProperty("resourceName", is("notExists")),
                hasProperty("path", is("src/test/resources/nablarch/fw/web/resourceLocator/notExists")),
                hasProperty("realPath", containsString("notExists")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(true)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("src/test/resources/nablarch/fw/web/resourceLocator/"))
        ));
        assertThat("存在しないファイルなのでfalse", sut.exists(), is(false));
        assertThat(sut.toString(),
                is("file://src/test/resources/nablarch/fw/web/resourceLocator/notExists"));

        // 存在しないファイルなのでアクセスできない
        try {
            sut.getReader();
            fail("");
        } catch (FileNotFoundException ignore) {
        }
        try {
            sut.getInputStream();
            fail("");
        } catch (FileNotFoundException ignore) {
        }

    }

    /**
     * 許可されないスキームを指定した場合
     */
    @Test
    public void disallowdScheme() {
        String scheme = "disallowed";
        assertThat(ResourceLocator.SCHEMES, not(containsString(scheme)));
        try {
            String path = scheme + "://foo/bar";
            ResourceLocator.valueOf(path);
            fail();
        } catch (HttpErrorResponse expected) {
            assertThat(expected.getResponse().getStatusCode(), is(400));
        }
    }

    /**
     * ディレクトリが許可されない文字を含んでいた場合
     */
    @Test
    public void httpContentDisallowedDirectory() {
        String directory = "x..z";
        assertThat(ResourceLocator.ALLOWED_CHAR.matcher(directory).matches(), is(false));
        try {
            String path = "http://yourhost.com/" + directory + "/index.html";
            ResourceLocator.valueOf(path);
            fail();
        } catch (HttpErrorResponse expected) {
            assertThat(expected.getResponse().getStatusCode(), is(400));
        }
    }

    /**
     * redirect schemeで絶対URLを指定した場合
     */
    @Ignore("5u12の仕様ではパスしないテストケース。リダイレクトの仕様変更にあわせてテストを実施するため、@Ignoreを付けている。")
    @Test
    public void redirectContentAbsoluteURL() {
        final ResourceLocator sut = ResourceLocator.valueOf("redirect:http://action/menu");

        assertThat(sut, allOf(
                hasProperty("scheme", is("redirect")),
                hasProperty("resourceName", isEmptyString()),
                hasProperty("path", is("http://action/menu")),
                hasProperty("redirect", is(true)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", isEmptyString())
        ));
        assertThat("redirectの場合は常にfalse", sut.exists(), is(false));
        assertThat(sut.toString(), is("redirect:http://action/menu"));

        invalidOperationWithoutClasspathAndFileScheme(sut);
    }

    /**
     * redirect schemeで{@link ResourceLocator#SCHEMES 対応するスキーム名}以外のURIを指定した場合
     */
    @Ignore("5u12の仕様ではパスしないテストケース。リダイレクトの仕様変更にあわせてテストを実施するため、@Ignoreを付けている。")
    @Test
    public void redirectContentUnknownSchemes() {
        final ResourceLocator sut = ResourceLocator.valueOf("redirect:URN:ISBN:978-4-7741-6931-6");

        assertThat(sut, allOf(
                hasProperty("scheme", is("redirect")),
                hasProperty("resourceName", isEmptyString()),
                hasProperty("path", is("URN:ISBN:978-4-7741-6931-6")),
                hasProperty("redirect", is(true)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", isEmptyString())
        ));
        assertThat("redirectの場合は常にfalse", sut.exists(), is(false));
        assertThat(sut.toString(), is("redirect:URN:ISBN:978-4-7741-6931-6"));

        invalidOperationWithoutClasspathAndFileScheme(sut);
    }
    
    /**
     * classpathとfile scheme以外で不正なオペレーションのアサートをする
     *
     * @param sut
     */
    private void invalidOperationWithoutClasspathAndFileScheme(ResourceLocator sut) {
        // httpではrealPathは不正な操作
        try {
            sut.getRealPath();
            fail("");
        } catch (UnsupportedOperationException ignore) {
        }

        // httpではgetReaderは不正な操作
        try {
            sut.getReader();
            fail();
        } catch (FileNotFoundException ignore) {
        }

        // httpではgetInputStreamは不正な操作
        try {
            sut.getInputStream();
            fail();
        } catch (FileNotFoundException ignore) {
        }
    }

    private String readAll(InputStream inputStream) throws Exception {
        return readAll(new InputStreamReader(inputStream, Charset.forName("utf-8")));
    }

    private String readAll(Reader reader) throws Exception {
        try {
            final BufferedReader bufferedReader = new BufferedReader(reader);
            return bufferedReader.readLine();
        } finally {
            reader.close();
        }
    }
}