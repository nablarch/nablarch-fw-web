package nablarch.fw.web.handler.normalizer;

import nablarch.core.util.annotation.Published;

/**
 * ノーマライズを行うインタフェース。
 *
 * @author Hisaaki Shioiri
 */
@Published(tag = "architect")
public interface Normalizer {

    /**
     * このパラメータをノーマライズするか否か。
     *
     * @param key パラメータのキー
     * @return ノーマライズ対象の場合は {@code true}
     */
    boolean canNormalize(String key);

    /**
     * ノーマライズを行う。
     *
     * @param value ノーマライズ対象の値
     * @return ノーマライズ後の値
     */
    String[] normalize(String[] value);
}
