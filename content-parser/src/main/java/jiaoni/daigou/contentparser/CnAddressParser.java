package jiaoni.daigou.contentparser;

import com.google.common.collect.ImmutableSet;
import jiaoni.common.location.CnLocations;
import jiaoni.common.utils.StringUtils2;
import jiaoni.common.wiremodel.Address;
import jiaoni.common.wiremodel.City;
import jiaoni.common.wiremodel.Region;
import jiaoni.common.wiremodel.Zone;
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
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class CnAddressParser implements Parser<Address> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CnAddressParser.class);

    private final CnLocations LOC = CnLocations.getInstance();
    private final List<City> ALL_CITIES = LOC.allCities();
    private final List<City> ALL_NON_MUNICIPALITY_CITIES = LOC.allNonMunicipalityCities();

    private static final Set<String> ZONE_FLAGS = ImmutableSet
            .<String>builder()
            .addAll(CnLocations.ZONE_FLAGS)
            .add("区", "市", "县", "镇", "乡", "村", "旗", "集")
            .build();

    private static final char[] ALLOWED_CHARS = { '-', '.', '(', ')', '（', '）', '#', '：', ':', '·' };

    private static final int SEARCH_ZONE_FLAG_MAX_LENGTH = 6;

    /**
     * Parse zone part of the address. Return [zone part, the rest]
     */
    private Pair<String, String> parseAddressZone(final String input, final City city) {
        // Check known zones
        List<String> allCityNames = LOC.allPossibleNames(city);
        for (Zone zone : city.getZonesList()) {
            List<String> possibleZonePrefixes = new ArrayList<>();
            for (String zoneName : LOC.allPossibleNames(zone)) {
                possibleZonePrefixes.add(zoneName);
                for (String cityAlias : allCityNames) {
                    possibleZonePrefixes.add(cityAlias + zoneName);
                    possibleZonePrefixes.add(cityAlias + " " + zoneName);
                }
                possibleZonePrefixes.sort((a, b) -> Integer.compare(b.length(), a.length()));
            }
            for (String possibleZonePrefix : possibleZonePrefixes) {
                if (input.startsWith(possibleZonePrefix)) {
                    return Pair.of(zone.getName(), input.substring(possibleZonePrefix.length()));
                }
            }
        }

        // If not found, check first 8 chars to detect zone flags.

        for (int i = 1; i < input.length() && i < SEARCH_ZONE_FLAG_MAX_LENGTH; i++) {
            String ch = input.substring(i, i + 1);
            if (ZONE_FLAGS.contains(ch)) {
                String zone = input.substring(0, i + 1);
                // In case somebody say 河南省许昌市许昌魏都区
                for (String cityName : LOC.allPossibleNames(city)) {
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

    private List<String> allPossibleRegionAndNameCombinations(final City city) {
        Set<String> toReturn = new HashSet<>();
        Region region = LOC.getRegionForCity(city);
        for (String regionName : LOC.allPossibleNames(region)) {
            for (String cityName : LOC.allPossibleNames(city)) {
                toReturn.add(regionName + cityName);
                toReturn.add(regionName + " " + cityName);
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
    private Answer<Address> parseSingleAddress(final String rawInput) {
        boolean unknownPostalCode = false;
        City foundCity = null;
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

        if (input.contains(" 000000")) { // unknown postal code
            unknownPostalCode = true;
        }

        // Find region + city
        loop1:
        for (City city : ALL_NON_MUNICIPALITY_CITIES) {
            for (String regionNameAndCityName : allPossibleRegionAndNameCombinations(city)) {
                if (input.contains(regionNameAndCityName)) {
                    foundCity = city;
                    conf = Conf.HIGH;
                    inputAfterCity = StringUtils.substringAfter(input, regionNameAndCityName).trim();
                    inputBeforeWholeAddress = StringUtils.substringBefore(input, regionNameAndCityName).trim();
                    break loop1;
                }
            }
        }

        // Find city only
        if (foundCity == null) {
            loop2:
            for (City city : ALL_CITIES) {
                for (String cityName : LOC.allPossibleNames(city)) {
                    if (input.contains(cityName)) {
                        foundCity = city;
                        conf = Conf.HIGH;
                        inputAfterCity = StringUtils.substringAfter(input, cityName).trim();
                        inputBeforeWholeAddress = StringUtils.substringBefore(input, cityName).trim();
                        // in case duplicated city name:
                        // 上海市上海市虹口区...
                        for (String duplicatedCityName : LOC.allPossibleNames(city)) {
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
            for (City city : ALL_NON_MUNICIPALITY_CITIES) {
                for (String cityName : LOC.allPossibleNames(city)) {
                    if (input.contains(cityName)) {
                        foundCity = city;
                        conf = Conf.MEDIUM;
                        inputAfterCity = StringUtils.substringAfter(input, cityName).trim();
                        inputBeforeWholeAddress = StringUtils.substringBefore(input, cityName).trim();
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

        // Extract postal code
        String foundPostalCode = "";
        if (!unknownPostalCode) {
            Set<String> postalCodes = new HashSet<>(foundCity.getPostalCodesList());
            if (foundZone != null) {
                foundCity.getZonesList()
                        .stream()
                        .filter(t -> t.getName().equals(foundZone))
                        .forEach(t -> postalCodes.addAll(t.getPostalCodesList()));
            }
            for (String postalCode : postalCodes) {
                if (foundAddress.contains(" " + postalCode)) {
                    foundPostalCode = postalCode;
                    foundAddress = StringUtils2.replaceLast(foundAddress, foundPostalCode, "");
                    break;
                }
            }
        }

        return new Answer<Address>()
                .setRawStringAfterExtraction(inputBeforeWholeAddress)
                .setTarget(Address.newBuilder()
                        .setRegion(foundCity.getRegionName())
                        .setCity(foundCity.getName())
                        .setZone(foundZone == null ? trimToEmpty(foundCity.getName()) : trimToEmpty(StringUtils.replace(foundZone, " ", "")))
                        .setAddress(trimToEmpty(StringUtils.replace(foundAddress, " ", "")))
                        .setPostalCode(foundPostalCode)
                        .build(), conf);
    }
}
