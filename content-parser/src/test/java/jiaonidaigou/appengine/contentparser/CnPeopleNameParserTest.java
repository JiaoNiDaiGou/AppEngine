package jiaonidaigou.appengine.contentparser;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static jiaonidaigou.appengine.contentparser.AnswerMatchers.atLeast;
import static jiaonidaigou.appengine.contentparser.AnswerMatchers.hasAnswers;
import static jiaonidaigou.appengine.contentparser.AnswerMatchers.is;
import static jiaonidaigou.appengine.contentparser.AnswerMatchers.noAnswer;
import static jiaonidaigou.appengine.contentparser.Conf.CONFIRMED;
import static jiaonidaigou.appengine.contentparser.Conf.HIGH;
import static jiaonidaigou.appengine.contentparser.Conf.LOW;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CnPeopleNameParserTest {
    private final CnPeopleNameParser underTest = new CnPeopleNameParser();

    @Test
    public void testKnownCustomerNames() throws Exception {
        List<String> knownNames;
        try (Reader reader = new InputStreamReader(
                Resources.getResource("cn_names.dat").openStream(), Charsets.UTF_8)) {
            knownNames = CharStreams.readLines(reader);
        }
        List<Answer<String>> allAnswers = new ArrayList<>();
        for (String name : knownNames) {
            Answers<String> answers = underTest.parse(name);
            assertThat(answers, hasAnswers(
                    atLeast(name, LOW)
            ));
            allAnswers.addAll(answers.getResults());
        }

        long atLeastHighRate = (allAnswers.stream().filter(t -> t.getConfidence() >= HIGH).count()) * 100
                / (long) allAnswers.size();
        assertTrue("atLeastHigh rate " + atLeastHighRate + "% is low.", atLeastHighRate > 0.9D);
    }

    @Test
    public void testParseName() {
        assertThat(underTest.parse("王大锤"), hasAnswers(
                is("王大锤", CONFIRMED)
        ));

        assertThat(underTest.parse("王大锤 马文超"), hasAnswers(
                is("王大锤", CONFIRMED),
                is("马文超", CONFIRMED)
        ));

        assertThat(underTest.parse("这不是名字"), noAnswer());
    }
}
