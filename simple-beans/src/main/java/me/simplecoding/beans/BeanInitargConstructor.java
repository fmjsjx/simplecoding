package me.simplecoding.beans;

@FunctionalInterface
public interface BeanInitargConstructor<T, A> {

	T create(A initarg) throws Exception;

}
