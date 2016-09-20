package nablarch.fw.web.download.encorder;

import java.util.ArrayList;
import java.util.List;

import nablarch.core.util.annotation.Published;


/**
 * User-Agentに対応するエンコーダを取得するインタフェース。
 * 
 * @author Masato Inoue
 */
@Published(tag = "architect")
public class DownloadFileNameEncoderFactory {

    /**
     * デフォルトのエンコーダ。
     */
    private DownloadFileNameEncoder defaultEncoder = new UrlDownloadFileNameEncoder();

    /**
     * User-Agentヘッダのパターンとエンコーダの関連を保持するエントリのList。
     */
    private List<DownloadFileNameEncoderEntry> downloadFileNameEncoderEntries = new ArrayList<DownloadFileNameEncoderEntry>();
    
    /**
     * コンストラクタ。
     * デフォルトのdownloadFileNameEncoderEntriesプロパティを生成する。
     */
    public DownloadFileNameEncoderFactory() {
        downloadFileNameEncoderEntries = createDownloadFileNameEncoderEntries();
     }
    
    /**
     * デフォルトのUser-Agentヘッダのパターンとエンコーダの関連を保持するエントリのList。
     * @return User-Agentヘッダのパターンとエンコーダの関連を保持するエントリのList
     */
    protected ArrayList<DownloadFileNameEncoderEntry> createDownloadFileNameEncoderEntries() {
        ArrayList<DownloadFileNameEncoderEntry> list = new ArrayList<DownloadFileNameEncoderEntry>();
        list.add(createEntry(".*MSIE.*", new UrlDownloadFileNameEncoder()));
        list.add(createEntry(".*Trident.*", new UrlDownloadFileNameEncoder()));
        list.add(createEntry(".*WebKit.*", new UrlDownloadFileNameEncoder()));
        list.add(createEntry(".*Gecko.*", new MimeBDownloadFileNameEncoder()));
        return list;
    }
    
    /**
     * User-Agentヘッダとエンコーダの関連を保持するエントリを生成する。
     * @param userAgentPattern User-Agentヘッダのパターン
     * @param encoder ファイル名のエンコーダ
     * @return 引数で指定された値を設定したエントリ
     */
    private DownloadFileNameEncoderEntry createEntry(String userAgentPattern, DownloadFileNameEncoder encoder) {
        DownloadFileNameEncoderEntry entry = new DownloadFileNameEncoderEntry();
        entry.setUserAgentPattern(userAgentPattern);
        entry.setEncoder(encoder);
        return entry;
    }
    
    /**
     * デフォルトのエンコーダを設定する。
     * @param defaultEncoder デフォルトのエンコーダ
     */
    public void setDefaultEncoder(DownloadFileNameEncoder defaultEncoder) {
        this.defaultEncoder = defaultEncoder;
    }
    
    /**
     * エンコーダのエントリを設定する。
     * @param downloadFileNameEncoderEntries エンコーダのエントリ
     */
    public void setDownloadFileNameEncoderEntries(
            List<DownloadFileNameEncoderEntry> downloadFileNameEncoderEntries) {
        for (DownloadFileNameEncoderEntry entry : downloadFileNameEncoderEntries) {
            if (entry.getUserAgentPattern() == null) {
                throw new RuntimeException("userAgentPattern property was not configured.");
            }
        }
        this.downloadFileNameEncoderEntries = downloadFileNameEncoderEntries;
    }
    
    /**
     * User-Agentに対応するエンコーダを取得する。
     * <br>
     * User-Agentヘッダのパターンとエンコーダの関連は、downloadFileNameEncoderEntriesプロパティより取得する。
     * @param userAgent User-Agentヘッダの内容
     * @return エンコードされたファイル名
     */
    public DownloadFileNameEncoder getEncoder(String userAgent) {
        // userAgentがnullの場合は、デフォルトのエンコーダを返却する
        if (userAgent == null) {
            return defaultEncoder;
        }
        
        for (DownloadFileNameEncoderEntry entry : downloadFileNameEncoderEntries) {
            if (entry.getUserAgentPattern().matcher(userAgent).matches()) {
                return entry.getEncoder();
            }
        }
        
        // 対応するエンコーダが見つからなかった場合は、デフォルトのエンコーダを返却する
        return defaultEncoder;
    }
    
}
