package jiaoni.daigou.contentparser;

import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.location.CnLocations;
import jiaoni.common.utils.FileUtils;
import jiaoni.common.wiremodel.Address;
import jiaoni.common.wiremodel.City;
import jiaoni.common.wiremodel.Region;
import jiaoni.common.wiremodel.Zone;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CnAddressParserTest {
    private final CnAddressParser underTest = new CnAddressParser();

    @Test
    public void testParseSingleAddress() {
        String input = "上海|上海|普陀区|曹杨新村街道 中山北路2907号上海市旧机动车交易市场 检测线|";
        Answers<Address> results = underTest.parse(input);
        for (Answer<Address> result : results) {
            Address address = result.getTarget();
//        Region actualRegion = CnLocations.getInstance().searchRegion(result.getRegion()).get(0);
//        City actualCity = CnLocations.getInstance().searchCity(actualRegion, result.getCity()).get(0);
//        Zone actualZone = actualCity.getZonesList().stream().filter(t -> t.getName().equals(result.getZone())).findFirst().orElse(null);
//        System.out.println(ObjectMapperProvider.prettyToJson(actualZone));
            System.out.println(ObjectMapperProvider.prettyToJson(address));
        }
    }

    /**
     * Use {@link #testParseSingleAddress()} to debug single address.
     */
    @Test
    public void testParseKnownAddress() throws Exception {
        List<String> lines = FileUtils.readLinesFromResource("cn_address.dat");
        for (String line : lines) {
            if (line.startsWith("//")) {
                continue;
            }
            String[] parts = StringUtils.split(line, "|");
            String region = parts[0];
            String city = parts[1];
            String zone = parts.length > 3 ? parts[2] : "";
            String address = parts.length > 3 ? parts[3] : parts[2];
            String postalCode = parts.length > 4 ? parts[4] : "";

            Answers<Address> answers = underTest.parse(line);
            assertTrue(answers.size() >= 1);
            Address result = answers.getResults().get(0).getTarget();

            Region actualRegion = CnLocations.getInstance().searchRegion(result.getRegion()).get(0);
            assertTrue(line,
                    CnLocations.getInstance().allPossibleNames(actualRegion).contains(region));

            City actualCity = CnLocations.getInstance().searchCity(actualRegion, result.getCity()).get(0);
            assertTrue(line,
                    CnLocations.getInstance().allPossibleNames(actualCity).contains(city));

            Zone actualZone = actualCity.getZonesList().stream().filter(t -> t.getName().equals(result.getZone())).findFirst().orElse(null);
            if (actualZone != null) {
                assertTrue(line, CnLocations.getInstance().allPossibleNames(actualZone).contains(zone));
            } else {
                System.out.println(result.getZone());
            }

            assertEquals(line,
                    StringUtils.replace(address, " ", ""), result.getAddress());

            assertEquals(line,
                    postalCode, result.getPostalCode());
        }
    }

    @Test
    public void testParseZone() {
        String input = "河南许昌魏都电视台";
        System.out.println(ObjectMapperProvider.prettyToJson(underTest.parse(input)));
        assertThat(underTest.parse(input), AnswerMatchers.hasAnswers(
                AnswerMatchers.atLeast(Address.newBuilder()
                        .setRegion("河南省")
                        .setCity("许昌市")
                        .setZone("魏都区")
                        .setAddress("电视台")
                        .build(), Conf.HIGH)
        ));
    }

    @Test
    public void testParsePostalCode() throws Exception {
        String input = "北京通州区梨园镇梨园翠景北里瑞都景园12号楼2单元702室,101100";
        Answers<Address> results = underTest.parse(input);
        assertEquals(1, results.size());
        Answer<Address> answer = results.getResults().get(0);
        assertTrue(answer.getConfidence() >= Conf.HIGH);
        assertEquals(Address.newBuilder()
                        .setAddress("梨园镇梨园翠景北里瑞都景园12号楼2单元702室")
                        .setRegion("北京市")
                        .setCity("北京市")
                        .setZone("通州区")
                        .setPostalCode("101100")
                        .build(),
                answer.getTarget());
    }
}
