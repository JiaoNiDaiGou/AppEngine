package jiaoni.daigou.contentparser;

import jiaoni.common.wiremodel.PhoneNumber;
import org.junit.Test;

import static jiaoni.daigou.contentparser.AnswerMatchers.hasAnswers;
import static org.junit.Assert.assertThat;

public class CnCellPhoneParserTest {
    private final CnCellPhoneParser underTest = new CnCellPhoneParser();

    @Test
    public void testCellPhone() {
        assertThat(underTest.parse("13812345670"), AnswerMatchers.hasAnswers(
                PhoneNumber::getPhone,
                AnswerMatchers.is("13812345670", Conf.CONFIRMED)
        ));

        assertThat(underTest.parse("13812345670 还有一个 13812345688"), AnswerMatchers.hasAnswers(
                PhoneNumber::getPhone,
                AnswerMatchers.is("13812345670", Conf.CONFIRMED),
                AnswerMatchers.is("13812345688", Conf.CONFIRMED)
        ));
    }
}
