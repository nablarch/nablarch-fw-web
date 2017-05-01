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
     * reidirect schemeの場合
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
     * classpath schemeでアーカイブファイルの中にある実在ファイルを指定する
     */
    @Test
    public void classpathContentInArchivedFile() throws Exception {
        ResourceLocator sut = ResourceLocator.valueOf("classpath://test.txt");

        assertThat(sut, allOf(
                hasProperty("scheme", is("classpath")),
                hasProperty("resourceName", is("test.txt")),
                hasProperty("path", is("test.txt")),
                hasProperty("realPath", containsString("test.txt")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", isEmptyString())
        ));
        assertThat("存在しているのでtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("classpath://test.txt"));

        assertThat(readAll(sut.getReader()), is("TestText"));
        assertThat(readAll(sut.getInputStream()), is("TestText"));
    }

    /**
     * classpath schemeでアーカイブファイルの中にある実在ファイルを指定する
     */
    @Test
    public void classpathDirContentInArchivedFile() throws Exception {
        ResourceLocator sut = ResourceLocator.valueOf("classpath://com/example/test-withDir.txt");

        assertThat(sut, allOf(
                hasProperty("scheme", is("classpath")),
                hasProperty("resourceName", is("test-withDir.txt")),
                hasProperty("path", is("com/example/test-withDir.txt")),
                hasProperty("realPath", containsString("com/example/test-withDir.txt")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("com/example/"))
        ));
        assertThat("存在しているのでtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("classpath://com/example/test-withDir.txt"));

        assertThat(readAll(sut.getReader()), is("DirTest"));
        assertThat(readAll(sut.getInputStream()), is("DirTest"));
    }

    /**
     * classpath schemeでアーカイブファイルの中にある実在ファイルを指定する
     */
    @Test
    public void classpathContentInZipFile() throws Exception {
        ResourceLocator sut = ResourceLocator.valueOf("classpath://a.txt");

        assertThat(sut, allOf(
                hasProperty("scheme", is("classpath")),
                hasProperty("resourceName", is("a.txt")),
                hasProperty("path", is("a.txt")),
                hasProperty("realPath", containsString("a.txt")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", isEmptyString())
        ));
        assertThat("存在しているのでtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("classpath://a.txt"));

        assertThat(readAll(sut.getReader()), is("Atext."));
        assertThat(readAll(sut.getInputStream()), is("Atext."));
    }

    /**
     * classpath schemeでアーカイブファイルの中にある実在ファイルを指定する
     */
    @Test
    public void classpathDirContentInZipFile() throws Exception {
        ResourceLocator sut = ResourceLocator.valueOf("classpath://ex/b.txt");

        assertThat(sut, allOf(
                hasProperty("scheme", is("classpath")),
                hasProperty("resourceName", is("b.txt")),
                hasProperty("path", is("ex/b.txt")),
                hasProperty("realPath", containsString("b.txt")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("ex/"))
        ));
        assertThat("存在しているのでtrue", sut.exists(), is(true));
        assertThat(sut.toString(), is("classpath://ex/b.txt"));

        assertThat(readAll(sut.getReader()), is("bText."));
        assertThat(readAll(sut.getInputStream()), is("bText."));
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
                "file:///src/test/resources/nablarch/fw/web/resourceLocator");

        assertThat(sut, allOf(
                hasProperty("scheme", is("file")),
                hasProperty("resourceName", is("resourceLocator")),
                hasProperty("path", is("/src/test/resources/nablarch/fw/web/resourceLocator")),
                hasProperty("realPath", containsString("resourceLocator")),
                hasProperty("redirect", is(false)),
                hasProperty("relative", is(false)),
                hasProperty("hostname", isEmptyString()),
                hasProperty("directory", is("/src/test/resources/nablarch/fw/web/"))
        ));
        assertThat("ディレクトリはファイルとして扱えないのでfalse", sut.exists(), is(false));
        assertThat(sut.toString(),
                is("file:///src/test/resources/nablarch/fw/web/resourceLocator"));

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