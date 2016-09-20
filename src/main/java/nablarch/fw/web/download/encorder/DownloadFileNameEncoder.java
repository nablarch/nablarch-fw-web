package nablarch.fw.web.download.encorder;

import nablarch.core.util.annotation.Published;

/**
 * ダウンロードファイル名をエンコードするインタフェース。
 * 
 * @author Masato Inoue
 */
@Published(tag = "architect")
public interface DownloadFileNameEncoder {

    /**
     * ダウンロードファイル名をエンコードする。
     * 
     * @param fileName ファイル名
     * @return エンコード後のファイル名
     */
    String encode(String fileName);
}
