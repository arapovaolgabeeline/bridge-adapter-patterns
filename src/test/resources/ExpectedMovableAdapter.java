package org.example.generated.adapters;

import java.util.Map;
import java.util.Vector;
import org.example.interfaces.ICommand;
import org.example.ioc.IoC;

public class MovableAdapter {
    private final Map<String, String> obj;

    public MovableAdapter(Map<String, String> obj) {
        this.obj = obj;
    }

    public Vector getPosition() {
        return IoC.resolve("Spaceship.Operations.IMovable:position.get", new Object[] { obj });
    }

    public Vector getVelocity() {
        return IoC.resolve("Spaceship.Operations.IMovable:velocity.get", new Object[] { obj });
    }

    public void setPosition(Vector newValue) {
        ((ICommand) IoC.resolve("Spaceship.Operations.IMovable:position.set", new Object[]{obj, newValue})).execute();
    }
}
