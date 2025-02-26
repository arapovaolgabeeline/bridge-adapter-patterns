package org.example.commands;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.example.interfaces.ICommand;
import org.example.interfaces.IDependency;
import org.example.interfaces.IDependencyResolverStrategyUpdater;
import org.example.interfaces.IDependencyResolverStrategy;
import org.example.ioc.IoC;
import org.example.resolvers.DependencyResolver;

import static org.example.commands.GenerateAdapterCommand.DEFAULT_ADAPTER_POSTFIX;

public class InitCommand implements ICommand {
    private static final ConcurrentMap<String, IDependency> rootScope = new ConcurrentHashMap<>();
    private static final String PARENT_SCOPE_DEPENDENCY_NAME = "IoC.Scope.Parent";
    private static final String CREATE_EMPTY_SCOPE_DEPENDENCY_NAME = "IoC.Scope.Create.Empty";
    private static final String CURRENT_SCOPE_DEPENDENCY_NAME = "IoC.Scope.Current";
    protected static boolean initialized = false;
    protected static final ThreadLocal<Object> currentScope = new ThreadLocal<>();

    @Override
    public void execute() {
        if (initialized) {
            return;
        }

        synchronized (rootScope) {
            rootScope.put("Adapter", (Object[] args) -> {
                GenerateAdapterCommand generateAdapterCommand = new GenerateAdapterCommand((Class) args[0], args[1]);
                generateAdapterCommand.execute();
                return IoC.resolve(((Class) args[0]).getSimpleName()
                        .substring(1)
                        .concat(DEFAULT_ADAPTER_POSTFIX), new Object[]{});
            });

            rootScope.put("IoC.Scope.Current.Set", (Object[] args) -> new SetCurrentScopeCommand(args[0]));
            rootScope.put("IoC.Scope.Current.Clear", (Object[] args) -> new ClearCurrentScopeCommand());
            rootScope.put(CURRENT_SCOPE_DEPENDENCY_NAME, (Object[] args) -> Objects.isNull(currentScope.get()) ? rootScope : currentScope.get());
            rootScope.put(PARENT_SCOPE_DEPENDENCY_NAME, (Object[] args) -> {
                throw new RuntimeException("The root scope has no a parent scope.");
            });
            rootScope.put(CREATE_EMPTY_SCOPE_DEPENDENCY_NAME, (Object[] args) -> new ConcurrentHashMap<>());
            rootScope.put("IoC.Scope.Create", (Object[] args) -> {
                ConcurrentMap<String, IDependency> createdScope = IoC.resolve(CREATE_EMPTY_SCOPE_DEPENDENCY_NAME,
                        new Object[]{});
                if (args.length != 0) {
                    Object parentScope = args[0];
                    createdScope.put(PARENT_SCOPE_DEPENDENCY_NAME, arguments -> parentScope);
                } else {
                    Object parentScope = IoC.resolve(CURRENT_SCOPE_DEPENDENCY_NAME, new Object[]{});
                    createdScope.put(PARENT_SCOPE_DEPENDENCY_NAME, arguments -> parentScope);
                }
                return createdScope;
            });
            rootScope.put("IoC.Register", (Object[] args) -> new RegisterDependencyCommand((String) args[0],
                    (IDependency) args[1]));

            Object[] args = new Object[1];
            args[0] = (IDependencyResolverStrategyUpdater) oldStrategy -> new IDependencyResolverStrategy() {
                @Override
                public <T> T resolve(String dependency, Object[] args) {
                    Object scope = Objects.nonNull(currentScope.get()) ? currentScope.get() : rootScope;
                    DependencyResolver dependencyResolver = new DependencyResolver(scope);
                    return (T) dependencyResolver.resolve(dependency, args);
                }
            };

            IoC.<ICommand>resolve("Update Ioc Resolve Dependency Strategy", args).execute();

            initialized = true;
        }
    }
}
