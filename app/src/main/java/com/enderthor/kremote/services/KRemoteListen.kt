package com.enderthor.kremote.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Intent
import android.graphics.Path
import android.os.CountDownTimer
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent

import com.enderthor.kremote.utils.KarooKey
import timber.log.Timber

class KRemoteListen: AccessibilityService() {
    class SpecialDeviceAdminReceiver: DeviceAdminReceiver()

    private var PressRepeatCount = 0
    var bServiceRunning: Boolean = false
    private var deviceAdminReceiver: ComponentName? = null
    private var timer = object : CountDownTimer(4000, 10000) {

        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            PressRepeatCount = 0
        }
    }

    private fun executegesture (startime: Int, duration: Int, path: Path,gestureBuilder: GestureDescription.Builder )
    {
        gestureBuilder.addStroke(StrokeDescription(path, startime.toLong(), duration.toLong()))
        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Timber.d("Swipe Gesture Completed")
                super.onCompleted(gestureDescription)
            }
        }, null)
    }

    private fun doubletap (path: Path,gestureBuilder: GestureDescription.Builder )
    {
        var time: Long= 0
        val stroke = StrokeDescription(path,0, ViewConfiguration.getTapTimeout().toLong())
        gestureBuilder.addStroke(stroke)

        time += stroke.duration + 40

        gestureBuilder.addStroke(StrokeDescription(stroke.path,time,stroke.duration,stroke.willContinue()))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Timber.d("Swipe Gesture Completed")
                super.onCompleted(gestureDescription)
            }
        }, null)
    }

    private fun swipescreen (karoobutton: KarooKey)
    {
        val displayMetrics = resources.displayMetrics
        val middleYValue = displayMetrics.heightPixels / 2
        val leftSideOfScreen = displayMetrics.widthPixels / 4
        val middleXValue = displayMetrics.widthPixels / 2
        val rightSizeOfScreen = leftSideOfScreen * 3
        val gestureBuilder = GestureDescription.Builder()

        val path = Path()
        val startime  = 100
        val duration = 50
        var repeat = false

        when (karoobutton) {
            KarooKey.BACK ->{
                Timber.d("BACK button pressed!")
                performGlobalAction(GLOBAL_ACTION_BACK)
                return
            }
            KarooKey.RIGHT -> {
                timer.cancel()
                PressRepeatCount += 1
                timer.start()

                if (PressRepeatCount < 2) {
                    path.moveTo(rightSizeOfScreen.toFloat(), middleYValue.toFloat())
                    path.lineTo(leftSideOfScreen.toFloat(), middleYValue.toFloat())
                } else {
                    Timber.d("RIGHT remote pressed two times in succession!")
                    path.moveTo(leftSideOfScreen.toFloat(), middleYValue.toFloat())
                    path.lineTo(rightSizeOfScreen.toFloat(), middleYValue.toFloat())
                }
            }
            KarooKey.VIRTUAL_SWITCH_TO_MAP_PAGE -> {
                path.moveTo(middleXValue.toFloat(), middleYValue.toFloat())
                repeat = true
            }
            else -> { return}
        }

        if (repeat) doubletap(path,gestureBuilder)
        else  executegesture(startime,duration,path,gestureBuilder)

    }
    override fun onCreate() {
        super.onCreate()
        deviceAdminReceiver = ComponentName(this, SpecialDeviceAdminReceiver::class.java)
        Timber.d("Accessibility Service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d( "Accessibility Service connected")
        bServiceRunning = true
        instance = this
    }
    companion object {
        @JvmField
        var bServiceRunning: Boolean = false
        @JvmStatic var instance: KRemoteListen? = null
    }
    fun doActionKarooScreen(karoobutton: KarooKey) {
        if (bGetServiceStatus()) {
            swipescreen(karoobutton)
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {
        bServiceRunning = false
        Timber.d( "Accessibility Service OnInterrupt")
    }
    override fun onUnbind(intent: Intent?): Boolean {
        bServiceRunning = false
        Timber.d( "Accessibility Service Unbind")
        return super.onUnbind(intent)
    }
    fun bGetServiceStatus(): Boolean {
        return bServiceRunning
    }
}