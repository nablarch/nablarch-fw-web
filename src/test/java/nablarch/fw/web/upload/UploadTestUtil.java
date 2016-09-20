package nablarch.fw.web.upload;

import nablarch.core.util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * アップロードのテスト用ユーティリティクラス。
 *
 * @author T.Kawasaki
 */
class UploadTestUtil {
    /**
     * 入力ストリームから行単位で文字列を読み込む。
     * @param in 入力ストリームから
     * @param charset エンコーディング
     * @return 読み込んだ文字列
     */
    static List<String> readAll(InputStream in, String charset) {
        List<String> result = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
            String line = reader.readLine();
            while (line != null) {
                result.add(line);
                line = reader.readLine();
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(in);
        }
    }
}
