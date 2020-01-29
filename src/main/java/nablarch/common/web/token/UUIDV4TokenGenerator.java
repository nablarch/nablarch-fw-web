package nablarch.common.web.token;

import java.util.UUID;

/**
 * UUID(version4)を使用した{@link TokenGenerator}実装クラス。
 *
 * @author Tsuyoshi Kawasaki
 */
public class UUIDV4TokenGenerator implements TokenGenerator {

    @Override
    public String generate() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

}
