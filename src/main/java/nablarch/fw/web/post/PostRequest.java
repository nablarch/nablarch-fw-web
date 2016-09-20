package nablarch.fw.web.post;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import nablarch.fw.web.HttpRequest;

/**
 * POST時のリクエスト情報を保持するクラス。
 *
 * @author Kiyohito Itoh
 */
public class PostRequest implements Serializable {

    /** リクエストパラメータ */
    private Map<String, String[]> paramMap;

    /**
     * コンストラクタ。
     * <p/>
     * リクエスト情報として下記を取得する。
     * ・リクエストパラメータ
     * ・マルチパート
     *
     * @param request リクエスト
     */
    public PostRequest(HttpRequest request) {
        paramMap = new HashMap<String, String[]>(request.getParamMap());
    }

    /**
     * リクエストパラメータを取得する。
     * @return リクエストパラメータ
     */
    public Map<String, String[]> getParamMap() {
        return paramMap;
    }

}
