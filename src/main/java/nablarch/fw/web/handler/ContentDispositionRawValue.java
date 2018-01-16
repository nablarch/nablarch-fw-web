package nablarch.fw.web.handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.fw.web.download.encorder.DownloadFileNameEncoder;
import nablarch.fw.web.download.encorder.UrlDownloadFileNameEncoder;

/**
 * エンコードされていない生のContent-Dispositionヘッダ値を表すクラス。
 * 
 * @author Taichi Uragami
 *
 */
class ContentDispositionRawValue {

    /** filenameキー */
    private static final String FILENAME_PARAM_NAME = normalizeKey("filename");
    /** filename*キー */
    private static final String FILENAME_EXT_PARAM_NAME = normalizeKey("filename*");
    /** disposition-typeを抽出するための正規表現 */
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\s*([^;\\s]+)");
    /** パラメーターを抽出するための正規表現 */
    private static final Pattern PARAM_PATTERN = Pattern
            .compile("\\s*;\\s*([^=\\s]+)\\s*=\\s*([^;\\s]+)");
    /** UTF-8でURLエンコードをする{@link DownloadFileNameEncoder} */
    private static final UrlDownloadFileNameEncoder UTF8_URL_ENCODER = new UrlDownloadFileNameEncoder();

    /**
     * inline、attachementなどのdisposition-type
     * 
     * @see <a href="https://tools.ietf.org/html/rfc6266#section-4.2">https://tools.ietf.org/html/rfc6266#section-4.2</a>
     */
    private final String type;
    /** パラメーターのリスト */
    private final List<Param> params;
    /** filenameパラメーター。nullの場合もある。 */
    private final Param filenameParam;
    /** filename*パラメーターを保持している場合はtrue */
    private final boolean containsFilenameExtParam;

    /**
     * コンストラクタ。
     * 
     * @param rawValue エンコードされていない生のContent-Dispositionヘッダ値
     */
    public ContentDispositionRawValue(final String rawValue) {
        final Matcher typeMatcher = TYPE_PATTERN.matcher(rawValue);
        typeMatcher.find();
        this.type = typeMatcher.group(1);

        int index = typeMatcher.group().length();

        final LinkedHashMap<String, Param> paramMap = new LinkedHashMap<String, Param>();
        final Matcher paramMatcher = PARAM_PATTERN.matcher(rawValue);
        while (paramMatcher.find(index)) {
            final String key = paramMatcher.group(1);
            final String value = paramMatcher.group(2);
            final Param param = new Param(key, value);
            param.putTo(paramMap);
            index += paramMatcher.group().length();
        }
        this.params = new ArrayList<Param>(paramMap.values());

        this.filenameParam = paramMap.get(FILENAME_PARAM_NAME);
        this.containsFilenameExtParam = paramMap.containsKey(FILENAME_EXT_PARAM_NAME);
    }

    /**
     *エンコードする必要があるかどうかを返す。
     * 
     * @return エンコードする必要があればtrue
     */
    public boolean needsToBeEncoded() {
        return filenameParam != null && filenameParam.quoted;
    }

    /**
     * エンコードされていないファイル名を取得する。
     * 
     * @return エンコードされていないファイル名
     */
    public String getRawFileName() {
        return filenameParam.value;
    }

    /**
     * エンコード済みのContent-Dispositionヘッダ値を組み立てる。
     * 
     * @param encodedFileName エンコード済みのファイル名
     * @return エンコード済みのContent-Dispositionヘッダ値
     */
    public String buildEncodedValue(final String encodedFileName) {
        final StringBuilder buf = new StringBuilder();
        buf.append(type);
        for (Param param : params) {
            param.appendTo(buf, encodedFileName);
        }
        return buf.toString();
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase();
    }

    private class Param {

        /** コンストラクタで受け取ったキー */
        final String originalKey;
        /**
         * キーに{@link ContentDispositionRawValue#normalizeKey(String)}を適用したもの。
         * キーの比較には{@link #originalKey}ではなくこちらを使用する。
         */
        final String normalizedKey;
        /** 値。ダブルクォートで囲まれていた場合はダブルクォートが取り除かれた状態 */
        final String value;
        /** 値がダブルクォートで囲まれていたか */
        final boolean quoted;

        /**
         * コンストラクタ。
         * 
         * @param key キー
         * @param value 値
         */
        Param(final String key, final String value) {
            this.originalKey = key;
            this.normalizedKey = normalizeKey(key);
            if (value.charAt(0) == '"') {
                this.value = value.substring(1, value.length() - 1);
                this.quoted = true;
            } else {
                this.value = value;
                this.quoted = false;
            }
        }

        /**
         * StringBuilderへパラメーターを追加する。
         * 
         * <p>
         * 自身がfilenameパラメーターなら引数で受け取ったencodedFileNameを値として追加する。
         * また、自身がfilenameパラメーターかつ明示的にfilename*パラメーターが設定されていない場合、
         * filenameパラメーターに設定された値をもとにしてfilename*パラメーターを追加する。
         * </p>
         * 
         * @param buf ここにパラメーターが追加される
         * @param encodedFileName エンコード済みのファイル名
         */
        void appendTo(final StringBuilder buf, final String encodedFileName) {

            final boolean isFilenameParam = normalizedKey.equals(FILENAME_PARAM_NAME);

            if (isFilenameParam && containsFilenameExtParam == false) {
                buf.append("; ")
                        .append(FILENAME_EXT_PARAM_NAME)
                        .append("=")
                        .append("UTF-8''").append(UTF8_URL_ENCODER.encode(value));
            }

            buf.append("; ").append(originalKey).append('=');
            if (quoted) {
                buf.append('"');
            }
            buf.append(isFilenameParam ? encodedFileName : value);
            if (quoted) {
                buf.append('"');
            }
        }

        /**
         * 引数で渡された{@link LinkedHashMap}に自身を加える。
         * 
         * @param paramMap ここに自身を加える
         */
        void putTo(final LinkedHashMap<String, Param> paramMap) {
            paramMap.put(normalizedKey, this);
        }
    }
}