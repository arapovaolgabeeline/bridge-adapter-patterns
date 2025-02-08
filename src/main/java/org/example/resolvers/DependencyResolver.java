package org.example.resolvers;

import java.util.Map;
import org.example.interfaces.IDependency;

/**
 * Хранит список зависимостей. Сами зависимости крадет из текущего скоупа. Если в нем нет нужной, идет в перент.
 */
public class DependencyResolver implements IDependencyResolver {
    private final Map<String, IDependency> dependencies;

    public DependencyResolver(Object scope) {
        dependencies = (Map<String, IDependency>) scope;
    }

    @Override
    public Object resolve(String dependencyName, Object[] args) {
        Map<String, IDependency> dependencies = this.dependencies;

        while (true) {
            IDependency IDependency;
            if (dependencies.containsKey(dependencyName)) {
                IDependency = dependencies.get(dependencyName);
                return IDependency.invoke(args);
            } else {
                dependencies = (Map<String, IDependency>) dependencies.get("IoC.Scope.Parent").invoke(args);
            }
        }
    }
}
