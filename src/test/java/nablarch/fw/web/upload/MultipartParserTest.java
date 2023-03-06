package nablarch.fw.web.upload;

import nablarch.core.util.FilePathSetting;
import nablarch.fw.results.BadRequest;
import nablarch.fw.results.RequestEntityTooLarge;
import nablarch.fw.web.servlet.MockServletInputStream;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nablarch.fw.web.upload.UploadTestUtil.readAll;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * {@link MultipartParser}のテスト。<br/>
 *
 * @author T.Kawasaki
 */
public class MultipartParserTest {

    /** 一時ディレクトリ */
    private static final File TEMP_DIR = new File(
            System.getProperty("java.io.tmpdir") + '/' + MultipartParserTest.class.getSimpleName());

    private UploadSettings settings = new UploadSettings();

    private Map<String, String[]> paramMap = new HashMap<String, String[]>();

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @BeforeClass
    public static void setUp() {
        FilePathSetting.getInstance().addBasePathSetting(UploadSettings.UPLOAD_FILE_TMP_DIR, TEMP_DIR.toURI().toString());
        clean();
    }

    @After
    public void after() {
        clean();
    }

    private static void clean() {
        if (TEMP_DIR.exists()) {
            for (File e : TEMP_DIR.listFiles()) {
                e.delete();
            }
        } else {
            TEMP_DIR.mkdir();
        }
        assertThat(TEMP_DIR.listFiles().length, is(0));
    }

    /**
     * 正常に解析できること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testParse() throws IOException {
        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("req.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469; charset=utf-8";
        int contentLength = 338;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        PartInfoHolder multipart = target.parse();

        // アップロードファイル
        PartInfo part = multipart.get("uploadfile1").get(0);
        assertThat(part.getName(), is("uploadfile1"));
        assertThat(part.getFileName(), is("upload.txt"));
        assertThat(part.size(), is(42));
        assertThat(TEMP_DIR.listFiles().length, is(1));

        // リクエストパラメータ
        assertThat(paramMap.size(), is(2));
        assertThat(paramMap.get("username")[0], is("hoge"));
        assertThat(paramMap.containsKey("uploadfile1"), is(true));

        multipart.cleanup();
        assertThat(TEMP_DIR.listFiles().length, is(0));
    }


    /**
     * ファイル名にフルパスが送信された場合でも正常に解析できること。
     * ※古いブラウザにはファイル名にフルパスを送信するものがある。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testParseFullPath() throws IOException {
        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("fullpath.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469";
        int contentLength = -1;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        PartInfoHolder multipart = target.parse();

        // アップロードファイル
        PartInfo part = multipart.get("uploadfile1").get(0);
        assertThat(part.getName(), is("uploadfile1"));
        assertThat(part.getFileName(), is("upload.txt"));   // ファイル名(フルパスでない)
        assertThat(part.size(), is(42));
        assertThat(TEMP_DIR.listFiles().length, is(1));
        assertTrue(TEMP_DIR.listFiles()[0].getName().matches("^\\d+?[a-z0-9]{10}.txt$"));

        // リクエストパラメータ
        assertThat(paramMap.size(), is(2));
        assertThat(paramMap.get("username")[0], is("hoge"));
        assertThat(paramMap.containsKey("uploadfile1"), is(true));

        multipart.cleanup();
        assertThat(TEMP_DIR.listFiles().length, is(0));
    }

    /** マルチパートでないContent-Typeの判定ができること。 */
    @Test
    public void testNotMultipart() {
        assertThat(MultipartParser.isMultipart("aaa"), is(false));
        assertThat(MultipartParser.isMultipart(null), is(false));
    }

    /** 境界文字列の抽出ができること。 */
    @Test
    public void testExtractBoundary() {
        assertThat(MultipartParser.extractBoundary(
                "multipart/form-data; boundary=---------------------------2394118477469"), is(
                "-----------------------------2394118477469"));
        assertThat(MultipartParser.extractBoundary(
                "multipart/form-data; boundary=\"---------------------------2394118477469\""), is(
                "-----------------------------2394118477469"));
    }

    /** 境界文字列が無い場合、例外が発生すること。 */
    @Test(expected = BadRequest.class)
    public void testExtractBoundaryNotFound() {
        MultipartParser.extractBoundary("multipart/form-data;");
    }


    /**
     * Content-Length0のとき、マルチパートの解析がされないこと。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testEmpty() throws IOException {
        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("req.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469";
        int contentLength = 0;
        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        PartInfoHolder multipart = target.parse();
        assertThat(multipart.isEmpty(), is(true));
    }

    /**
     * 大きなファイルが解析できること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testLargeFile() throws IOException {
        final int size = 1024 * 1024;
        File f = createFile(size, createBoundaryHeader(), createBoundaryFooter());

        MockServletInputStream in = new MockServletInputStream(new BufferedInputStream(new FileInputStream(f)));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469";
        int contentLength = size + 10;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        PartInfoHolder multipart = target.parse();

        // アップロードファイル
        PartInfo part = multipart.get("uploadfile1").get(0);
        assertThat(part.getName(), is("uploadfile1"));
        assertThat(part.getFileName(), is("upload.txt"));
        assertThat(TEMP_DIR.listFiles().length, is(1));
        assertTrue(TEMP_DIR.listFiles()[0].getName().matches("^\\d+?[a-z0-9]{10}.txt$"));

        // リクエストパラメータ
        assertThat(paramMap.size(), is(1));

        multipart.cleanup();
        assertThat(TEMP_DIR.listFiles().length, is(0));
    }

    /**
     * 解析途中でストリームが終了した場合、例外が発生すること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testParseFailUnexpectedEof() throws IOException {
        MockServletInputStream in = new MockServletInputStream(new ByteArrayInputStream(createBoundaryHeader().getBytes()));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469";
        int contentLength = -1;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        try {
            target.parse();
            fail();
        } catch (BadRequest e) {
            assertThat(e.getMessage(),
                    containsString("input stream unexpectedly ended before boundary appears."));
        }

        assertThat("例外発生時には、一時ファイルがクリーニングされていること", TEMP_DIR.list().length, is(0));
    }

    /**
     * ストリームが空である場合に、例外が発生すること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testParseNoData() throws IOException {
        MockServletInputStream in = new MockServletInputStream(new ByteArrayInputStream(new byte[0]));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469";
        int contentLength = -1;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        try {
            target.parse();
            fail();
        } catch (BadRequest e) {
            assertThat(e.getMessage(), containsString("no data found."));
        }
        assertThat("上限超過時には、一時ファイルがクリーニングされていること", TEMP_DIR.list().length, is(0));
    }

    /**
     * Content-Length最大許容値を超過した場合、例外が発生すること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testParseOverLimit() throws IOException {

        final int size = 2 * 1024 * 1024;
        File f = createFile(size, createBoundaryHeader(), createBoundaryFooter());

        MockServletInputStream in = new MockServletInputStream(new BufferedInputStream(new FileInputStream(f)));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469";
        int contentLength = size + 10;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        settings.setContentLengthLimit(1024);
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        try {
            target.parse();
            fail();
        } catch (RequestEntityTooLarge e) {
            assertThat(e.getStatusCode(), is(413));
            assertThat(e.getMessage(), is("The request you send was refused because the request entity is larger than the server is willing or able to process."));
        }

        assertThat("上限超過時には、一時ファイルがクリーニングされていること", TEMP_DIR.list().length, is(0));

    }

    /**
     * ヘッダ行のサイズが最大許容値を超過した場合、例外が発生すること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testHeaderSizeLimit() throws IOException {

        final int size = 1024 * 1024;
        File f = createFile(size, createLargeBoundaryHeader(), createBoundaryFooter());

        MockServletInputStream in = new MockServletInputStream(new BufferedInputStream(new FileInputStream(f)));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469";
        int contentLength = size + 10;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        try {
            target.parse();
            fail();
        } catch (BadRequest e) {
            assertThat(e.getStatusCode(), is(400));
            assertThat(e.getMessage(), containsString("header record size is too large."));
        }
    }

    /**
     * 日本語ファイル名でかつヘッダが \r\n がジャスト4KB（バッファサイズ）に収まるケース
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void test4KB() throws IOException {
        PartInfo part = testForSizeVariation("4k.dat", "uploadfile");
        String fileName = part.getFileName();
        assertThat(fileName.getBytes("Windows-31J").length, is(4032));
    }

    /**
     * 日本語ファイル名でかつヘッダが \r\n がジャスト4KB + 1Bに収まるケース
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void test4097B() throws IOException {
        PartInfo part =testForSizeVariation("4k+1.dat", "uploadfile1");
        String fileName = part.getFileName();
        assertThat(fileName.getBytes("Windows-31J").length, is(4032));
    }

    /**
     * 日本語ファイル名でかつ、ファイル名のマルチバイト文字がバッファ途中で途切れるケース。<br/>
     * （バッファ（4KB）の最終バイトと次のバイトで一文字）
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testFileNameSplit() throws IOException {
        PartInfo part = testForSizeVariation("fileNameSplit.dat", "uploadfile2");
        String fileName = part.getFileName();
        assertThat(fileName.getBytes("Windows-31J").length, is(4034));
    }

    /**
     * アップロードファイル数の上限を超える数のファイルを受け取った場合はエラーとなること。
     */
    @Test
    public void testMaxFileCountThrowsExceptionIfOverMaxCount() {
        settings.setMaxFileCount(2);
        
        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("testMaxFileCountThrowsExceptionIfOverMaxCount.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469; charset=utf-8";
        int contentLength = 646;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        final MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        BadRequest exception = assertThrows(BadRequest.class, new ThrowingRunnable() {
            @Override
            public void run() {
                target.parse();
            }
        });
        
        assertThat(exception.getMessage(), is("The uploaded file count is over than max count."));
    }

    /**
     * アップロードファイル数の上限と同じ数のファイルを受け取った場合はエラーとならないこと。
     */
    @Test
    public void testMaxFileCountNotThrowsExceptionIfEqualsToMaxCount() {
        settings.setMaxFileCount(2);

        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("testMaxFileCountNotThrowsExceptionIfEqualsToMaxCount.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469; charset=utf-8";
        int contentLength = 481;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        final MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        final PartInfoHolder holder = target.parse();

        final PartInfo uploadfile1 = holder.get("uploadfile1").get(0);
        assertThat(uploadfile1.getFileName(), is("upload1.txt"));

        final PartInfo uploadfile2 = holder.get("uploadfile2").get(0);
        assertThat(uploadfile2.getFileName(), is("upload2.txt"));

        assertThat(paramMap.get("username")[0], is("hoge"));
    }

    /**
     * アップロードファイル数の上限より少ない数のファイルを受け取った場合はエラーとならないこと。
     */
    @Test
    public void testMaxFileCountNotThrowsExceptionIfLessThanMaxCount() {
        settings.setMaxFileCount(2);

        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("testMaxFileCountNotThrowsExceptionIfLessThanMaxCount.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469; charset=utf-8";
        int contentLength = 314;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        final MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        final PartInfoHolder holder = target.parse();

        final PartInfo uploadfile1 = holder.get("uploadfile1").get(0);
        assertThat(uploadfile1.getFileName(), is("upload1.txt"));

        assertThat(paramMap.get("username")[0], is("hoge"));
    }

    /**
     * アップロードファイル数の上限が未設定だった場合、デフォルト1000が設定されることのテスト（エラーケース）。
     */
    @Test
    public void testMaxFileCountDefaultSettingsOverMaxCount() {
        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("testMaxFileCountDefaultSettingsOverMaxCount.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469; charset=utf-8";
        int contentLength = 169000;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        final MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        BadRequest exception = assertThrows(BadRequest.class, new ThrowingRunnable() {
            @Override
            public void run() {
                target.parse();
            }
        });

        assertThat(exception.getMessage(), is("The uploaded file count is over than max count."));
    }

    /**
     * アップロードファイル数の上限が未設定だった場合、デフォルト1000が設定されることのテスト（非エラーケース）。
     */
    @Test
    public void testMaxFileCountDefaultSettingsNoError() {
        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("testMaxFileCountDefaultSettingsNoError.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469; charset=utf-8";
        int contentLength = 168828;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        final MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        final PartInfoHolder holder = target.parse();

        assertThat(holder.getDelegateMap().size(), is(1000));

        assertThat(paramMap.get("username")[0], is("hoge"));
    }

    /**
     * アップロードファイル数の上限に0が設定されている場合、1件でもファイルがあればエラーになること。
     */
    @Test
    public void testMaxFileCountIfSetZero() {
        settings.setMaxFileCount(0);

        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("testMaxFileCountIfSetZero.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469; charset=utf-8";
        int contentLength = 314;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        final MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        BadRequest exception = assertThrows(BadRequest.class, new ThrowingRunnable() {
            @Override
            public void run() {
                target.parse();
            }
        });

        assertThat(exception.getMessage(), is("The uploaded file count is over than max count."));
    }

    /**
     * アップロードファイル数の上限に負数が設定されている場合、上限無しになること。
     * <p>
     * 無限個のファイルをアップロードすることはできないので、デフォルト値よりも
     * 大きい数をアップロードしてエラーにならないことを確認することで、
     * 上限無しになっているものと判断する。
     * </p>
     */
    @Test
    public void testMaxFileCountIfSetMinus() {
        settings.setMaxFileCount(-1);
        
        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream("testMaxFileCountIfSetMinus.dat"));
        String contentType = "multipart/form-data; boundary=---------------------------2394118477469; charset=utf-8";
        int contentLength = 254828;

        MultipartContext ctx = new MultipartContext(contentType, contentLength, "UTF-8");
        final MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);

        final PartInfoHolder holder = target.parse();

        assertThat(holder.getDelegateMap().size(), is(1500));

        assertThat(paramMap.get("username")[0], is("hoge"));
    }

    /**
     * サイズバリエーションのテストを行う。
     *
     * @param resourceName ヘッダに使う入力リソース名
     * @param name アップロードファイル名のname属性
     * @return パート情報
     * @throws IOException 予期しない例外
     */
    PartInfo testForSizeVariation(String resourceName, String name) throws IOException {

        String contentType = "multipart/form-data; boundary=---------------------------2394118477469";
        MockServletInputStream in = new MockServletInputStream(
                getClass().getResourceAsStream(resourceName));
        int contentLength = -1;
        final String encoding = "Windows-31J";
        MultipartContext ctx = new MultipartContext(contentType, contentLength, encoding);
        MultipartParser target = new MultipartParser(in, paramMap, settings, ctx);
        // 実行
        PartInfoHolder multipart = target.parse();

        // アップロードファイル
        assertThat(multipart.toString(), multipart.size(), is(1));
        PartInfo part = multipart.get(name).get(0);
        assertThat(part.getName(), is(name));
        assertThat(TEMP_DIR.listFiles().length, is(1));

        List<String> lines = readAll(part.getInputStream(), encoding);
        assertThat(lines.size(), is(1));
        assertThat(lines.get(0), is("ファイルの中身です。"));
        return part;
    }

    /**
     * ファイルを生成する。
     *
     * @param fileSize ファイルサイズ
     * @param header ヘッダ文字列
     * @param footer フッタ文字列
     * @return ファイル
     * @throws IOException ファイル生成失敗時に送出する例外
     */
    private File createFile(int fileSize, String header, String footer) throws IOException {
        int rest = fileSize - header.length() - footer.length();
        File file = File.createTempFile(getClass().getSimpleName(), ".tmp");
        file.deleteOnExit();
        Writer writer = new BufferedWriter(new FileWriter(file));
        try {
            writer.write(header);

            for (int i = 1; i <= rest; i++) {
                writer.append('a');
                if (i % 100 == 0 && i != rest) {
                    writer.append(LINE_SEPARATOR);
                    i++;
                }
            }
            writer.append(LINE_SEPARATOR);
            writer.write(footer);
            return file;
        } finally {
            writer.close();
        }
    }

    /**
     * ヘッダ文字列を生成する。
     *
     * @return ヘッダ文字列
     */
    private String createBoundaryHeader() {
        return new StringBuilder()
                .append(LINE_SEPARATOR)
                .append("-----------------------------2394118477469")
                .append(LINE_SEPARATOR)
                .append("Content-Disposition: form-data; name=\"uploadfile1\"; filename=\"upload.txt\"")
                .append(LINE_SEPARATOR)
                .append("Content-Type: text/plain")
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR).toString();
    }

    /**
     * 許容値を超えたヘッダ文字列を生成する。
     *
     * @return ヘッダ文字列
     */
    private String createLargeBoundaryHeader() {
        StringBuilder sb = new StringBuilder()
                .append(LINE_SEPARATOR)
                .append("-----------------------------2394118477469")
                .append(LINE_SEPARATOR)
                .append("Content-Disposition: form-data; name=\"uploadfile1\"; filename=\"upload.txt\"");

        for (int i = 0; i < 2048; i++) {
            sb.append("          ");
        }

        return sb.append(LINE_SEPARATOR)
                .append("Content-Type: text/plain")
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR).toString();
    }

    /**
     * フッタ文字列を生成する。
     *
     * @return フッタ文字列
     */
    private String createBoundaryFooter() {
        return "-----------------------------2394118477469--" + LINE_SEPARATOR;
    }
}

