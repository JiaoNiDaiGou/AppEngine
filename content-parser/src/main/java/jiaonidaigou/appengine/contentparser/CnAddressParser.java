package jiaonidaigou.appengine.contentparser;

import com.google.common.collect.ImmutableSet;
import jiaoni.common.location.CnCity;
import jiaoni.common.location.CnLocations;
import jiaoni.common.location.CnZone;
import jiaoni.common.utils.StringUtils2;
import jiaonidaigou.appengine.wiremodel.entity.Address;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jiaoni.common.utils.LocalMeter.meterOff;
import static jiaoni.common.utils.LocalMeter.meterOn;

public class CnAddressParser implements Parser<Address> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CnAddressParser.class);

    private static final Set<String> ZONE_FLAGS = ImmutableSet
            .<String>builder()
            .addAll(CnLocations.ZONE_FLAGS)
            .add("区", "市", "县", "镇", "乡", "村", "旗", "集")
            .build();

    private static final char[] ALLOWED_CHARS = { '-', '.', '(', ')', '（', '#' };

    private static final int SEARCH_ZONE_FLAG_MAX_LENGTH = 6;

    /**
     * Parse zone part of the address. Return [zone part, the rest]
     */
    private static Pair<String, String> parseAddressZone(final String input, final CnCity city) {
        // Check known zones
        for (CnZone zone : city.getZones()) {
            for (String zoneName : zone.getAllPossibleNames()) {
                if (input.startsWith(zoneName)) {
                    return Pair.of(zone.getName(), input.substring(zoneName.length()));
                }
            }
        }

        // If not found, check first 8 chars to detect zone flags.

        for (int i = 1; i < input.length() && i < SEARCH_ZONE_FLAG_MAX_LENGTH; i++) {
            String ch = input.substring(i, i + 1);
            if (ZONE_FLAGS.contains(ch)) {
                String zone = input.substring(0, i + 1);
                // In case somebody say 河南省许昌市许昌魏都区
                for (String cityName : city.getAllPossibleNames()) {
                    if (zone.startsWith(cityName) && zone.length() > cityName.length() + 1) {
                        zone = zone.substring(cityName.length());
                        break;
                    }
                }
                return Pair.of(zone, input.substring(i + 1));
            }
        }

        // Zone not found
        return Pair.of(null, input);
    }

    private List<String> allPossibleRegionAndNameCombinations(final CnCity city) {
        Set<String> toReturn = new HashSet<>();
        for (String regionName : city.getRegion().getAllPossibleNames()) {
            for (String cityName : city.getAllPossibleNames()) {
                toReturn.add(regionName + cityName);
            }
        }
        List<String> sorted = new ArrayList<>(toReturn);
        sorted.sort((a, b) -> Integer.compare(b.length(), a.length()));
        return sorted;
    }

    @Override
    public Answers<Address> parse(String input) {
        meterOn();
        LOGGER.info("input: {}", input);

        if (StringUtils.isBlank(input)) {
            return Answers.noAnswer();
        }

        List<Answer<Address>> toReturn = new ArrayList<>();

        String left = input;
        while (StringUtils.isNotBlank(left)) {
            Answer<Address> answer = parseSingleAddress(left);
            if (answer != null && answer.hasTarget()) {
                toReturn.add(answer);
                left = answer.getRawStringAfterExtraction();
            } else {
                break;
            }
        }

        meterOff();
        return toReturn.isEmpty() ? Answers.noAnswer() : Answers.of(toReturn);
    }

    /**
     * Find backwards.
     */
    public Answer<Address> parseSingleAddress(final String rawInput) {
        final List<CnCity> allCities = CnLocations.getInstance().allCities();
        final List<CnCity> allNonMunicipalityCities = CnLocations.getInstance().allNonMunicipalityCities();

        CnCity foundCity = null;
        String inputAfterCity = null;
        String inputBeforeWholeAddress = null;
        int conf = Conf.ZERO;

        String input = rawInput;
        input = input.trim();
        input = StringUtils2.replaceNonCharTypesWith(input,
                new StringUtils2.CharType[]{
                        StringUtils2.CharType.CHINESE,
                        StringUtils2.CharType.A2Z,
                        StringUtils2.CharType.DIGIT
                },
                " ",
                ALLOWED_CHARS);

        // Find region + city
        loop1:
        for (CnCity city : allNonMunicipalityCities) {
            for (String regionNameAndCityName : allPossibleRegionAndNameCombinations(city)) {
                if (input.contains(regionNameAndCityName)) {
                    foundCity = city;
                    conf = Conf.HIGH;
                    inputAfterCity = StringUtils.substringAfterLast(input, regionNameAndCityName).trim();
                    inputBeforeWholeAddress = StringUtils.substringBeforeLast(input, regionNameAndCityName).trim();
                    break loop1;
                }
            }
        }

        // Find city only
        if (foundCity == null) {
            loop2:
            for (CnCity city : allCities) {
                for (String cityName : city.getAllPossibleNames()) {
                    if (input.contains(cityName)) {
                        foundCity = city;
                        conf = Conf.HIGH;
                        inputAfterCity = StringUtils.substringAfterLast(input, cityName).trim();
                        inputBeforeWholeAddress = StringUtils.substringBeforeLast(input, cityName).trim();
                        // in case duplicated city name:
                        // 上海市上海市虹口区...
                        for (String duplicatedCityName : city.getAllPossibleNames()) {
                            if (inputBeforeWholeAddress.endsWith(duplicatedCityName)) {
                                inputBeforeWholeAddress = inputBeforeWholeAddress.substring(
                                        0, inputBeforeWholeAddress.length() - duplicatedCityName.length());
                            }
                        }
                        break loop2;
                    }
                }
            }
        }

        // Find city only
        if (foundCity == null) {
            loop3:
            for (CnCity city : allNonMunicipalityCities) {
                for (String cityName : city.getAllPossibleNames()) {
                    if (input.contains(cityName)) {
                        foundCity = city;
                        conf = Conf.MEDIUM;
                        inputAfterCity = StringUtils.substringAfterLast(input, cityName).trim();
                        inputBeforeWholeAddress = StringUtils.substringBeforeLast(input, cityName).trim();
                        break loop3;
                    }
                }
            }
        }

        if (foundCity == null) {
            return null;
        }

        // Extract zone
        Pair<String, String> zoneInfo = parseAddressZone(inputAfterCity, foundCity);
        String foundZone = zoneInfo.getLeft();
        String foundAddress = zoneInfo.getRight();

        if (conf == Conf.HIGH && foundZone != null) {
            conf = Conf.CONFIRMED;
        }

        return new Answer<Address>()
                .setRawStringAfterExtraction(inputBeforeWholeAddress)
                .setTarget(Address.newBuilder()
                        .setRegion(foundCity.getRegion().getName())
                        .setCity(foundCity.getName())
                        .setZone(foundZone == null ? foundCity.getName() : foundZone)
                        .setAddress(StringUtils2.removeDuplicatedSpaces(foundAddress))
                        .build(), conf);
    }
}
