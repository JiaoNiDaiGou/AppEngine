package jiaonidaigou.appengine.tools;

import jiaonidaigou.appengine.api.registry.Registry;
import jiaonidaigou.appengine.common.utils.Environments;
import jiaonidaigou.appengine.tools.remote.RemoteApi;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

import static jiaonidaigou.appengine.api.tasks.DumpJiaoniShippingOrderTaskRunner.REGISTRY_KEY_LAST_DUMP_ID;

public class RegistryTool {
    public static void main(String[] args) throws Exception {
        try (RemoteApi remoteApi = RemoteApi.login()) {
            Registry registry = new Registry(remoteApi.getDatastoreService());
            listRegistries(registry);
        }
    }

    private static void listRegistries(Registry registry) {
        List<Pair<String, String>> values = registry
                .scan()
                .collect(Collectors.toList());
        System.out.println("\n =========== Registry ===========\n");
        values.forEach(t -> System.out.println(String.format("[%s] %s", t.getLeft(), t.getRight())));
    }

    private static void putRegistry(Registry registry) {
        registry.setRegistry(
                Environments.SERVICE_NAME_JIAONIDAIGOU,
                REGISTRY_KEY_LAST_DUMP_ID,
                "134009");
    }
}
