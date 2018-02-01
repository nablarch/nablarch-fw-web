package nablarch.fw.web.message;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.hamcrest.Matchers;

import nablarch.core.message.ApplicationException;
import nablarch.core.message.BasicStringResource;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.validation.ValidationResultMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link ErrorMessages}のテストクラス。
 */
public class ErrorMessagesTest {
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ErrorMessages sut;

    @Before
    public void setUp() {

        // @formatter:off
        final ApplicationException exception = new ApplicationException();
        exception.addMessages(new ValidationResultMessage(
                "prop1", new BasicStringResource("id", Collections.singletonMap("ja", "エラー")), new Object[0]));

        exception.addMessages(new Message(
                MessageLevel.ERROR, new BasicStringResource("global", Collections.singletonMap("ja", "globalメッセージ")), new Object[0]));

        exception.addMessages(new ValidationResultMessage(
                "prop2", new BasicStringResource("id2", Collections.singletonMap("ja", "エラー２")), new Object[0]));
        
        exception.addMessages(new ValidationResultMessage(
                "prop2", new BasicStringResource("id2", Collections.singletonMap("ja", "エラー２の２")), new Object[0]));

        exception.addMessages(new Message(
                MessageLevel.ERROR, new BasicStringResource("global2", Collections.singletonMap("ja", "globalメッセージ２")), new Object[0]));
        sut = new ErrorMessages(exception);
        // @formatter:on
    }

    @Test
    public void プロパティに紐づくメッセージが扱えること() {

        assertThat("prop1のエラーはあるのでtrue", sut.hasError("prop1"), is(true));
        assertThat("prop1のメッセージ", sut.getMessage("prop1"), is("エラー"));
        
        assertThat("同一のプロパティ名のメッセージが複数あった場合は最後のメッセージが返される",
                sut.getMessage("prop2"), is("エラー２の２"));

        assertThat("prop3のエラーはないのでfalse", sut.hasError("prop3"), is(false));
        assertThat("prop3は存在しないのでメッセージはnull", sut.getMessage("prop3"), is(nullValue()));

        assertThat("プロパティに紐づくメッセージだけが元の順番のまま返されること", sut.getPropertyMessages(), contains("エラー", "エラー２", "エラー２の２"));
    }

    @Test
    public void hasErrorでプロパティ名にnullを指定した場合は例外が送出されること() {
        expectedException.expect(IllegalArgumentException.class);
        sut.hasError(null);
    }

    @Test
    public void getMessageでプロパティ名にnullを指定した場合は例外が送出されること() {
        expectedException.expect(IllegalArgumentException.class);
        sut.getMessage(null);
    }

    @Test
    public void グローバルメッセージが扱えること() {
        assertThat("グローバルメッセージだけが元の順番のまま返されること",
                sut.getGlobalMessages(), contains("globalメッセージ", "globalメッセージ２"));
    }

    @Test
    public void 全てのメセージが扱えること() {
        assertThat("全てのメッセージが元の順番のまま返されること",
                sut.getAllMessages(), contains("エラー", "globalメッセージ", "エラー２", "エラー２の２", "globalメッセージ２"));
    }

    @Test
    public void メッセージが存在しない場合空のリストが返されること() {
        final ErrorMessages sut = new ErrorMessages(new ApplicationException());

        assertThat(sut.getAllMessages(), Matchers.<String>empty());
        assertThat(sut.getGlobalMessages(), Matchers.<String>empty());
        assertThat(sut.getPropertyMessages(), Matchers.<String>empty());
    }
}