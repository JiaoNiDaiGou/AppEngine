package jiaoni.daigou.contentparser;

import jiaoni.common.utils.StringUtils2;
import jiaoni.daigou.wiremodel.entity.PhoneNumber;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jiaoni.common.utils.LocalMeter.meterOff;
import static jiaoni.common.utils.LocalMeter.meterOn;

public class CnCellPhoneParser implements Parser<PhoneNumber> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CnCellPhoneParser.class);

    private static final Pattern CHINESE_CELL_PHONE = Pattern.compile("(1\\d{10})");

    @Override
    public Answers<PhoneNumber> parse(String input) {
        meterOn();
        LOGGER.info("input: {}", input);

        if (StringUtils.isBlank(input)) {
            return Answers.noAnswer();
        }

        input = input.trim();
        Matcher matcher = CHINESE_CELL_PHONE.matcher(input);
        List<Answer<PhoneNumber>> toReturn = new ArrayList<>();
        while (matcher.find()) {
            String phone = matcher.group();
            List<String> afterExtract = StringUtils2.removeEveryMatch(input, phone);
            for (String s : afterExtract) {
                toReturn.add(new Answer<PhoneNumber>()
                        .setTarget(PhoneNumber.newBuilder().setCountryCode("86").setPhone(phone).build(), Conf.CONFIRMED)
                        .setRawStringAfterExtraction(s));
            }
        }

        meterOff();
        return toReturn.isEmpty() ? Answers.noAnswer() : Answers.of(toReturn);
    }
}
