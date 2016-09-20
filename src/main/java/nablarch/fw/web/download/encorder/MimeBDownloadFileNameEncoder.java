package nablarch.fw.web.download.encorder;

import java.io.UnsupportedEncodingException;

import nablarch.core.util.Base64Util;

/**
 * {@link nablarch.fw.web.download.encorder.DownloadFileNameEncoder}の実装クラス。
 * <br>
 * ダウンロードファイル名をRFC2047の仕様に従い、MIME-Bエンコード方式でエンコードするクラス。
 * 
 * @author Masato Inoue
 */
public class MimeBDownloadFileNameEncoder implements DownloadFileNameEncoder {
    /**
     * 文字コード。デフォルトは「UTF-8」。
     */
    private String charset = "UTF-8";

    /**
     * 文字コードを設定する。
     * @param charset 文字コード
     * @return このエンコーダー
     */
    public MimeBDownloadFileNameEncoder setCharset(String charset) {
        this.charset = charset;
        return this;
    }
    
    /**
     * RFC2047の仕様に従い、ファイル名をMIME-Bエンコードする。
     * <br>
     * エンコードの際の文字コードには、charsetプロパティに設定された文字コードを使用する。
     * @param fileName ファイル名
     * @return エンコードされたファイル名
     */
    public String encode(String fileName) {
        byte[] bytes;
        try {
            bytes = fileName.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(
                 "not supported charset. charset=[" + charset + "]", e);
        }
        return "=?" + charset + "?B?" + Base64Util.encode(bytes) + "?=";
    }
}
