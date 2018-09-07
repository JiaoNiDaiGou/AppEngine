package jiaonidaigou.appengine.api.auth;

import com.google.common.collect.Sets;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

class UserPrincipal implements Principal {
    private final String name;
    private final AuthenticationScheme scheme;
    private final Set<String> roles;
    private final boolean secure;

    UserPrincipal(final String name,
                  final AuthenticationScheme scheme,
                  final boolean secure,
                  final Set<String> roles) {
        this.name = name;
        this.scheme = scheme;
        this.roles = new HashSet<>(roles);
        this.secure = secure;
    }

    UserPrincipal(final String name,
                  final AuthenticationScheme scheme,
                  final boolean secure,
                  final String... roles) {
        this(name, scheme, secure, Sets.newHashSet(roles));
    }

    @Override
    public String getName() {
        return name;
    }

    AuthenticationScheme getScheme() {
        return scheme;
    }

    Set<String> getRoles() {
        return new HashSet<>(roles);
    }

    boolean isSecure() {
        return secure;
    }

    enum AuthenticationScheme {
        GOOGLE_OAUTH2,
        GAE_TASK_QUEUE,
    }
}
