package nablarch.fw.web.upload;

import nablarch.fw.results.BadRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;


/**
 * @author T.Kawasaki
 */
public class PartInfoTest {

    /** 一時フォルダ */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Content-Dispositionの指定が不正である場合に、例外が発生すること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testContentDispositionFail() throws IOException {
        // 値が無い。
        trySetContentDisposition("content-disposition:")
                .shouldFailWithMessage("; not found.");
        // form-dataがない（form-date）
        trySetContentDisposition("content-disposition: form-date;")
                .shouldFailWithMessage("form-data expected");
        // form-data以降のnameがない
        trySetContentDisposition("Content-Disposition: form-data; ")
                .shouldFailWithMessage("name not found.");

    }

    /**
     * Content-Dispositionの設定が正しくできること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testContentDisposition() throws IOException {

        // name

        PartInfo target = trySetContentDisposition(
                "Content-Disposition: form-data; name=uploadfile1")
                .getResult();
        assertThat(target.getName(), is("uploadfile1"));

        target = trySetContentDisposition(
                "Content-Disposition: form-data; name=uploadfile1;")
                .getResult();
        assertThat(target.getName(), is("uploadfile1"));

        target = trySetContentDisposition(
                "Content-Disposition: form-data; name=")
                .getResult();
        assertThat(target.getName(), is(""));

        target = trySetContentDisposition(
                "Content-Disposition: form-data; name=\"hoge")
                .getResult();
        assertThat(target.getName(), is("\"hoge"));

        target = trySetContentDisposition(
                "Content-Disposition: form-data; name=\"foo\"")
                .getResult();
        assertThat(target.getName(), is("foo"));

        // filename

        target = trySetContentDisposition(
                "Content-Disposition: form-data; name=\"uploadfile1\"; filename=")
                .getResult();
        assertNull(target.getFileName());

        target = trySetContentDisposition(
                "Content-Disposition: form-data; name=\"uploadfile1\"; filename=\"upload.txt")
                .getResult();
        assertNull(target.getFileName());

        target = trySetContentDisposition(
                "Content-Disposition: form-data; name=\"uploadfile1\"; filename=\"upload.txt\"")
                .getResult();
        assertThat(target.getFileName(), is("upload.txt"));
    }

    /**
     * Content-Typeの設定が正しくできること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testContentType() throws IOException {

        assertThat(PartInfo.newInstance(
            Arrays.asList("content-type: testType")).getContentType(), is("testType"));
        assertThat(PartInfo.newInstance(
            Arrays.asList("content-type: testType;")).getContentType(), is("testType"));

        // 不正なヘッダの場合はデフォルトが返る。
        assertThat(PartInfo.newInstance(
            Arrays.asList("content-type2: testType;")).getContentType(), is("application/octet-stream"));
    }

    @Test
    public void testFileNotFoundException() {
        PartInfo info = PartInfo.newInstance("");
        info.setSavedFile(new File("unknown.file"));
        try {
            info.getInputStream();
            fail();
        } catch (nablarch.fw.results.InternalError e) {
            assertTrue(e.getMessage().contains("opening upload file failed."));
            assertThat(e.getCause().getClass().getSimpleName(), is(FileNotFoundException.class.getSimpleName()));
        }
    }

    @Test
    public void testIllegalInvoking() throws Exception {
        PartInfo info =
                PartInfo.newInstance(
                        Arrays.asList("Content-Disposition: form-data; name=\"uploadfile1\"; filename=\"upload.txt\""));
        info.clean(); // いきなりclean呼んでも大丈夫
        try {
            info.setSize(-99);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("size must not be negative. [-99]"));
        }
        try {
            info.setSize(-99);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("size must not be negative. [-99]"));
        }
        try {
            info.moveTo(folder.getRoot(), "hoge");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("upload file not saved yet."));
        }
        info.getOutputStream(folder.getRoot());
        try {
            info.getOutputStream(folder.getRoot());
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("already saved."));
        } finally {
            info.clean();
        }

        info = PartInfo.newInstance(Arrays.asList("Content-Disposition: form-data; name=\"uploadfile1\"; filename=\"upload.txt\""));
        OutputStream stream = info.getOutputStream(folder.getRoot());
        stream.write(49);
        stream.flush();
        stream.close();
        info.moveTo(folder.getRoot(), "hoge");
        try {
            info.moveTo(folder.getRoot(), "hoge");
            fail();
        } catch (RuntimeException e) {
            assertThat("移動済みなので例外が発生する", e.getMessage(), is("upload already removed."));
        }
    }

    /**
     * getSavedFileのテスト
     */
    @Test
    public void testGetSavedFile() throws Exception {
        PartInfo info = PartInfo.newInstance("test");
        File file = folder.newFile("hoge");
        info.setSavedFile(file);
        assertThat(info.getSavedFile(), is(file));
    }

    private Tester trySetContentDisposition(String line) {
        return new Tester(line);
    }

    private static class Tester {
        private String line;

        Tester(String line) {
            this.line = line;
        }

        PartInfo getResult() throws IOException {
            return invoke();
        }

        void shouldFailWithMessage(String expectedMsg) throws IOException {
            try {
                invoke();
                fail("expected exception not occurred.");
            } catch (BadRequest e) {
                assertThat(e.getMessage(), containsString(expectedMsg));
            }
        }

        private PartInfo invoke() throws IOException {
            return PartInfo.newInstance(Arrays.asList(line));
        }
    }
}
