package nablarch.common.web.token;

/**
 * @author Kiyohito Itoh
 */
public class MockTokenGenerator implements TokenGenerator {

    /**
     * {@inheritDoc}
     */
    public String generate() {
        return "token_test";
    }

}
