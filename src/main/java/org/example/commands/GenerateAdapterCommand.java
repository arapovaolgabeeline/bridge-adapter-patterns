package org.example.commands;

import java.lang.reflect.Method;
import org.example.interfaces.ICommand;

public class GenerateAdapterCommand implements ICommand {
    private final Class desiredInterface;
    private final Object object;

    public GenerateAdapterCommand(Class desiredInterface, Object object) {
        this.desiredInterface = desiredInterface;
        this.object = object;
    }

    @Override
    public void execute() {
        Method[] declaredMethods = desiredInterface.getDeclaredMethods();
        StringBuilder generatedAdapter = new StringBuilder();
        generatedAdapter.append("package\n");
        generatedAdapter.append("classname and ifc\n");

        for (Method declaredMethod : declaredMethods) {
            String name = declaredMethod.getName();

        }
    }
}
