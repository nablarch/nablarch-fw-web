package nablarch.common.web.session;

import java.util.NoSuchElementException;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;

/**
 * セッションに関するユーティリティ。
 * <p/>
 * 業務Actionハンドラからは、必ず本クラスを使用してセッションの読み書きを行う。
 * <p/>
 * セッションへの登録処理は{@link SessionManager}によって提供される。
 * {@link SessionManager}の実装は、{@link SystemRepository}からコンポーネント名"sessionManager"で取得される。
 * <p/>
 * 本クラスは{@link SessionStoreHandler}と併せて使用すること。
 *
 * @author kawasima
 * @author tajima
 */
@Published
public final class SessionUtil {

    /**
     * 本クラスはインスタンスを生成しない。
     */
    private SessionUtil() {
    }

    /**
     * 名称を指定してセッションからオブジェクトを取得する。
     * <p/>
     * セッションに指定した名称をもつオブジェクトが存在しない場合、
     * {@link NoSuchElementException}を送出する。
     * <pre>
     * {@code
     * // "userName"という名称のオブジェクトがセッションに登録済み。設定値は"Nabu Rakutaro"
     * SessionUtil.get(ctx, "userName"); // -> "Nabu Rakutaro"
     *
     * // セッションに存在しないオブジェクトを指定
     * SessionUtil.get(ctx, "test"); // -> NoSuchElementExceptionを送出
     * }
     * </pre>
     *
     * @param <T> セッションに格納されているオブジェクトの型
     * @param ctx 実行コンテキスト
     * @param name セッションに登録したオブジェクトの名称
     * @return セッションから取得したオブジェクト
     * @throws NoSuchElementException 指定したオブジェクトの名称がセッションに存在しない場合
     */
    public static <T> T get(ExecutionContext ctx, String name) {
        T value = getSessionValue(ctx, name);
        if (value != null) {
            return value;
        } else {
            throw new NoSuchElementException();
        }
    }
    
    /**
     * 名称を指定してセッションからオブジェクトを取得する。
     * <p/>
     * セッションに指定した名称をもつオブジェクトが存在しない場合、nullを返す。
     * <pre>
     * {@code
     * // "userName"という名称のオブジェクトがセッションに登録済み。設定値は"Nabu Rakutaro"
     * SessionUtil.orNull(ctx, "userName"); // -> "Nabu Rakutaro"
     *
     * // セッションに存在しないオブジェクトを指定
     * SessionUtil.orNull(ctx, "test"); // -> null
     * }
     * </pre>
     *
     * @param <T> セッションに格納されているオブジェクトの型
     * @param ctx 実行コンテキスト
     * @param name セッションに登録したオブジェクトの名称
     * @return セッションから取得したオブジェクト
     */
    public static <T> T orNull(ExecutionContext ctx, String name) {
        return getSessionValue(ctx, name);
    }
    
    /**
     * 名称を指定してセッションからオブジェクトを取得する。
     * <p/>
     * セッションに指定した名称をもつオブジェクトが存在しない場合、デフォルト値を返す。
     * <pre>
     * {@code
     * // "userName"という名称のオブジェクトがセッションに登録済み。設定値は"Nabu Rakutaro"
     * SessionUtil.or(ctx, "userName", "デフォルト値"); // -> "Nabu Rakutaro"
     *
     * // セッションに存在しないオブジェクトを指定
     * SessionUtil.or(ctx, "test", "デフォルト値"); // -> "デフォルト値"
     * }
     * </pre>
     *
     * @param <T> セッションに格納されているオブジェクトの型
     * @param ctx 実行コンテキスト
     * @param name セッションに登録したオブジェクトの名称
     * @param defaultValue デフォルト値
     * @return セッションから取得したオブジェクト
     */
    public static <T> T or(ExecutionContext ctx, String name, T defaultValue) {
        T value = getSessionValue(ctx, name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 指定されたセッションオブジェクトを取得する。
     * 
     * @param <T> セッションに格納されているオブジェクトの型
     * @param ctx 実行コンテキスト
     * @param name セッションに登録したオブジェクトの名称
     * @return セッションから取得したオブジェクト
     */
    @SuppressWarnings("unchecked")
    private static <T> T getSessionValue(ExecutionContext ctx, String name) {
        Object value = ctx.getSessionStoredVar(name);
        if (value instanceof SessionEntry) {
            return (T) SessionEntry.class.cast(value).getValue();
        } else {
            return null;
        }
    }

    /**
     * {@link SessionStore}に変数を保存する。
     * <p/>
     * オブジェクトの保存先は{@link SessionManager}で指定した{@link SessionStore}が選択される。
     * 同一の登録名をもつオブジェクトは上書きされる。
     * よって、複数の{@link SessionStore}を利用する場合でも、
     * 同一の登録名をもつオブジェクトは一つしか登録できない。
     * <p/>
     * 注意:セッションで管理できるオブジェクトの制限について、
     * {@link #put(ExecutionContext, String, Object, String)}を参照すること。
     * <p/>
     * <pre>
     * {@code
     * SessionUtil.put(ctx, "userName", "Nabu Rakutaro");
     * }
     * </pre>
     *
     * @param ctx 実行コンテキスト
     * @param name セッションに登録するオブジェクトの名称
     * @param value セッションに登録するオブジェクト
     */
    public static void put(ExecutionContext ctx, String name, Object value) {
        ctx.setSessionStoredVar(
                name, new SessionEntry(name, value, getSessionManager().getDefaultStore()));
    }

    /**
     * 保存先の{@link SessionStore}を指定して、セッションに変数を保存する。
     * <p/>
     * 同一の登録名をもつオブジェクトは上書きされる。
     * よって、複数の{@link SessionStore}を利用する場合でも、
     * 同一の登録名をもつオブジェクトは一つしか登録できない。
     * <p/>
     * 注意:セッションに直接格納し、復元ができるオブジェクトの制限について<br/>
     * セッションに直接格納できるのは、下記条件を満たすJava Beanオブジェクトのみである。
     * <ul>
     *     <li>デフォルトコンストラクタが定義されていること</li>
     *     <li>値を保持したいプロパティに対し、setter及びgetterが定義されていること</li>
     *     <li>シリアライズ可能であること</li>
     * </ul>
     * 上記条件を満たさないオブジェクトを登録した場合、格納/復元処理が正常に動作しない。
     * 例えば、配列型のオブジェクトを直接セッションに格納した場合、復元できない。
     * その場合、配列を直接格納するのではなくBeanオブジェクトのプロパティとして保持し、
     * Beanオブジェクトをセッションに格納すること。
     * <p/>
     * <pre>
     * {@code
     * SessionUtil.put(ctx, "userName", "Nabu Rakutaro", "httpSession");
     * }
     * </pre>
     *
     * @param ctx 実行コンテキスト
     * @param name セッションに登録するオブジェクトの名称
     * @param value セッションに登録するオブジェクト
     * @param storeName 登録対象のセッションストア名
     */
    public static void put(ExecutionContext ctx, String name, Object value, String storeName) {
        ctx.setSessionStoredVar(
                name, new SessionEntry(name, value, getSessionManager().findSessionStore(storeName)));
    }

    /**
     * リポジトリから{@link SessionManager}を取得する。
     * @return {@link SessionManager}
     */
    private static SessionManager getSessionManager() {
        return SystemRepository.get("sessionManager");
    }

    /**
     * セッションを削除する。
     * <p/>
     * 指定した名称のセッションオブジェクトが存在しない場合は無視される。
     * <pre>
     * {@code
     * // Sessionスコープに"sessionProject"という名称でオブジェクトが登録されている前提
     * SessionUtil.delete(context, "sessionProject");
     * }
     * </pre>
     *
     * @param ctx 実行コンテキスト
     * @param name セッションに登録したオブジェクトの名称
     * @return 削除されたセッションの値(セッションオブジェクトが存在しない場合は{@code null})
     */
    public static <T> T delete(ExecutionContext ctx, String name) {
        Object sessionStoredVar = ctx.getSessionStoredVar(name);
        if (sessionStoredVar instanceof SessionEntry) {
            final T value = (T) ((SessionEntry) sessionStoredVar).getValue();
            ctx.setSessionStoredVar(name, value);
            return value;
        }
        return null;
    }

    /**
     * セッションを無効化する。
     *
     * @param ctx 実行コンテキスト
     */
    public static void invalidate(ExecutionContext ctx) {
        for (String key : ctx.getSessionStoreMap().keySet()) {
            delete(ctx, key);
        }
        ctx.setSessionStoredVar(SessionStoreHandler.IS_INVALIDATED_KEY, Boolean.TRUE);
    }
}
