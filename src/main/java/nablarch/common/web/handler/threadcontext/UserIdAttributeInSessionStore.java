package nablarch.common.web.handler.threadcontext;

import nablarch.common.handler.threadcontext.UserIdAttribute;
import nablarch.common.web.session.SessionUtil;
import nablarch.fw.ExecutionContext;

/**
 * セッションストアを使用してユーザIDの保持を行うクラス
 *
 * @author Goro KUMANO
 */
public class UserIdAttributeInSessionStore extends UserIdAttribute {
    @Override
    protected Object getUserIdSession(ExecutionContext ctx, String skey) {
        return SessionUtil.orNull(ctx, skey);
    }
}
