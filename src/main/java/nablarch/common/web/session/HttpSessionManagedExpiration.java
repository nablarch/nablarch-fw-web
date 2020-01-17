package nablarch.common.web.session;

import nablarch.fw.ExecutionContext;

public class HttpSessionManagedExpiration implements Expiration {

    /** セッションの有効期限を格納するHttpSessionの名前 */
    private static final String EXPIRATION_DATE_KEY = ExecutionContext.FW_PREFIX + "sessionStore_expiration_date";

    @Override
    public boolean isExpired(String sessionId, long currentDateTime, ExecutionContext context) {
        Long expiration = context.getSessionScopedVar(EXPIRATION_DATE_KEY);
        if (expiration == null) {
            return true;
        }
        return expiration >= currentDateTime;
    }

    @Override
    public void saveExpirationDateTime(String sessionId, long expirationDateTime, ExecutionContext context) {
        context.setSessionScopedVar(EXPIRATION_DATE_KEY, expirationDateTime);
    }

}
