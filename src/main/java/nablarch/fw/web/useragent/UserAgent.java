package nablarch.fw.web.useragent;

import nablarch.core.util.annotation.Published;

import static nablarch.core.util.StringUtil.nullToEmpty;

/**
 * UserAgent情報を表すクラス。
 *
 * @author TIS
 */
public class UserAgent {

    /** デフォルトの種類 */
    @Published
    public static final String DEFAULT_TYPE_VALUE = "UnknownType";

    /** デフォルトの名称 */
    @Published
    public static final String DEFAULT_NAME_VALUE = "UnknownName";

    /** デフォルトのバージョン */
    @Published
    public static final String DEFAULT_VERSION_VALUE = "UnknownVersion";

    /** UserAgent文字列 */
    private final String text;

    /** OSタイプ */
    private String osType;

    /** OS名称 */
    private String osName;

    /** OSバージョン */
    private String osVersion;

    /** ブラウザタイプ */
    private String browserType;

    /** ブラウザ名称 */
    private String browserName;

    /** ブラウザバージョン */
    private String browserVersion;

    /**
     * 指定したUser-Agent文字列を保持する{@code UserAgent}オブジェクトを生成する。
     *
     * @param text User-Agent文字列
     */
    @Published(tag = "architect")
    public UserAgent(String text) {
        this.text = text;
    }

    /**
     * 指定した{@code UserAgent}オブジェクトを保持する{@code UserAgent}オブジェクトを生成する。
     *
     * @param original {@code UserAgent}オブジェクト
     */
    @Published(tag = "architect")
    public UserAgent(UserAgent original) {
        text = original.getText();
        setBrowserType(original.getBrowserType());
        setBrowserName(original.getBrowserName());
        setBrowserVersion(original.getBrowserVersion());
        setOsType(original.getOsType());
        setOsName(original.getOsName());
        setOsVersion(original.getOsVersion());
    }


    /**
     * UserAgent文字列を取得する。
     *
     * @return User-Agent文字列（User-Agent文字列が{@code null}の場合は空文字を返却する）
     */
    @Published
    public String getText() {
        return nullToEmpty(text);
    }

    /**
     * ブラウザタイプを取得する。
     *
     * @return ブラウザタイプ（ブラウザタイプが{@code null}の場合は"UnknownType"を返却する）
     */
    @Published
    public String getBrowserType() {
        return nullToDefault(browserType, DEFAULT_TYPE_VALUE);
    }

    /**
     * ブラウザタイプをセットする。
     *
     * @param browserType ブラウザタイプ
     */
    @Published(tag = "architect")
    public void setBrowserType(String browserType) {
        this.browserType = browserType;
    }

    /**
     * ブラウザ名称を取得する。
     *
     * @return ブラウザ名称（ブラウザ名称が{@code null}の場合は"UnknownName"を返却する）
     */
    @Published
    public String getBrowserName() {
        return nullToDefault(browserName, DEFAULT_NAME_VALUE);
    }

    /**
     * ブラウザ名称をセットする。
     *
     * @param browserName ブラウザ名称
     */
    @Published(tag = "architect")
    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    /**
     * ブラウザバージョンを取得する。
     *
     * @return ブラウザバージョン（ブラウザバージョンが{@code null}の場合は"UnknownVersion"を返却する）
     */
    @Published
    public String getBrowserVersion() {
        return nullToDefault(browserVersion, DEFAULT_VERSION_VALUE);
    }

    /**
     * ブラウザバージョンをセットする。
     *
     * @param browserVersion ブラウザバージョン
     */
    @Published(tag = "architect")
    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    /**
     * OSタイプを取得する。
     *
     * @return OSタイプ（OSタイプが{@code null}の場合は"UnknownType"を返却する）
     */
    @Published
    public String getOsType() {
        return nullToDefault(osType, DEFAULT_TYPE_VALUE);
    }

    /**
     * OSタイプをセットする。
     *
     * @param osType OSタイプ
     */
    @Published(tag = "architect")
    public void setOsType(String osType) {
        this.osType = osType;
    }

    /**
     * OS名称を取得する。
     *
     * @return OS名称（OS名称が{@code null}の場合は"UnknownName"を返却する）
     */
    @Published
    public String getOsName() {
        return nullToDefault(osName, DEFAULT_NAME_VALUE);
    }

    /**
     * OS名称をセットする。
     *
     * @param osName OS名称
     */
    @Published(tag = "architect")
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * OSバージョンを取得する。
     *
     * @return OSバージョン（OSバージョンが{@code null}の場合は"UnknownVersion"を返却する）
     */
    @Published
    public String getOsVersion() {
        return nullToDefault(osVersion, DEFAULT_VERSION_VALUE);
    }

    /**
     * OSバージョンをセットする。
     *
     * @param osVersion OSバージョン
     */
    @Published(tag = "architect")
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }


    /**
     * 与えられた文字列がnullの場合、代替値を返却する。
     * そうでない場合は文字列をそのまま返却する。
     *
     * @param str 文字列
     * @param alternative 代替値
     * @return 変換後の文字列
     */
    private String nullToDefault(String str, String alternative) {
        return str == null ? alternative : str;
    }
}
