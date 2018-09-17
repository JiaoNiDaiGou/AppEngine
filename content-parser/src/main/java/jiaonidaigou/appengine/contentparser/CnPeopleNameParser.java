package jiaonidaigou.appengine.contentparser;

import jiaonidaigou.appengine.common.people.CnPeopleNames;
import jiaonidaigou.appengine.common.utils.StringUtils2;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CnPeopleNameParser implements Parser<String> {

    private static int confidenceOfChineseName(final String input) {
        if (!StringUtils2.containsOnlyCharTypes(input, StringUtils2.CharType.CHINESE)) {
            return Conf.ZERO;
        }

        List<String> knownLastNames = CnPeopleNames.getInstance().allLastNames();
        for (int i = 0; i < knownLastNames.size(); i++) {
            String lastName = knownLastNames.get(i);

            int firstNameLength = input.length() - lastName.length();
            if (input.startsWith(lastName)) {
                if (i <= 200) { // Top 200 last names
                    if (firstNameLength <= 2) {
                        return Conf.CONFIRMED;
                    } else if (firstNameLength == 3) {
                        return Conf.HIGH;
                    }
                    return Conf.LOW;
                } else if (i <= 800) { // Top 800
                    if (firstNameLength <= 2) {
                        return Conf.HIGH;
                    } else if (firstNameLength == 3) {
                        return Conf.MEDIUM;
                    }
                    return Conf.LOW;
                } else {
                    return Conf.LOW;
                }
            }
        }
        if (input.length() < 4) {
            return Conf.LOW;
        }
        return Conf.ZERO;
    }

    @Override
    public Answers<String> parse(String input) {
        if (StringUtils.isBlank(input)) {
            return Answers.noAnswer();
        }

        input = input.trim();

        List<Answer<String>> toReturn = new ArrayList<>();

        for (String part : StringUtils.split(input)) {
            int conf = confidenceOfChineseName(part);
            if (conf > Conf.ZERO) {
                List<String> afterExtract = StringUtils2.removeEveryMatch(input, part);
                for (String s : afterExtract) {
                    toReturn.add(new Answer<String>()
                            .setTarget(part, conf)
                            .setRawStringAfterExtraction(s));
                }
            }
        }

        return toReturn.isEmpty() ? Answers.noAnswer() : Answers.of(toReturn);
    }
}
