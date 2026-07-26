package com.whatsapp.scheduler.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.whatsapp.scheduler.data.model.FailureReason

data class SendTask(
    val messageId: Long,
    val contactName: String,
    val phoneNumber: String,
    val messageText: String,
    val attachmentPath: String?,
    val onResult: (success: Boolean, failureReason: String?) -> Unit
)

class WhatsAppAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var currentTask: SendTask? = null
    private var taskStep = Step.IDLE
    private var timeoutRunnable: Runnable? = null

    enum class Step {
        IDLE,
        WAITING_FOR_CHAT,
        ENTERING_TEXT,
        ATTACHING_FILE,
        SENDING,
        FINISHED
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceConnected = true
        Log.d(TAG, "WhatsApp Accessibility Service Connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || currentTask == null) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName != WHATSAPP_PKG && packageName != WHATSAPP_BUSINESS_PKG) return

        val rootNode = rootInActiveWindow ?: return
        processAutomationStep(rootNode)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceConnected = false
    }

    fun executeSendTask(task: SendTask) {
        this.currentTask = task
        this.taskStep = Step.WAITING_FOR_CHAT
        Log.d(TAG, "Starting automation task for messageId=${task.messageId}")

        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            if (taskStep != Step.FINISHED && currentTask == task) {
                Log.e(TAG, "Automation task timed out waiting for WhatsApp UI")
                finishTask(false, FailureReason.TIMEOUT_WAITING_FOR_UI)
            }
        }
        handler.postDelayed(timeoutRunnable!!, 15000)

        rootInActiveWindow?.let { processAutomationStep(it) }
    }

    private fun processAutomationStep(rootNode: AccessibilityNodeInfo) {
        val task = currentTask ?: return

        when (taskStep) {
            Step.WAITING_FOR_CHAT -> {
                val inputNode = findInputEditText(rootNode)
                if (inputNode != null) {
                    Log.d(TAG, "Found chat input field. Proceeding to ENTERING_TEXT step")
                    taskStep = Step.ENTERING_TEXT
                    handler.postDelayed({
                        rootInActiveWindow?.let { enterTextAndSend(it, inputNode, task) }
                    }, 500)
                }
            }

            Step.ENTERING_TEXT -> {
                val inputNode = findInputEditText(rootNode)
                if (inputNode != null) {
                    enterTextAndSend(rootNode, inputNode, task)
                }
            }

            Step.ATTACHING_FILE -> {
            }

            Step.SENDING -> {
                val sendButton = findSendButton(rootNode)
                if (sendButton != null) {
                    Log.d(TAG, "Clicking WhatsApp Send Button...")
                    val clicked = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) {
                        Log.d(TAG, "Send button clicked successfully")
                        taskStep = Step.FINISHED
                        handler.postDelayed({
                            finishTask(true, null)
                        }, 1000)
                    }
                }
            }

            Step.FINISHED, Step.IDLE -> {}
        }
    }

    private fun enterTextAndSend(
        rootNode: AccessibilityNodeInfo,
        inputNode: AccessibilityNodeInfo,
        task: SendTask
    ) {
        if (task.messageText.isNotEmpty()) {
            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    task.messageText
                )
            }
            inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "Set text in input field: '${task.messageText}'")
        }

        taskStep = Step.SENDING

        handler.postDelayed({
            rootInActiveWindow?.let { currentRoot ->
                val sendButton = findSendButton(currentRoot)
                if (sendButton != null) {
                    Log.d(TAG, "Clicking Send button now")
                    sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    taskStep = Step.FINISHED
                    handler.postDelayed({
                        finishTask(true, null)
                    }, 1000)
                } else {
                    Log.w(TAG, "Send button not immediately found, retrying...")
                }
            }
        }, 600)
    }

    private fun findInputEditText(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.EditText") {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findInputEditText(child)
            if (result != null) return result
        }
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (contentDesc.contains("send") || text == "send" || viewId.contains("send")) {
            return if (node.isClickable) node else findClickableParent(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSendButton(child)
            if (result != null) return result
        }
        return null
    }

    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) return parent
            parent = parent.parent
        }
        return null
    }

    private fun finishTask(success: Boolean, failureReason: String?) {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        val task = currentTask ?: return
        currentTask = null
        taskStep = Step.FINISHED
        Log.d(TAG, "Finished automation task. Success=$success, FailureReason=$failureReason")
        task.onResult(success, failureReason)
    }

    companion object {
        private const val TAG = "WhatsAppAccService"
        const val WHATSAPP_PKG = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PKG = "com.whatsapp.w4b"

        var instance: WhatsAppAccessibilityService? = null
            private set

        var isServiceConnected: Boolean = false
            private set
    }
}
