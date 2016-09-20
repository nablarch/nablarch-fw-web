package nablarch.fw.web.download.encorder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * {@link nablarch.fw.web.download.encorder.DownloadFileNameEncoder}の実装クラス。
 * <br>
 * ダウンロードファイル名をURLエンコード方式でエンコードするクラス。
 * 
 * @author Masato Inoue
 */
public class UrlDownloadFileNameEncoder implements DownloadFileNameEncoder {

    /**
     * 文字コード。デフォルトは「UTF-8」。
     */
    private String charset = "UTF-8";

    /**
     * 文字コードを設定する。
     * @param charset 文字コード
     * @return このエンコーダー
     */
    public UrlDownloadFileNameEncoder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    /**
     * ファイル名をURLエンコードする。
     * <br>
     * エンコードの際の文字コードには、charsetプロパティに設定された文字コードを使用する。
     * @param fileName ファイル名
     * @return エンコードされたファイル名
     */
    public String encode(String fileName) {
        
        try {
            return URLEncoder.encode(fileName, charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("not supported charset. charset=[" + charset + "]", e);
        } 
    }

}
