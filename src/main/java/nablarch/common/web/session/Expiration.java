package nablarch.common.web.session;

import nablarch.fw.ExecutionContext;

public interface Expiration {

    boolean isExpired(String sessionID, long currentDateTime, ExecutionContext context);

    void saveExpirationDateTime(String sessionId, long expirationDateTime, ExecutionContext context);

}
