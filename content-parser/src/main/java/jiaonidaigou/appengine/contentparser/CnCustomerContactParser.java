package jiaonidaigou.appengine.contentparser;

import jiaonidaigou.appengine.wiremodel.entity.Address;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static jiaoni.common.utils.LocalMeter.meterOff;
import static jiaoni.common.utils.LocalMeter.meterOn;
import static jiaonidaigou.appengine.contentparser.Answers.noAnswer;
import static jiaonidaigou.appengine.contentparser.Answers.useInputIfNoAnswer;
import static jiaonidaigou.appengine.contentparser.Answers.weightedAverage;

public class CnCustomerContactParser implements Parser<Customer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CnCustomerContactParser.class);

    private CnPeopleNameParser peopleNameParser;
    private CnCellPhoneParser phoneParser;
    private CnAddressParser addressParser;

    public CnCustomerContactParser() {
        addressParser = new CnAddressParser();
        peopleNameParser = new CnPeopleNameParser();
        phoneParser = new CnCellPhoneParser();
    }

    @Override
    public Answers<Customer> parse(final String rawInput) {
        meterOn();
        LOGGER.info("input: {}", rawInput);

        if (rawInput == null) {
            return Answers.noAnswer();
        }

        String input = rawInput.trim();

        List<Answer<Customer>> customers = new ArrayList<>();

        // Phone first
        Answers<PhoneNumber> phoneAnswers = useInputIfNoAnswer(phoneParser.parse(input), input);
        for (Answer<PhoneNumber> phoneAnswer : phoneAnswers) {
            PhoneNumber phone = phoneAnswer.getTarget();
            input = phoneAnswer.getRawStringAfterExtraction();

            // People name next
            Answers<String> nameAnswers = useInputIfNoAnswer(peopleNameParser.parse(input), input);
            for (Answer<String> nameAnswer : nameAnswers) {
                String name = nameAnswer.getTarget();
                input = nameAnswer.getRawStringAfterExtraction();

                // Address last
                Answers<Address> addressAnswers = useInputIfNoAnswer(addressParser.parse(input), input);
                for (Answer<Address> addressAnswer : addressAnswers) {
                    Address address = addressAnswer.getTarget();
                    input = addressAnswer.getRawStringAfterExtraction();

                    if (phone == null && StringUtils.isBlank(name) && address == null) {
                        continue;
                    }

                    Customer.Builder builder = Customer.newBuilder();
                    if (phone != null) {
                        builder.setPhone(phone);
                    }
                    if (StringUtils.isNotBlank(name)) {
                        builder.setName(name);
                    }
                    if (address != null) {
                        builder.addAddresses(address);
                    }

                    // Conf rule: name 20% phone 40% address 40%
                    int conf = weightedAverage(
                            Pair.of(nameAnswer, 2),
                            Pair.of(phoneAnswer, 4),
                            Pair.of(addressAnswer, 4));

                    customers.add(new Answer<Customer>()
                            .setTarget(builder.build(), conf)
                            .setRawStringAfterExtraction(input));
                }
            }
        }

        if (customers.isEmpty()) {
            return noAnswer();
        }
        customers.sort((a, b) -> Integer.compare(b.getConfidence(), a.getConfidence()));

        meterOff();
        return Answers.of(customers);
    }
}
