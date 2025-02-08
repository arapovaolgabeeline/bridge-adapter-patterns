package org.example.ioc;

import org.example.interfaces.IDependencyResolverStrategyUpdater;
import org.example.interfaces.IDependencyResolverStrategy;

public class IoC {
    static IDependencyResolverStrategy strategy = new IDependencyResolverStrategy() {
        @Override
        public <T> T resolve(String dependency, Object[] args) {
            if ("Update Ioc Resolve Dependency Strategy".equals(dependency)) {
                return (T) new IocResolveDependencyStrategySetterCommand((IDependencyResolverStrategyUpdater) args[0]);
            } else {
                throw new IllegalArgumentException("Dependency " + dependency + " was not found.");
            }
        }
    };

    public static <T> T resolve(String dependency, Object[] args) {
        return (T) strategy.resolve(dependency, args);
    }

}
