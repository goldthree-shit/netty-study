package com.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadServer {

    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("boss");
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector boss = Selector.open();
        SelectionKey bossKey = ssc.register(boss, 0, null);
        bossKey.interestOps(SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(8080));
        // 创建固定数量的worker
        // Worker workder = new Worker("worker-0");
        Worker[] workders = new Worker[2];

        for (int i = 0; i < workders.length; i++) {
            workders[i] = new Worker("worker-" + i);
        }
        AtomicInteger index = new AtomicInteger(0);
        while (true) {
            boss.select();
            Iterator<SelectionKey> iterator = boss.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    // workder.register(sc); // 被boss线程所调用
                    workders[index.getAndIncrement() % workders.length].register(sc);
                }

            }
        }
    }

    static class Worker implements Runnable {
        private Thread thread;
        private Selector selector;
        private String name;
        private Boolean start = false;
        // 在线程之间传递消息，则可以利用队列作为通道。实现线程间的通信
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        public Worker(String name) {
            this.name = name;
        }

        public void register(SocketChannel sc) throws IOException {
            if (!start) {
                thread = new Thread(this, name);
                thread.start();
                this.selector = Selector.open();
                start = true;
            }
            // 向队列添加了任务，但这个任务没有立即执行
            // 保证sc.register的执行，在selector.select之后，确保已经被唤醒
            queue.add(() -> {
                try {
                    sc.register(this.selector, SelectionKey.OP_READ, null);
                } catch (ClosedChannelException e) {
                    throw new RuntimeException(e);
                }
            });
            // selector.wakeup(); // 唤醒selector，并不单单是唤醒阻塞状态，
            // 而是相当于发一张票，解除一次阻塞状态。即使之后陷入阻塞，如果存在这张牌，也会被唤醒
            // 关联
            // sc.register(this.selector, SelectionKey.OP_READ, null); // 仍然在boss线程
        }

        @Override
        public void run() {
            while (true) {
                try {
                    selector.select(); // 阻塞
                    Runnable task = queue.poll();
                    // 确保已经被唤醒了之后在执行注册
                    if (task != null) {
                        task.run(); // 执行sc.register(this.selector, SelectionKey.OP_READ, null);
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        // 如果是可读事件
                        if (key.isReadable()) {
                            ByteBuffer byteBuffer = ByteBuffer.allocate(16);
                            SocketChannel socketChannel = (SocketChannel)key.channel();
                            socketChannel.read(byteBuffer);
                            byteBuffer.flip();
                            while (byteBuffer.hasRemaining()) {
                                System.out.print((char) byteBuffer.get());
                            }
                        }

                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
