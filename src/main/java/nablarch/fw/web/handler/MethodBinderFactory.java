package nablarch.fw.web.handler;

import nablarch.core.util.annotation.Published;
import nablarch.fw.MethodBinder;
import nablarch.fw.web.HttpRequest;

/**
 * {@link MethodBinder}のファクトリクラス。
 *
 * @author Naoki Yamamoto
 * @param <T> レスポンス型
 */
@Published(tag = "architect")
public interface MethodBinderFactory<T> {

    /**
     * {@link MethodBinder}を生成する。
     *
     * @param methodName ディスパッチするメソッド名
     * @return {@link MethodBinder}インスタンス
     */
    MethodBinder<HttpRequest, T> create(final String methodName);
}
