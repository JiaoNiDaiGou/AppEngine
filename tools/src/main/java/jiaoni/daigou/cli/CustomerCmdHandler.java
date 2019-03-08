package jiaoni.daigou.cli;

import jiaoni.common.json.ObjectMapperProvider;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient;
import jiaoni.daigou.v2.entity.Customer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import static jiaoni.daigou.cli.CliUtils.customerDbClient;
import static jiaoni.daigou.cli.CliUtils.intArgOf;
import static jiaoni.daigou.cli.CliUtils.print;
import static jiaoni.daigou.cli.DaigouCli.ARG_ID;
import static jiaoni.daigou.cli.DaigouCli.ARG_LIMIT;
import static jiaoni.daigou.cli.DaigouCli.ARG_NAME;
import static jiaoni.daigou.cli.DaigouCli.ARG_PHONE;

class CustomerCmdHandler {
    private static final int DEFAULT_QUERY_LIMIT = 3;

    static void handleGetCustomer(final CommandLine commandLine,
                                  final RemoteApi remoteApi,
                                  final Env env) {
        final CustomerDbClient dbClient = customerDbClient(remoteApi, env);

        final String id = commandLine.getOptionValue(ARG_ID);
        final String name = commandLine.getOptionValue(ARG_NAME);
        final String phone = commandLine.getOptionValue(ARG_PHONE);

        if (StringUtils.isNotBlank(id)) {
            // Get by ID
            print(env, "Get customer by '%s':", id);
            Customer customer = dbClient.getById(id);
            if (customer == null) {
                print(env, "No customer found.");
            }
        } else {
            // Query
            int limit = intArgOf(commandLine, ARG_LIMIT, DEFAULT_QUERY_LIMIT);
            List<Customer> customers = dbClient.scan()
                    .filter(t -> StringUtils.isBlank(name) || t.getName().contains(name))
                    .filter(t -> StringUtils.isBlank(phone) || t.getPhone().equals(phone))
                    .limit(limit)
                    .collect(Collectors.toList());
            print(env, "Found %s customers.", customers.size());
            System.out.println("---------------------");
            for (Customer customer : customers) {
                print(env, ObjectMapperProvider.prettyToJson(customer));
                System.out.println("---------------------");
            }
        }
    }
}
