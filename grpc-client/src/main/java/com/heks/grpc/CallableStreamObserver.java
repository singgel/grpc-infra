package com.heks.grpc;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * @author heks
 * @description:
 * 阻塞get()方法最常见的思路是get()写一个while循环，直到变量值改为true才退出循环并返回结果。
 * 这种方式的优点是简单直接，任何语言都可以简单实现，缺点是由于使用循环可能CPU占用较高。
 * 而对于java这种多线程比较完善的语言，另一个比较好思路是Response结束前将线程挂起，当调用responseObserver.onCompleted()方法再唤醒线程。
 * @date 2020/11/16
 */
public class CallableStreamObserver<T> implements StreamObserver<T> {
    List<T> values = new ArrayList<T>();
    boolean isCompleted = false;
    Throwable t = null;

    @Override
    public void onNext(T value) {
        this.values.add(value);
    }

    @Override
    public void onError(Throwable t) {
        this.isCompleted = true;
        notifyAll();
    }

    @Override
    public synchronized void onCompleted() {
        this.isCompleted = true;
        notifyAll();
    }

    public List<T> get() throws Throwable {
        if (!this.isCompleted) {
            synchronized (this) {
                this.wait(60 * 1000);
            }
        }
        if (null != t) {
            throw this.t;
        } else {
            return this.values;
        }
    }
}
