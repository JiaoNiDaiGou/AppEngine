package jiaoni.daigou.tools;

import jiaoni.common.appengine.registry.Registry;
import jiaoni.common.model.Env;
import jiaoni.common.test.RemoteApi;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

public class RegistryTool {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            listRegistries(new Registry(remoteApi.getDatastoreService(), Env.DEV));
        }
    }

    private static void listRegistries(Registry registry) {
        List<Pair<String, String>> values = registry
                .scan()
                .collect(Collectors.toList());
        System.out.println("\n =========== Registry ===========\n");
        values.forEach(t -> System.out.println(String.format("[%s] %s", t.getLeft(), t.getRight())));
    }
}
