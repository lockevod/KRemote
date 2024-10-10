package com.enderthor.kremote.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.ComponentName
import android.content.Intent
import android.graphics.Path
//import android.os.CountDownTimer
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import com.enderthor.kremote.RemoteKey

import timber.log.Timber

class KRemoteListen: AccessibilityService() {

    //private var pressRepeatCount = 0
    var bServiceRunning: Boolean = false
    var isRideActivityProcess: Boolean = false
   /* private var timer = object : CountDownTimer(4000, 10000) {

        override fun onTick(millisUntilFinished: Long) {
        }
        override fun onFinish() {
            pressRepeatCount = 0
        }
    }
    */

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
                Timber.d("DoubleTap Gesture Completed")
                super.onCompleted(gestureDescription)
            }
        }, null)
    }

    private fun swipescreen (karoobutton: RemoteKey)
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

        // determine action
        when (karoobutton) {
            RemoteKey.BACK ->{
                Timber.d("BACK button pressed!")
                performGlobalAction(GLOBAL_ACTION_BACK)
                return
            }
            RemoteKey.RIGHT -> {
                Timber.d("RIGHT remote pressed one time only!")
                path.moveTo(rightSizeOfScreen.toFloat(), middleYValue.toFloat())
                path.lineTo(leftSideOfScreen.toFloat(), middleYValue.toFloat())
                /*
                timer.cancel()
                pressRepeatCount += 1
                timer.start()

                if (pressRepeatCount < 2) {
                    Timber.d("RIGHT remote pressed one time only!")
                    path.moveTo(rightSizeOfScreen.toFloat(), middleYValue.toFloat())
                    path.lineTo(leftSideOfScreen.toFloat(), middleYValue.toFloat())
                }
               else {
                    Timber.d("RIGHT remote pressed two times in succession!")
                    path.moveTo(leftSideOfScreen.toFloat(), middleYValue.toFloat())
                    path.lineTo(rightSizeOfScreen.toFloat(), middleYValue.toFloat())
                }*/
            }
            RemoteKey.MIDDLE -> {
                Timber.d("Map button pressed!")
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
    fun doActionKarooScreen(karoobutton: RemoteKey) {
        Timber.d("%s%s", "Check On Ride ", isRideActivityProcess)
        if (bGetServiceStatus() && isRideActivityProcess) {
            swipescreen(karoobutton)
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.packageName != null && event.className != null) {
                val componentName = ComponentName(
                    event.packageName.toString(),
                    event.className.toString()
                )
                isRideActivityProcess =  componentName.flattenToShortString().contains("io.hammerhead.rideapp") || componentName.flattenToShortString().contains("com.android.systemui")
                Timber.d("%s%s", "Ride activity status: ", isRideActivityProcess)
                Timber.d("%s%s", "Activity name: ", componentName.flattenToShortString())
            }
        }
    }

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