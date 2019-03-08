package jiaoni.daigou.cli;

import jiaoni.common.appengine.access.db.DoNothingMemcache;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import jiaoni.daigou.service.appengine.impls.db.v2.CustomerDbClient;
import jiaoni.daigou.service.appengine.impls.db.v2.ProductBrandDbClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.EnumUtils;

class CliUtils {
    static void print(final Env env, final String template, final Object... args) {
        System.out.println("[" + env + "] " + String.format(template, args));
    }

    static CustomerDbClient customerDbClient(final RemoteApi remoteApi, final Env env) {
        return new CustomerDbClient(env, remoteApi.getDatastoreService(), new DoNothingMemcache());
    }

    static ProductBrandDbClient productBrandDbClient(final RemoteApi remoteApi, final Env env) {
        return new ProductBrandDbClient(env, remoteApi.getDatastoreService(), new DoNothingMemcache());
    }

    static int intArgOf(CommandLine commandLine, String arg) {
        return intArgOf(commandLine, arg, 0);
    }

    static int intArgOf(CommandLine commandLine, String arg, int defaultValue) {
        String str = commandLine.getOptionValue(arg, String.valueOf(defaultValue));
        return Integer.parseInt(str);
    }

    static <T extends Enum<T>> T enumArgOf(CommandLine commandLine, String arg, T defaultValue) {
        String str = commandLine.getOptionValue(arg, defaultValue.name());
        return (T) EnumUtils.getEnum(defaultValue.getClass(), str);
    }
}
