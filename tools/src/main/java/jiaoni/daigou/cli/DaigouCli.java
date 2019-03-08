package jiaoni.daigou.cli;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;

import static jiaoni.daigou.cli.CliUtils.enumArgOf;
import static jiaoni.daigou.cli.CustomerCmdHandler.handleGetCustomer;

public class DaigouCli {

    //
    // Commands
    //
    private static final String CMD_CUSTOMER = "customer";
    private static final String CMD_PRODUCT = "product";
    private static final String CMD_BRAND = "brand";

    //
    // Verbs
    //
    private static final String VERB_GET = "get";

    //
    // Args
    //
    static final String ARG_ENV = "env";
    static final String ARG_ID = "id";
    static final String ARG_LIMIT = "limit";
    static final String ARG_NAME = "name";
    static final String ARG_PHONE = "phone";
    static final String ARG_HELP = "help";

    //
    // Options
    //
    private static final Option OPTION_ENV = Option.builder()
            .longOpt(ARG_ENV)
            .desc("Environment. E.g. prod, dev.")
            .hasArg()
            .build();
    private static final Option OPTION_ID = Option.builder()
            .longOpt(ARG_ID)
            .desc("ID")
            .hasArg()
            .build();
    private static final Option OPTION_LIMIT = Option.builder()
            .longOpt(ARG_LIMIT)
            .desc("Query limit.")
            .hasArg()
            .build();
    private static final Option OPTION_NAME = Option.builder()
            .longOpt(ARG_NAME)
            .desc("Name")
            .hasArg()
            .build();
    private static final Option OPTION_PHONE = Option.builder()
            .longOpt(ARG_PHONE)
            .desc("Phone number.")
            .hasArg()
            .build();

    private static final Table<String, String, Options> COMMANDS = ImmutableTable
            .<String, String, Options>builder()
            //
            // Customer
            //
            .put(CMD_CUSTOMER, VERB_GET, new Options()
                    .addOption(OPTION_ENV)
                    .addOption(OPTION_ID)
                    .addOption(OPTION_NAME)
                    .addOption(OPTION_PHONE)
                    .addOption(OPTION_LIMIT)
            )
            //
            // Product brand
            //
            .put(CMD_BRAND, VERB_GET, new Options()
                    .addOption(OPTION_ENV)
                    .addOption(OPTION_LIMIT))
            .build();

    public static void main(String[] args) {
        boolean hasHelp = Arrays.stream(args).anyMatch("--help"::equalsIgnoreCase);
        final String cmd = (args.length > 0 && COMMANDS.containsRow(args[0])) ? args[0] : null;
        final String verb = (cmd != null && args.length > 1 && COMMANDS.row(cmd).containsKey(args[1])) ? args[1] : null;
        if (hasHelp || cmd == null || verb == null) {
            printHelper(cmd, verb);
            return;
        }

        Options options = COMMANDS.get(cmd, verb);
        CommandLine commandLine;
        try {
            String[] params = Arrays.copyOfRange(args, 1, args.length);
            commandLine = new DefaultParser().parse(options, params);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if (commandLine.hasOption(ARG_HELP)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp(cmd + " " + verb, options);
            return;
        }

        final Env env = enumArgOf(commandLine, ARG_ENV, Env.PROD);
        try (RemoteApi remoteApi = RemoteApi.login()) {
            printHeader();

            if (CMD_CUSTOMER.equals(cmd)) {
                if (VERB_GET.equals(verb)) {
                    handleGetCustomer(commandLine, remoteApi, env);
                }
            }

            printTailer();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void printHelper(final String cmd, final String verb) {
        HelpFormatter helpFormatter = new HelpFormatter();
        if (StringUtils.isNoneBlank(cmd, verb)) {
            Options options = COMMANDS.get(cmd, verb);
            helpFormatter.printHelp(cmd + " " + verb, options);
        } else if (StringUtils.isNotBlank(cmd)) {
            for (Map.Entry<String, Options> entry : COMMANDS.row(cmd).entrySet()) {
                helpFormatter.printHelp(cmd + " " + entry.getKey(), entry.getValue());
            }
        } else {
            for (Table.Cell<String, String, Options> cell : COMMANDS.cellSet()) {
                helpFormatter.printHelp(cell.getRowKey() + " " + cell.getColumnKey(), cell.getValue());
            }
        }
    }

    private static void printHeader() {
        String toPrint = "\n\n" +
                "       _ _             _   _ _ _____        _  _____                _____ _      _____ \n" +
                "      | (_)           | \\ | (_)  __ \\      (_)/ ____|              / ____| |    |_   _|\n" +
                "      | |_  __ _  ___ |  \\| |_| |  | | __ _ _| |  __  ___  _   _  | |    | |      | |  \n" +
                "  _   | | |/ _` |/ _ \\| . ` | | |  | |/ _` | | | |_ |/ _ \\| | | | | |    | |      | |  \n" +
                " | |__| | | (_| | (_) | |\\  | | |__| | (_| | | |__| | (_) | |_| | | |____| |____ _| |_ \n" +
                "  \\____/|_|\\__,_|\\___/|_| \\_|_|_____/ \\__,_|_|\\_____|\\___/ \\__,_|  \\_____|______|_____|\n" +
                "                                                                                       \n" +
                "                                                                                       " +
                "\n\n";
        System.out.println(toPrint);
    }

    private static void printTailer() {
        System.out.println("\n=======================================\n");
    }
}
