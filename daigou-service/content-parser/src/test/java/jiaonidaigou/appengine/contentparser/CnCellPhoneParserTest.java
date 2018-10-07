package jiaonidaigou.appengine.contentparser;

import jiaoni.daigou.wiremodel.entity.PhoneNumber;
import org.junit.Test;

import static jiaonidaigou.appengine.contentparser.AnswerMatchers.hasAnswers;
import static jiaonidaigou.appengine.contentparser.AnswerMatchers.is;
import static jiaonidaigou.appengine.contentparser.Conf.CONFIRMED;
import static org.junit.Assert.assertThat;

public class CnCellPhoneParserTest {
    private final CnCellPhoneParser underTest = new CnCellPhoneParser();

    @Test
    public void testCellPhone() {
        assertThat(underTest.parse("13812345670"), hasAnswers(
                PhoneNumber::getPhone,
                is("13812345670", CONFIRMED)
        ));

        assertThat(underTest.parse("13812345670 还有一个 13812345688"), hasAnswers(
                PhoneNumber::getPhone,
                is("13812345670", CONFIRMED),
                is("13812345688", CONFIRMED)
        ));
    }
}
