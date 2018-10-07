package jiaoni.daigou.contentparser;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.wiremodel.Address;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CnAddressParserTest {
    private final CnAddressParser underTest = new CnAddressParser();

    @Test
    public void testParseKnownAddress() throws Exception {
        List<String> lines;
        try (Reader reader = new InputStreamReader(Resources.getResource("cn_address.dat").openStream(), Charsets.UTF_8)) {
            lines = CharStreams.readLines(reader);
        }
        for (int i = 0; i < lines.size(); i += 2) {
            String input = lines.get(i);
            String[] expectedParts = StringUtils.split(lines.get(i + 1), "|");
            Address expectedAddress = Address.newBuilder()
                    .setRegion(expectedParts[0])
                    .setCity(expectedParts[1])
                    .setZone(expectedParts[2])
                    .setAddress(expectedParts[3])
                    .build();
            Answers<Address> result = underTest.parse(input);

            System.out.println(input + "  =>");
            for (Answer<Address> answer : result) {
                assertTrue(answer.getConfidence() >= Conf.MEDIUM);

                System.out.println(String.format("  %s %s %s %s",
                        answer.getTarget().getRegion(),
                        answer.getTarget().getCity(),
                        answer.getTarget().getZone(),
                        answer.getTarget().getAddress()));

//                assertTrue(answer.getTarget().getZone().contains(expectedParts[2]) || );
//                assertTrue(answer.getTarget().getRegion().contains(expectedParts[0]));
            }
            System.out.println("\n");
        }
    }

    @Test
    public void testParseZone() throws Exception {
        String input = "河南许昌魏都电视台";
        System.out.println(ObjectMapperProvider.get().writerWithDefaultPrettyPrinter().writeValueAsString(underTest.parse(input)));
        assertThat(underTest.parse(input), AnswerMatchers.hasAnswers(
                AnswerMatchers.atLeast(Address.newBuilder()
                        .setRegion("河南省")
                        .setCity("许昌市")
                        .setZone("魏都区")
                        .setAddress("电视台")
                        .build(), Conf.HIGH)
        ));
    }
}
