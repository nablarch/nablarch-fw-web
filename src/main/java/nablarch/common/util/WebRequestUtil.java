package nablarch.common.util;

import java.util.List;
import java.util.Map;


/**
 * リクエスト処理に使用するユーティリティ。
 * @author Kiyohito Itoh
 */
public final class WebRequestUtil {
    
    /** パラメータマップとパラメータ文字列の変換に使用する{@link nablarch.common.util.ParamsConvertor} */
    private static final ParamsConvertor PARAMS_CONVERTOR = new ParamsConvertor('|', '=', '\\');

    /** 隠蔽コンストラクタ */
    private WebRequestUtil() {
    }

    /**
     * リクエストパスからリクエストIDに相当する部分を抜き出す。
     * @param path リクエストパス
     * @return リクエストID。見つからない場合はnull
     */
    public static String getRequestId(String path) {
        return RequestUtil.getRequestId(path);
    }

    /**
     * パラメータマップをパラメータ文字列に変換する。<br>
     * 変換では、下記の文字を使用する。
     * <pre>
     * パラメータ間のセパレータ: ','
     * name/value間のセパレータ: '='
     * セパレータのエスケープ文字: '\'
     * </pre>
     * @param params パラメータマップ
     * @return パラメータ文字列
     */
    public static String convertToParamsString(Map<String, List<String>> params) {
        return PARAMS_CONVERTOR.convert(params);
    }

    /**
     * パラメータ文字列をパラメータマップに変換する。<br>
     * パラメータ文字列のパース処理は、{@link #convertToParamsString(java.util.Map)}を使用して変換されていることを前提に行う。
     * @param params パラメータ文字列
     * @return パラメータマップ
     */
    public static Map<String, List<String>> convertToParamsMap(String params) {
        return PARAMS_CONVERTOR.convert(params);
    }

}
