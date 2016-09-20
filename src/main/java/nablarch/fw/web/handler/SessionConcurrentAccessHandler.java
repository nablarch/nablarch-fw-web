package nablarch.fw.web.handler;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.message.ApplicationException;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MessageUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.core.util.map.CopyOnReadMap;
import nablarch.core.util.map.LockableMap;
import nablarch.core.util.map.MapWrapper;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.results.Conflicted;
import nablarch.fw.web.HttpResponse;

/**
 * セッションスコープに対する並行アクセス制御を行うハンドラ。
 * 
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 * @deprecated 本ハンドラは、{@link nablarch.common.web.session.SessionStore}を用いてセッション管理を行う
 *             {@link nablarch.common.web.session.SessionStoreHandler}に置き換わりました。
 */
@Deprecated
public class SessionConcurrentAccessHandler implements Handler<Object, Object> {
    
    //---------------------------------------------------- Concurrent policies
    /**
     * セッションスコープ変数に対する並行アクセス同期ポリシー。<br>
     * 補足：並行アクセス同期ポリシーについて、version 1.4.3まではMANUALとSERIALIZEDが存在していたが、MANUALとSERIALIZEについてはversion 1.5.0以降で廃止された。<br>
     */
    public static enum ConcurrentAccessPolicy {
        /**
         * スレッド毎のスナップショットを作成することで、
         * 並行アクセスに対する一貫読み取りおよび
         * 楽観ロック方式による書き込みを行う。
         * <p/>
         * リクエストスレッドがセッション上の変数にアクセスした時点で、
         * セッションのスナップショット（ディープコピー）をスレッドローカル変数上に作成する。
         * 以降、当該リクエストスレッドからのアクセスはこのスナップショットに対して行われる。
         * スナップショットにはバージョン番号が付与され、新たなスナップショットを生成する度にインクリメントされる。
         * リクエスト終了時にスナップショット上の変更をもとのセッションにマージするが、
         * この際、セッション上のバージョン番号とスナップショット上のバージョン番号が一致していない場合は
         * ワーニングメッセージを画面に表示する。
         */
        CONCURRENT
    }
    
    
    // ------------------------------------------------------ Custom errors
    /**
     * セッション書き込みに競合が発生したことを表す例外。
     */
    @Published(tag = "architect")
    public static class SessionConfliction extends Conflicted {
        /**
         * コンストラクタ。
         * @param message メッセージ
         */
        public SessionConfliction(String message) {
            super(message);
        }
    }
    
    
    // ------------------------------------------------ Configurable settings
    /** セッションスコープ変数に対する並行アクセス同期ポリシー */
    private ConcurrentAccessPolicy
        concurrentAccessPolicy = ConcurrentAccessPolicy.CONCURRENT;
    
    /** セッションへの書き込みの際に競合が発生した場合に表示されるメッセージのID */
    private String conflictWarningMessageId = null;

    
    // ------------------------------------------ Main routine and its helpers
    /** {@inheritDoc}
     * このハンドラの実装では、各並行アクセスポリシーを実装したMapWrapperによって
     * 実行コンテキスト上のセッションスコープをラップし、同期アクセス制御を開始する。
     * その後、後続ハンドラに処理を委譲する。
     * 
     * 同期制御はこのハンドラの終了とともに停止する。
     * そのため、JSPサーブレットからのセッション書込みを同期対象に含めるには、
     * 本ハンドラをレスポンスハンドラより上位に配置する必要がある。
     */
    @SuppressWarnings("unchecked")
    public Object handle(Object req, ExecutionContext ctx) {
        // restore the flags on this request thread.
        setThrowsErrorOnSessionWriteConflict(false);
        
        MapWrapper<String, Object> wrappedSession = null;
        Map<String, Object> session = ctx.getSessionScopeMap();
         
        wrappedSession = new CopyOnReadMap<String, Object>(
            new LockableMap<String, Object>(session)
        ).setIgnoredEntries("/nablarch_session_token");
      
        HttpResponse res = null;
        
        try {
            ctx.setSessionScopeMap(wrappedSession);
            res = ctx.handleNext(req);
            
        } finally {
            try {
                wrappedSession.getDelegateMapOfType(CopyOnReadMap.class)
                              .save();

            } catch (ConcurrentModificationException e) {
                // 並行する他のスレッドからこのセッションに対して先に
                // 書込みが行われ、書き戻しができない場合。
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.logInfo(
                        "Could not apply modification of session scope variables "
                      + "because another concurrent thread has already applied its modification."
                      , e
                    );
                }
                manageSessionRWConfliction(ctx, e);

            } catch (CopyOnReadMap.SnapshotCreationError e) {
                // セッションスコープ内にシリアライズ不可能なオブジェクトが存在する
                // などの理由で、セッション状態のスナップショットの作成に失敗した場合。
                throw new IllegalStateException(
                    "Could not apply modification of session scope variables "
                  + "because could not take snapshot of current session scope."
                  , e
                );
            } catch (IllegalStateException e) {
                // 並行するスレッドにより、このセッションが invalidate()された場合。
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.logInfo(
                        "Could not apply modification of session scope variables "
                      + "because the session scope is in invalid state."
                      , e
                    );
                }
                manageSessionRWConfliction(ctx, e);
            }
            LockableMap<String, Object>
            lockable = wrappedSession.getDelegateMapOfType(LockableMap.class);
            
            // ここで同期アクセス制御を終了させる。
            if (lockable != null) {
                lockable.deactivate();
            }
        }
        return res;
    }
    
    /**
     * セッション書き込みの競合が発生した旨を知らせるワーニングを追加する。
     * @param ctx 実行コンテキスト
     * @param e RuntimeException
     */
    private void manageSessionRWConfliction(ExecutionContext ctx, RuntimeException e) {        
        Message message = null;
        if (!StringUtil.isNullOrEmpty(conflictWarningMessageId)) {
            message = MessageUtil.createMessage(
                MessageLevel.ERROR, conflictWarningMessageId
            );
            ApplicationException ae = new ApplicationException(message);
            ctx.setException(ae);
        }

        if (THROWS_ON_SESSION_WRITE_CONFLICT.get().booleanValue()) {
            // 実行時例外を送出し、トランザクションをロールバックする。
            throw new SessionConfliction(
                (message == null) ? "your request was failed due to a session "
                                  + "access confliction with another concurrent request."
                                  : message.formatMessage()
            );
        }
    }

    
    // --------------------------------------------------------- Accessors
    /**
     * セッションスコープ変数に対する並行アクセス同期ポリシーを定義する。
     * <p>
     * 補足：<br>
     * version 1.5.0以降では、"CONCURRENT" のみ有効。本メソッドは互換性のために残っている。
     * </p>
     * @param policyName
     *     平行アクセス同期ポリシーの名称(version 1.5.0以降では、"CONCURRENT" のみ有効)
     * @throws IllegalArgumentException 上記以外の文字列を指定した場合。
     * @return このオブジェクト自体
     */
    public SessionConcurrentAccessHandler setConcurrentAccessPolicy(String policyName)
    throws IllegalArgumentException {
        concurrentAccessPolicy = Enum.valueOf(ConcurrentAccessPolicy.class, policyName);
        return this;
    }

    /**
     * 並行アクセス同期ポリシーを返す。
     * <pre>
     * </pre>
     * @return 現状のセッションスコープ変数に対する並行アクセス同期ポリシー。
     * version 1.5.0以降では、{@link ConcurrentAccessPolicy.CONCURRENT}が常に返却される。
     */
    public ConcurrentAccessPolicy getConcurrentAccessPolicy() {
        return (concurrentAccessPolicy == null) ? ConcurrentAccessPolicy.CONCURRENT
                                                : concurrentAccessPolicy;
    }
    
    /**
     * セッションへの書き込みの際に競合が発生した場合に表示される文言の
     * メッセージIDを設定する。
     * @param messageId メッセージID
     */
    public void setConflictWarningMessageId(String messageId) {
        conflictWarningMessageId = messageId;
    }

    /**
     * セッションオブジェクトに対する排他ロックを獲得する。
     * @param session セッションオブジェクト
     */
    public static void lockSession(Map<String, Object> session) {
        getSessionLock(session).lock();
    }
    
    /**
     * カレントスレッドがセッションオブジェクトに対する排他ロックを
     * 保持しているばあい、それを開放する。
     * @param session セッションオブジェクト
     */
    public static void unlockSession(Map<String, Object> session) {
        getSessionLock(session).unlock();
    }
    
    /**
     * カレントスレッドが保持しているセッションオブジェクトに対する排他ロックを返す。
     * @param session セッションオブジェクト
     * @return セッションオブジェクトに対する排他ロック
     *          (保持していない場合はnullを返す。)
     */
    private static Lock getSessionLock(Map<String, Object> session) {
        return ((MapWrapper<String, Object>) session)
               .getDelegateMapOfType(Lock.class);
    }
    
    /**
     * セッション変更の書き戻しに失敗した場合に実行時例外を送出するか否かを設定する。
     * <pre>
     * 明示的に設定しない場合のデフォルトはfalse。
     * この場合、エラー画面にワーニングが表示されるものの、DBのトランザクションは正常にコミットされる。
     * </pre>
     * @param throwsError 例外を送出する場合はtrue
     */
    public static void setThrowsErrorOnSessionWriteConflict(boolean throwsError) {
        THROWS_ON_SESSION_WRITE_CONFLICT.set(throwsError);
    }
    
    /** セッション変更の書き戻しに失敗した場合に実行時例外を送出するか否か */
    private static final ThreadLocal<Boolean>
            THROWS_ON_SESSION_WRITE_CONFLICT = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };
    
    /** ロガー */
    private static final Logger
    LOGGER = LoggerManager.get(SessionConcurrentAccessHandler.class);
}
