package jiaonidaigou.appengine.api.auth;

import com.google.common.collect.Sets;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

class UserPrincipal implements Principal {
    private final String name;
    private final AuthenticationScheme scheme;
    private final Set<Role> roles;
    private final boolean secure;

    UserPrincipal(final String name,
                  final AuthenticationScheme scheme,
                  final boolean secure,
                  final Set<Role> roles) {
        this.name = name;
        this.scheme = scheme;
        this.roles = new HashSet<>(roles);
        this.secure = secure;
    }

    UserPrincipal(final String name,
                  final AuthenticationScheme scheme,
                  final boolean secure,
                  final Role... roles) {
        this(name, scheme, secure, Sets.newHashSet(roles));
    }

    @Override
    public String getName() {
        return name;
    }

    AuthenticationScheme getScheme() {
        return scheme;
    }

    Set<Role> getRoles() {
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
