package nablarch.fw.web.download.encorder;

import java.util.regex.Pattern;

import nablarch.core.util.annotation.Published;


/**
 * User-Agentヘッダとエンコーダの関連を保持するエントリ。
 * 
 * @author Masato Inoue
 */
@Published(tag = "architect")
public class DownloadFileNameEncoderEntry {

    /**
     * User-AgentヘッダのパターンをPatternオブジェクト化したもの。
     */
    private Pattern userAgentPattern = null;
    
    /**
     * ファイル名のエンコーダ。
     */
    private DownloadFileNameEncoder encoder = new UrlDownloadFileNameEncoder();
    
    /**
     * User-Agentヘッダのパターンを取得する。
     * @return userAgentPattern User-Agentヘッダのパターン
     */
    public Pattern getUserAgentPattern() {
        return userAgentPattern;
    }
    /**
     * User-Agentヘッダのパターンを設定する。
     * @param userAgentPattern userAgentPattern User-Agentヘッダのパターン
     */
    public void setUserAgentPattern(String userAgentPattern) {
        this.userAgentPattern = Pattern.compile(userAgentPattern.trim());
    }
    
    /**
     * ファイル名のエンコーダを取得する。
     * @return encoder ファイル名のエンコーダ
     */
    public DownloadFileNameEncoder getEncoder() {
        return encoder;
    }
    /**
     * ファイル名のエンコーダを設定する。
     * @param encoder ファイル名のエンコーダ
     */
    public void setEncoder(DownloadFileNameEncoder encoder) {
        this.encoder = encoder;
    }


    
}
