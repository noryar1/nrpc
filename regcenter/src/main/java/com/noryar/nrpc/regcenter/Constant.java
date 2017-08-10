/*****************************************************************
 * Copyright (c) 2017 www.noryar.com Inc. All rights reserved.
 *****************************************************************/
package com.noryar.nrpc.regcenter;

/**
 * 类描述:常量.
 *
 * @author leon.
 */
public class Constant {
	public static final int ZK_SESSION_TIMEOUT = 5000;
    // 注册节点
    public static final String ZK_REGISTRY_PATH = "/registry";
    // 节点
    public static final String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";
}