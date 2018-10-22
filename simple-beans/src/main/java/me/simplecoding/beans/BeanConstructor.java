package me.simplecoding.beans;

@FunctionalInterface
public interface BeanConstructor<E> {

	E create() throws Exception;

}