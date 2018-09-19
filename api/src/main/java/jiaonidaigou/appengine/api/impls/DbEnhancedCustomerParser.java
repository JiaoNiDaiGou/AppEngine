package jiaonidaigou.appengine.api.impls;

import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.api.guice.JiaoNiDaiGou;
import jiaonidaigou.appengine.contentparser.Answer;
import jiaonidaigou.appengine.contentparser.Answers;
import jiaonidaigou.appengine.contentparser.CnCustomerContactParser;
import jiaonidaigou.appengine.contentparser.Conf;
import jiaonidaigou.appengine.contentparser.Parser;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class DbEnhancedCustomerParser implements Parser<Customer> {
    private final Parser<Customer> parser;
    private CustomerDbClient dbClient;

    @Inject
    public DbEnhancedCustomerParser(final CnCustomerContactParser customerContactParser,
                                    @JiaoNiDaiGou final CustomerDbClient dbClient) {
        this.parser = checkNotNull(customerContactParser);
        this.dbClient = checkNotNull(dbClient);
    }

    @Override
    public Answers<Customer> parse(final String input) {
        Answers<Customer> customerAnswers = parser.parse(input);

        Set<Customer> knownCustomers = new HashSet<>();
        for (Answer<Customer> answer : customerAnswers) {
            if (answer.hasTarget()) {
                if (StringUtils.isNotBlank(answer.getTarget().getName())) {
                    dbClient.scan()
                            .filter(t -> t.getName().equals(answer.getTarget().getName()))
                            .forEach(knownCustomers::add);
                }
                if (answer.getTarget().hasPhone()) {
                    dbClient.scan()
                            .filter(t -> t.getPhone().equals(answer.getTarget().getPhone()))
                            .forEach(knownCustomers::add);
                }
            }
        }

        List<Answer<Customer>> enhancedAnswers = new ArrayList<>(customerAnswers.getResults());
        knownCustomers.stream().map(t -> new Answer<Customer>().setTarget(t, Conf.HIGH)).forEach(enhancedAnswers::add);
        return Answers.of(enhancedAnswers);
    }
}
