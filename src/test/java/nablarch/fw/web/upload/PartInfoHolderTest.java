package nablarch.fw.web.upload;

import nablarch.test.core.log.LogVerifier;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link PartInfoHolder}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class PartInfoHolderTest {

    /** 一時フォルダ */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** テスト対象 */
    private PartInfoHolder target = new PartInfoHolder();

    /**
     * 全てのパート情報がログ出力されること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testLogAllPart() throws IOException {
        PartInfo part1 = PartInfo.newInstance(Arrays.asList(
                "Content-Disposition: form-data; name=\"uploadfile1\"; filename=\"myFile1_ja.txt\"",
                "Content-Type: text/plain"));
        part1.setSize(1024);
        part1.getOutputStream(folder.getRoot()).close();
        target.addPart(part1);

        PartInfo part2 = PartInfo.newInstance(Arrays.asList(
                "Content-Disposition: form-data; name=\"uploadfile2\"; filename=\"myFile2_ja.xml\"",
                "Content-Type: text/xml"));

        part2.setSize(512);
        part2.getOutputStream(folder.getRoot()).close();
        target.addPart(part2);

        LogVerifier.setExpectedLogMessages(createExpectedLogs(new String[][]{
                {"INFO", "2 file(s) uploaded."},
                {"INFO", "name='uploadfile1', fileName='myFile1_ja.txt', contentType='text/plain', size=1024" },
                {"INFO", "name='uploadfile2', fileName='myFile2_ja.xml', contentType='text/xml', size=512"}
        }));
        target.logAllPart();
        LogVerifier.verify("期待したログが出力されませんでした。");
    }

    @After
    public  void after() {
        LogVerifier.clear();
    }

    private List<Map<String, String>> createExpectedLogs(String[][] arg) {
                List<Map<String, String>> expectedLogs = new ArrayList<Map<String, String>>();
        for (int i = 0, argLength = arg.length; i < argLength; i++) {
            String[] e = arg[i];
            Map<String, String> m = new HashMap<String, String>();
            m.put("logLevel", e[0]);
            m.put("message1", e[1]);
            expectedLogs.add(m);
        }
        return expectedLogs;
    }
}
