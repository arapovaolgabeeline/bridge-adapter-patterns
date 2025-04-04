package org.example.interfaces;

import java.util.Vector;

public interface IMovable {
    Vector getPosition();
    void setPosition(Vector newValue);
    Vector getVelocity();

    /**
     * Deletes IMovable from IoC
     * */
    void finish();
}
