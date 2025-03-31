package org.example.interfaces;

import java.util.Vector;

public interface IMovable {
    Vector getPosition();
    void setPosition(Vector newValue);
    Vector getVelocity();

    void finish();
}
