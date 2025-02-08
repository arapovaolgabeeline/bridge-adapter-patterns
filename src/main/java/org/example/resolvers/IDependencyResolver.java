package org.example.resolvers;

@FunctionalInterface
public interface IDependencyResolver {

    <T> T resolve(String dependencyName, Object[] args);

}
