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
import java.util.stream.Collectors;
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
        final List<Customer> allCustomers = dbClient.scan().collect(Collectors.toList());

        Answers<Customer> customerAnswers = parser.parse(input);

        Set<Customer> knownCustomers = new HashSet<>();
        for (Answer<Customer> answer : customerAnswers) {
            if (answer.hasTarget()) {
                if (StringUtils.isNotBlank(answer.getTarget().getName())) {
                    allCustomers.stream()
                            .filter(t -> t.getName().equals(answer.getTarget().getName()))
                            .forEach(knownCustomers::add);
                }
                if (answer.getTarget().hasPhone()) {
                    allCustomers.stream()
                            .filter(t -> t.getPhone().equals(answer.getTarget().getPhone()))
                            .forEach(knownCustomers::add);
                }
            }
        }

        List<Answer<Customer>> enhancedAnswers = new ArrayList<>();
        for (Customer customer : knownCustomers) {
            enhancedAnswers.add(new Answer<Customer>().setTarget(customer, Conf.HIGH));
        }
        for (Answer<Customer> answer : customerAnswers) {
            Customer parsedCustomer = answer.getTarget();
            boolean alreadyExists = knownCustomers.stream().anyMatch(
                    knownCustomer -> knownCustomer.getName().equals(parsedCustomer.getName())
                            && knownCustomer.getPhone().equals(parsedCustomer.getPhone())
                            && parsedCustomer.getAddressesCount() > 0
                            && knownCustomer.getAddressesList().containsAll(parsedCustomer.getAddressesList()));
            if (!alreadyExists) {
                enhancedAnswers.add(answer);
            }
        }
        // This already sorted.
        return Answers.of(enhancedAnswers, false);
    }
}
