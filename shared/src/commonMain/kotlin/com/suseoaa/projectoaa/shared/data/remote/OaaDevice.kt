package com.suseoaa.projectoaa.shared.data.remote

/**
 * 登录、刷新、登出时上报的设备标识。后端按设备维护会话：同一设备类型重新登录会
 * 顶掉旧会话，登出也只注销当前设备。
 */
expect val oaaDevice: String
