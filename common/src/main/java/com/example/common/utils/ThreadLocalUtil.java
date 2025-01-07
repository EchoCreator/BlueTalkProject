package com.example.common.utils;

public class ThreadLocalUtil {
    private static final ThreadLocal THREAD_LOCAL=new ThreadLocal<>();

    public static <T> T get(){
        return (T) THREAD_LOCAL.get();
    }

    public static <T> void set(T value){
        THREAD_LOCAL.set(value);
    }

    public static void remove(){
        THREAD_LOCAL.remove();
    }
}
