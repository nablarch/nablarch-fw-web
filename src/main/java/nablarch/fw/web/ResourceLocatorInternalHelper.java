package nablarch.fw.web;

import java.util.regex.Pattern;

/**
 * nablarch-fw-web内でのみ使用する{@link ResourceLocator}の補助クラス。
 * 
 * @author Taichi Uragami
 */
public class ResourceLocatorInternalHelper {

    /** スキームから開始される文字列であるか検証するための正規表現 */
    private static final Pattern STARTS_WITH_SCHEME_PATTERN = Pattern
            .compile("[A-Za-z][A-Za-z0-9+-.]*:.*");

    /**
     * 渡された{@code path}がschemeとコロンから始まるかどうか判断して結果を返す。
     * 
     * schemeの仕様はRFC3986のSection 3.1に準ずる。
     * 
     * @param path 評価対象となるパス
     * @return {@code path}がschemeとコロンから始まっていれば{@code true}
     * @see <a href="https://tools.ietf.org/html/rfc3986#section-3.1" rel="nofollow">RFC 3986 Section 3.1. Scheme</a>
     */
    public static boolean startsWithScheme(String path) {
        return STARTS_WITH_SCHEME_PATTERN.matcher(path).matches();
    }

    /**
     * {@link ResourceLocator#isRedirectWithAbsoluteUri()}を中継する。
     * 
     * @param resourceLocator リソースロケータ
     * @return {@link ResourceLocator#isRedirectWithAbsoluteUri()}の戻り値
     */
    public static boolean isRedirectWithAbsoluteUri(ResourceLocator resourceLocator) {
        return resourceLocator.isRedirectWithAbsoluteUri();
    }
}
