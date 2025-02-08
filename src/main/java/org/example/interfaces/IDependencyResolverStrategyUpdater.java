package org.example.interfaces;

@FunctionalInterface
public interface IDependencyResolverStrategyUpdater {

    IDependencyResolverStrategy update(IDependencyResolverStrategy newIDependencyResolverStrategy);

}
