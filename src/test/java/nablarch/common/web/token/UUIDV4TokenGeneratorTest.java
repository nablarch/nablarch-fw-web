package nablarch.common.web.token;

import org.junit.Test;

import static org.junit.Assert.*;

public class UUIDV4TokenGeneratorTest {

    @Test
    public void test() {
        final UUIDV4TokenGenerator sut = new UUIDV4TokenGenerator();
        final String generate = sut.generate();
        System.out.println("generate = " + generate);
        System.out.println(generate.length());
    }
}