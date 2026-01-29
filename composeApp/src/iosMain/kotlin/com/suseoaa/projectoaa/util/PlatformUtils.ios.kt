package com.suseoaa.projectoaa.util

import androidx.compose.runtime.Composable

@Composable
actual fun showToast(message: String) {
    // iOS implementation - could use native alerts or custom composable
    println("Toast: $message")
}

@Composable
actual fun pickImageForAvatar(onImagePicked: (ByteArray?) -> Unit) {
    // iOS implementation - would use PHPickerViewController
    println("Pick image not implemented for iOS")
    onImagePicked(null)
}
