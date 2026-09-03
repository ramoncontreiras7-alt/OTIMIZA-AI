package com.otimiza.delivery.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.otimiza.delivery.domain.model.Platform

interface DetectedStopListener {
    fun onStopDetected(nativeStopId: String, platform: Platform, address: String?)
}

class DeliveryAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DeliveryAccessibility"
        private val NATIVE_ID_PATTERN = Regex("""(IFOOD|ML[A-Z0-9]{2}|LALA[0-9A-Z_-]+)""")
    }

    var stopListener: DetectedStopListener? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return
        parseForDeliveryData(root)
        root.recycle()
    }

    private fun parseForDeliveryData(node: AccessibilityNodeInfo) {
        val text = node.text?.toString()
        val viewId = node.viewIdResourceName
        if (!text.isNullOrBlank() && isTargetField(viewId)) {
            NATIVE_ID_PATTERN.find(text)?.value?.let { rawId ->
                val platform = runCatching { Platform.fromNativeId(rawId) }.getOrDefault(Platform.IFOOD)
                val address = resolveAddress(node)
                Log.d(TAG, "ID nativo preservado: $rawId | $platform")
                stopListener?.onStopDetected(rawId, platform, address)
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                parseForDeliveryData(child)
                child.recycle()
            }
        }
    }

    private fun isTargetField(viewId: String?): Boolean =
        viewId?.let { it.endsWith("order_id") || it.endsWith("delivery_address") || it.endsWith("stop_id") }
            ?: false

    private fun resolveAddress(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i) ?: continue
            sibling.text?.toString()?.let { t ->
                if (t.contains(",") && t.length > 10) return t
            }
        }
        return null
    }

    override fun onInterrupt() {
        Log.w(TAG, "Servico de acessibilidade interrompido.")
    }
}
