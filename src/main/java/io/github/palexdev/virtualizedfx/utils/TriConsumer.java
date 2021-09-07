package io.github.palexdev.virtualizedfx.utils;

@FunctionalInterface
public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}
