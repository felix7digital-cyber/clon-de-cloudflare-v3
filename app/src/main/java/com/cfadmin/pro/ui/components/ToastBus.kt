package com.cfadmin.pro.ui.components

/**
 * Singleton global para disparar toasts desde cualquier parte del codigo.
 * El ToastHost en CfAdminApp consume ToastBus.state.
 */
object ToastBus {
    val state: ToastHostState = ToastHostState()

    fun show(text: String, type: ToastType = ToastType.Info) {
        state.show(text, type)
    }
}
