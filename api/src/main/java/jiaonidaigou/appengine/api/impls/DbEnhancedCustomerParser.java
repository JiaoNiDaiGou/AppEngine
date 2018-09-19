package jiaonidaigou.appengine.api.impls;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.contentparser.Answer;
import jiaonidaigou.appengine.contentparser.Answers;
import jiaonidaigou.appengine.contentparser.CnCustomerContactParser;
import jiaonidaigou.appengine.contentparser.Parser;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class DbEnhancedCustomerParser implements Parser<Customer> {
    private final Parser<Customer> parser;
    private CustomerDbClient dbClient;

    @Inject
    public DbEnhancedCustomerParser(final CnCustomerContactParser customerContactParser,
                                    final CustomerDbClient dbClient) {
        this.parser = checkNotNull(customerContactParser);
        this.dbClient = checkNotNull(dbClient);
    }

    @Override
    public Answers<Customer> parse(final String input) {
        Answers<Customer> customerAnswers = parser.parse(input);

        List<Customer> knownCustomers = new ArrayList<>();
        for (Answer<Customer> answer : customerAnswers) {
            if (answer.hasTarget() && StringUtils.isNotBlank(answer.getTarget().getName())
                    && answer.getTarget().hasPhone()) {

            }
        }

        return null;
    }
}
