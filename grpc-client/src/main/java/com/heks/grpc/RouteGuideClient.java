package com.heks.grpc;

import com.heks.grpc.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author heks
 * @description:
 * gRPC官方将自己分为三层组件：Stub、Channel和Transport。
 *
 * Stub层是最上层的代码，gRPC附带的插件可以从.proto文件直接生成Stub层代码，开发人员通过直接调用Stub层的代码调用RPC服务
 * Channel层是对Transport层功能的抽象，同时提供了很多有用的功能，比如服务发现和负载均衡。。
 * Transport层承担了将字节从网络中取出和放入数据的工作，有三种实现Netty、okHttp、inProgress。Transport层是最底层的代码。
 * @date 2020/11/16
 */
public class RouteGuideClient {

    private final ManagedChannel channel;
    private final RouteGuideGrpc.RouteGuideBlockingStub blockingStub;
    private final RouteGuideGrpc.RouteGuideStub asyncStub;

    public RouteGuideClient(String host, int port) {
        String target = "dns:///" + host + ":" + port;
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                //（1）class.forName()除了将类的.class文件加载到jvm中之外，还会对类进行解释，执行类中的static块。当然还可以指定是否执行静态块。
                //
                //（2）classLoader只干一件事情，就是将.class文件加载到jvm中，不会执行static中的内容,只有在newInstance才会去执行static块。
                .forTarget(target)
                .usePlaintext();
        channel = channelBuilder.build();
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public LocationNote getPoint(int lo, int lt, boolean blocking) throws Throwable {
        Point point = Point.newBuilder().setLongitude(lo).setLatitude(lt).build();
        if(blocking) {
            return blockingStub.getPoint(point);
        }else{
            CallableStreamObserver<LocationNote> responseObserver = new CallableStreamObserver<LocationNote>();
            asyncStub.getPoint(point, responseObserver);
            return responseObserver.get().get(0);
        }
    }

    public Iterator<Point> listPoints(int left, int top, int right, int bottom, boolean blocking) throws Throwable {
        Point hi = Point.newBuilder().setLongitude(left).setLatitude(top).build();
        Point lo = Point.newBuilder().setLongitude(right).setLatitude(bottom).build();
        Rectangle rec = Rectangle.newBuilder().setHi(hi).setLo(lo).build();
        if(blocking){
            return blockingStub.listPoints(rec);
        }else{
            CallableStreamObserver<Point> responseObserver = new CallableStreamObserver<Point>();
            asyncStub.listPoints(rec, responseObserver);
            return responseObserver.get().iterator();
        }
    }

    public RouteSummary recordRoute(Collection<Point> points) throws Throwable {
        CallableStreamObserver<RouteSummary> responseObserver = new CallableStreamObserver<RouteSummary>();
        StreamObserver<Point> requestObserver = asyncStub.recordRoute(responseObserver);
        points.stream().parallel().forEach(p -> requestObserver.onNext(p));
        requestObserver.onCompleted();
        return responseObserver.get().get(0);

    }

    public List<RouteSummary> getPointStream(Collection<Point> points) throws Throwable {
        CallableStreamObserver<RouteSummary> responseObserver = new CallableStreamObserver<RouteSummary>();
        StreamObserver<Point> requestObserver = asyncStub.getPointStream(responseObserver);
        points.stream().parallel().forEach(p -> requestObserver.onNext(p));
        requestObserver.onCompleted();
        return responseObserver.get();
    }
}
