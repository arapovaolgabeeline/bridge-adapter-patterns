package org.example.ioc;

import org.example.interfaces.ICommand;
import org.example.interfaces.IDependencyResolverStrategyUpdater;

public class IocResolveDependencyStrategySetterCommand implements ICommand {
    private final IDependencyResolverStrategyUpdater strategyUpdater;

    public IocResolveDependencyStrategySetterCommand(IDependencyResolverStrategyUpdater strategyUpdater) {
        this.strategyUpdater = strategyUpdater;
    }

    @Override
    public void execute() {
        IoC.strategy = strategyUpdater.update(IoC.strategy);
    }

}
