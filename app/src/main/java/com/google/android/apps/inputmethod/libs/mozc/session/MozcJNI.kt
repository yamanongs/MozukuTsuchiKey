package com.google.android.apps.inputmethod.libs.mozc.session

object MozcJNI {
    init {
        System.loadLibrary("mozc")
    }

    @JvmStatic
    external fun initialize(): Boolean

    @JvmStatic
    external fun onPostLoad(userProfileDirectoryPath: String, dataFilePath: String): Boolean

    @JvmStatic
    external fun evalCommand(inBytes: ByteArray): ByteArray

    @JvmStatic
    external fun getDataVersion(): String
}
