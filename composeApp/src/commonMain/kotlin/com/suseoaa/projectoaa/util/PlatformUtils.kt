package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

/**
 * 显示Toast消息
 */
@Composable
expect fun showToast(message: String)

/**
 * 选择图片并返回字节数组
 */
@Composable
expect fun pickImageForAvatar(onImagePicked: (ByteArray?) -> Unit)
