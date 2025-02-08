package org.example.interfaces;

@FunctionalInterface
public interface IDependencyResolverStrategy {

    <T> T resolve(String dependency, Object[] args);

}
