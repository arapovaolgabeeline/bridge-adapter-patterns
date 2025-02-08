package org.example.ioc;

import org.example.interfaces.IDependencyResolverStrategy;
import org.example.interfaces.IDependencyResolverStrategyUpdater;

public class IocContextCleaner extends IoC {

    public static void clean() {
        strategy = new IDependencyResolverStrategy() {
            @Override
            public <T> T resolve(String dependency, Object[] args) {
                if ("Update Ioc Resolve Dependency Strategy".equals(dependency)) {
                    return (T) new IocResolveDependencyStrategySetterCommand((IDependencyResolverStrategyUpdater) args[0]);
                } else {
                    throw new IllegalArgumentException("Dependency " + dependency + " was not found.");
                }
            }
        };
    }

}
