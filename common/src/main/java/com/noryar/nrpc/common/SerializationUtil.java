/*****************************************************************
 * Copyright (c) 2017 www.noryar.com Inc. All rights reserved.
 *****************************************************************/
package com.noryar.nrpc.common;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类描述:序列化工具类（基于protostuff实现）.
 *
 * @author leon.
 */
public class SerializationUtil {

    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<Class<?>, Schema<?>>();

    private static Objenesis objenesis = new ObjenesisStd(true);

    private SerializationUtil() {
    }

    /**
     * 功能描述:获取类的schema.
     *
     * @param cls class.
     * @return schema.
     */
    private static <T> Schema<T> getSchema(Class<T> cls) {
        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(cls);
            if (schema != null) {
                cachedSchema.put(cls, schema);
            }
        }
        return schema;
    }

    /**
     * 功能描述:序列化（对象 -> 字节数组）.
     *
     * @param obj obj.
     * @return byte arr.
     */
    public static <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);//序列化
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 功能描述:反序列化（字节数组 -> 对象）.
     *
     * @param data byte arr.
     * @param cls  class.
     * @return obj.
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        try {
            // 如果一个类没有空参构造方法时候，那么你直接调用newInstance方法试图得到一个实例对象的时候是会抛出异常的
            // 通过ObjenesisStd可以完美的避开这个问题
            T message = (T) objenesis.newInstance(cls); //实例化
            Schema<T> schema = getSchema(cls); //获取类的schema
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
