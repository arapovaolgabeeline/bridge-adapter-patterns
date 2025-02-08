package org.example.commands;

import org.example.interfaces.ICommand;

public class SetCurrentScopeCommand implements ICommand {
    private final Object scope;

    public SetCurrentScopeCommand(Object scope) {
        this.scope = scope;
    }

    @Override
    public void execute() {
        InitCommand.currentScope.set(scope);
    }
}
