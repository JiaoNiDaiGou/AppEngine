package jiaoni.daigou.contentparser;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static jiaoni.daigou.contentparser.AnswerMatchers.hasAnswers;
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
            assertThat(answers, AnswerMatchers.hasAnswers(
                    AnswerMatchers.atLeast(name, Conf.LOW)
            ));
            allAnswers.addAll(answers.getResults());
        }

        long atLeastHighRate = (allAnswers.stream().filter(t -> t.getConfidence() >= Conf.HIGH).count()) * 100
                / (long) allAnswers.size();
        assertTrue("atLeastHigh rate " + atLeastHighRate + "% is low.", atLeastHighRate > 0.9D);
    }

    @Test
    public void testParseName() {
        assertThat(underTest.parse("王大锤"), AnswerMatchers.hasAnswers(
                AnswerMatchers.is("王大锤", Conf.CONFIRMED)
        ));

        assertThat(underTest.parse("王大锤 马文超"), AnswerMatchers.hasAnswers(
                AnswerMatchers.is("王大锤", Conf.CONFIRMED),
                AnswerMatchers.is("马文超", Conf.CONFIRMED)
        ));

        assertThat(underTest.parse("这不是名字"), AnswerMatchers.noAnswer());
    }
}
