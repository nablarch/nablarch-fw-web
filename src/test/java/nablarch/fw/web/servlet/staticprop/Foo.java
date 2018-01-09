package nablarch.fw.web.servlet.staticprop;

public class Foo {

    private static Bar bar;

    public static Bar getBar() {
        return bar;
    }

    public static void setBar(Bar bar) {
        Foo.bar = bar;
    }
}