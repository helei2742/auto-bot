package cn.com.helei.bot.core.util;

import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class DiscardingBlockingQueue<T> {

    private final BlockingQueue<T> queue;

    /**
     *  队列的容量
     */
    @Getter
    private final int capacity;

    /**
     * 构造固定长度的阻塞队列
     *
     * @param capacity 队列的最大容量
     * @throws IllegalArgumentException 如果容量小于等于 0
     */
    public DiscardingBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    /**
     * 插入元素。如果队列满了，丢弃前面的元素再插入新元素。
     *
     * @param item 要插入的元素
     * @throws InterruptedException 如果线程被中断
     */
    public synchronized void put(T item) throws InterruptedException {
        if (queue.size() == capacity) {
            // 移除队列头部的元素
            queue.poll();
        }
        // 插入新元素
        queue.put(item);
    }

    /**
     * 从队列中取出元素（如果队列为空，阻塞直到有元素可用）
     *
     * @return 队列中的元素
     * @throws InterruptedException 如果线程被中断
     */
    public T take() throws InterruptedException {
        return queue.take();
    }

    /**
     * 获取当前队列的大小
     *
     * @return 当前队列大小
     */
    public int size() {
        return queue.size();
    }

    /**
     * 检查队列是否为空
     *
     * @return 如果队列为空返回 true，否则返回 false
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * 检查队列是否已满
     *
     * @return 如果队列已满返回 true，否则返回 false
     */
    public boolean isFull() {
        return queue.size() == capacity;
    }
}
