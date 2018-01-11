package nablarch.fw.web.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * エンコードされていない生のContent-Dispositionヘッダ値を表すクラス。
 * 
 * @author Taichi Uragami
 *
 */
class ContentDispositionRawValue {

    private final Matcher matcher;
    private final boolean matched;

    /**
     * コンストラクタ。
     * 
     * @param rawValue エンコードされていない生のContent-Dispositionヘッダ値
     */
    public ContentDispositionRawValue(final String rawValue) {
        Pattern pattern = Pattern.compile("(.*filename=\")(.*)(\".*)");
        this.matcher = pattern.matcher(rawValue);
        this.matched = this.matcher.matches();
    }

    /**
     *エンコードする必要があるかどうかを返す。
     * 
     * @return エンコードする必要があればtrue
     */
    public boolean needsToBeEncoded() {
        return matched;
    }

    /**
     * エンコードされていないファイル名を取得する。
     * 
     * @return エンコードされていないファイル名
     */
    public String getRawFileName() {
        return matcher.group(2);
    }

    /**
     * エンコード済みのContent-Dispositionヘッダ値を組み立てる。
     * 
     * @param encodedFileName エンコード済みのファイル名
     * @return エンコード済みのContent-Dispositionヘッダ値
     */
    public String buildEncodedValue(final String encodedFileName) {
        return matcher.replaceFirst("$1" + encodedFileName + "$3");
    }
}