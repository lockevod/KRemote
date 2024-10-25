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
import com.enderthor.kremote.KRemoteKeys

import timber.log.Timber


class KRemoteListen: AccessibilityService() {

    //private var pressRepeatCount = 0
    internal var isRideActivityProcess: Boolean = false
    internal var timing: Long = 100
    internal var term: Long = 50

    companion object {
        @JvmStatic var instance: KRemoteListen? = null
    }
   /* private var timer = object : CountDownTimer(4000, 10000) {

        override fun onTick(millisUntilFinished: Long) {
        }
        override fun onFinish() {
            pressRepeatCount = 0
        }
    }
    */


    private fun executegesture (path: Path, gestureBuilder: GestureDescription.Builder, doublet: Boolean )
    {
        if (doublet)
        {
            val stroke = StrokeDescription(path,0, ViewConfiguration.getTapTimeout().toLong())
            gestureBuilder.addStroke(stroke)
            gestureBuilder.addStroke(StrokeDescription(stroke.path,stroke.duration + 40,stroke.duration,stroke.willContinue()))
        }
        else gestureBuilder.addStroke(StrokeDescription(path, timing, term))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Timber.d("Swipe Gesture Completed")
                super.onCompleted(gestureDescription)
            }
        }, null)
    }

    private fun swipescreen (karoobutton: KRemoteKeys)
    {
        val displayMetrics = resources.displayMetrics
        val middleYValue = displayMetrics.heightPixels / 2
        val leftSideOfScreen = displayMetrics.widthPixels / 4
        val middleXValue = displayMetrics.widthPixels / 2
        val rightSizeOfScreen = leftSideOfScreen * 3
        val gestureBuilder = GestureDescription.Builder()

        val path = Path()
        var repeat = false

        // determine action
        when (karoobutton)
        {
            KRemoteKeys.BACK ->{
                Timber.d("BACK button pressed!")
                performGlobalAction(GLOBAL_ACTION_BACK)
                return
            }
            KRemoteKeys.RIGHT -> {
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
            KRemoteKeys.MIDDLE -> {
                Timber.d("Map button pressed!")
                path.moveTo(middleXValue.toFloat(), middleYValue.toFloat())
                repeat = true
            }
            else -> {
                Timber.d("Action/Button not valid")
                return
            }
        }

        executegesture(path,gestureBuilder,repeat)

    }
    override fun onCreate() {
        Timber.d("Accessibility Service created")
        super.onCreate()
    }

    override fun onServiceConnected() {
        Timber.d( "Accessibility Service connected")
        instance = this
        super.onServiceConnected()
    }

    fun doActionKarooScreen(karoobutton: KRemoteKeys) {
        Timber.d("%s%s", "Check On Ride ", isRideActivityProcess)
        if (isRideActivityProcess) {
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
        Timber.d( "Accessibility Service OnInterrupt")
    }
    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d( "Accessibility Service Unbind")
        return super.onUnbind(intent)
    }
}