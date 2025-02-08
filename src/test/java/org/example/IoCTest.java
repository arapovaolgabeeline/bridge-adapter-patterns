package org.example;

import org.example.interfaces.ICommand;
import org.example.ioc.IocResolveDependencyStrategySetterCommand;
import org.example.interfaces.IDependencyResolverStrategyUpdater;
import org.example.interfaces.IDependencyResolverStrategy;
import org.example.ioc.IoC;
import org.example.ioc.IocContextCleaner;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IoCTest {

    @BeforeEach
    public void beforeEach() {
        IocContextCleaner.clean();
    }

    @Test
    void shouldResolveUpdateIocDependency() {
        String dependencyName = "Update Ioc Resolve Dependency Strategy";

        Object[] args = new Object[1];
        args[0] = (IDependencyResolverStrategyUpdater) newDependency -> new IDependencyResolverStrategy() {
            @Override
            public <T> T resolve(String dependency, Object[] args1) {
                return (T) new IocResolveDependencyStrategySetterCommand(newDependency1 -> null);
            }
        };

        ICommand resolve = IoC.<ICommand>resolve(dependencyName, args);

        Assertions.assertNotNull(resolve);
        Assertions.assertInstanceOf(IocResolveDependencyStrategySetterCommand.class, resolve);
        resolve.execute();
    }

    @Test
    void shouldThrowExceptionWhenUpdateIocResolveDependencyWasNotInvoke() {
        String dependencyName = "Unknown Strategy";

        Object[] args = new Object[1];
        args[0] = new IDependencyResolverStrategyUpdater() {
            @Override
            public IDependencyResolverStrategy update(IDependencyResolverStrategy newIDependencyResolverStrategy) {
                return new IDependencyResolverStrategy() {
                    @Override
                    public <T> T resolve(String dependency, Object[] args) {
                        return (T) new IocResolveDependencyStrategySetterCommand(newDependency1 -> null);
                    }
                };
            }
        };

        assertThrows(IllegalArgumentException.class, () -> IoC.<ICommand>resolve(dependencyName, args));
    }
}