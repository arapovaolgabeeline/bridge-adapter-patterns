package org.example.commands;

import org.example.interfaces.ICommand;
import org.example.interfaces.IDependency;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class UnregisterDependencyCommand implements ICommand {
    private final String dependencyName;
    private final Map<String, IDependency> currentScope;

    public UnregisterDependencyCommand(String dependencyName, Map<String, IDependency> currentScope) {
        this.dependencyName = dependencyName;
        this.currentScope = currentScope;
    }

    @Override
    public void execute() {
        currentScope.remove(dependencyName);
    }
}
