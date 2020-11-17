# 20201116 stream  
主要是client端被block方式的调用阻塞，影响了P99  

## 服务端
无论客户端以异步非阻塞还是同步阻塞形式调用，gRPC服务端的Response都是异步形式。  
对于异步的Request或者Response，都需要实现gRPC的io.grpc.stub.StreamObserver接口。  

io.grpc.stub.StreamObserver接口有三个方法：  
* onNext:表示接收/发送一个对象  
* onError:处理异常  
* onCompleted:表示Request或Response结束  

当Request发送到服务端端时，会异步调用requestObserver的onNext方法，直到结束时调用requestObserver的onCompleted方法；  
服务端调用responseObserver的onNext把Response返回给客户端，直到调用responseObserver的onCompleted方法通知客户端Response结束。  

## 客户端
gRPC的客户端有同步阻塞客户端（blockingStub)和异步非阻塞客户端(Stub）两种。  
同步客户端使用比较方便，但是性能较低，而且不支持stream形式的Request;  
异步客户端性能较高，支持stream形式的Request，但是如果想要以同步方式调用需要额外封装。  

## 异步转同步
由于gRPC的异步客户端性能较高且功能更完整，所以一般都会采用异步客户端。  
异步客户端接收到的Response也是以io.grpc.stub.StreamObserver形式。  
由于客户端的调用可能是在异步进程中但更可能是在同步进程中，所以就存在一个如何把gRPC异步Response转为同步Response的问题。  

一个比较常见的思路是写一个io.grpc.stub.StreamObserver实现，里面有一个内置变量保存异步Response的结果，再添加一个阻塞式的get()方法，直到Response结束才把所有结果返回。  
要知道Response是否结束，需要添加一个Boolean或者AtomicBoolean变量，初始化为false，调用responseObserver.onCompleted()方法时设置为true，这样就可以通过这个变量判断Response是否结束。  

阻塞get()方法最常见的思路是get()写一个while循环，直到变量值改为true才退出循环并返回结果。  
这种方式的优点是简单直接，任何语言都可以简单实现，缺点是由于使用循环可能CPU占用较高。  
而对于java这种多线程比较完善的语言，另一个比较好思路是Response结束前将线程挂起，当调用responseObserver.onCompleted()方法再唤醒线程。  
