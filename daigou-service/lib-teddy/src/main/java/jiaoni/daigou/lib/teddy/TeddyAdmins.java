package jiaoni.daigou.lib.teddy;

import jiaoni.common.utils.Secrets;
import jiaoni.daigou.lib.teddy.model.Admin;

public interface TeddyAdmins {
    String JIAONI = "jiaoni";
    String HACK = "hack";
    String BY_ENV = "byEnv";

    static Admin adminOf(final String adminUsername) {
        return Secrets.of("teddy." + adminUsername + ".json").getAsJson(Admin.class);
    }
}
