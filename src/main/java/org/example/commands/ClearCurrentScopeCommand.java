package org.example.commands;

import org.example.interfaces.ICommand;

public class ClearCurrentScopeCommand implements ICommand {

    @Override
    public void execute() {
        InitCommand.currentScope.remove();
    }

}
