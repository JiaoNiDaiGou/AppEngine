package jiaonidaigou.appengine.contentparser;

import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Test
    public void testDD() {
        List<Character> a = Arrays.asList('3', '4');
        List<Character> b = Arrays.asList('5', '6');
        int n = a.size() - 1;
        int m = b.size() - 1;
        int carry = 0;
        List<Integer> result = new ArrayList<>(n + m);
        int i = 0, j = 0;
        for (i = n; i >= 0; --i) {
            for (j = m; j >= 0; --j) {
                int temp = (a.get(i) - '0') * (b.get(j) - '0');
                result.set(i + j - 1, (carry + temp) % 10);
                carry = temp / 10;
            }
        }
        if (carry > 0) {
            result.set(i + j - 1, carry);
        }

        System.out.println(result);
    }
}
