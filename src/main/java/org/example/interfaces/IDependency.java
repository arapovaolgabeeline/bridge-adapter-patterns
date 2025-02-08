package org.example.interfaces;

@FunctionalInterface
public interface IDependency {

    Object invoke(Object[] args);

}
