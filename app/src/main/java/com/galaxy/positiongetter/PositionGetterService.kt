package com.galaxy.positiongetter

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat

class PositionGetterService : AccessibilityService() {
    private val VIEW_TYPE_FILTER = "VIEW_TYPE_FILTER"
    private val VIEW_TYPE_INFO = "VIEW_TYPE_INFO"
    private val VIEW_TYPE_BUTTON = "VIEW_TYPE_BUTTON"

    // View for Getting Touch Position
    private lateinit var filterView: View
    private lateinit var filterWm: WindowManager
    private lateinit var filterLp: LayoutParams

    // Info View
    private lateinit var infoView: View
    private lateinit var infoWm: WindowManager
    private lateinit var infoLp: LayoutParams

    // Button View
    private lateinit var buttonView: View
    private lateinit var buttonWm: WindowManager
    private lateinit var buttonLp: LayoutParams



    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if(intent?.getBooleanExtra("Get Position", false)!!) {
            val pkgName = intent.getStringExtra("pkg") ?: ""
            startActivity(packageManager.getLaunchIntentForPackage(pkgName))

            initView()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun initView() {

        // View for Getting Touch Position
        filterView = LayoutInflater.from(this).inflate(R.layout.view_filter, null)
        filterWm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        filterLp = getLayoutParams(viewType = VIEW_TYPE_FILTER)
        filterWm.addView(filterView, filterLp)

        // Info View
        infoView = LayoutInflater.from(this).inflate(R.layout.view_info, null)
        infoWm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        infoLp = getLayoutParams(viewType = VIEW_TYPE_INFO)
        infoWm.addView(infoView, infoLp)

        // Button
        buttonView = LayoutInflater.from(this).inflate(R.layout.view_button, null)
        buttonWm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        buttonLp = getLayoutParams(viewType = VIEW_TYPE_BUTTON)
        buttonWm.addView(buttonView, buttonLp)

        setFilterTouchListener()
        setFilterToggleButtonClickListener()
        setDoneButtonClickListener()
    }

    private fun getLayoutParams(viewType: String) : LayoutParams = LayoutParams(

        if (viewType == VIEW_TYPE_FILTER) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT,
        if (viewType == VIEW_TYPE_FILTER) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT,
        LayoutParams.TYPE_APPLICATION_OVERLAY,
        if(viewType == VIEW_TYPE_INFO) LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE else LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {

        if(viewType == VIEW_TYPE_BUTTON) {
            gravity = Gravity.BOTTOM
            verticalMargin = 0.1f
        }
        else if(viewType == VIEW_TYPE_INFO) {
            gravity = Gravity.TOP
            verticalMargin = 0.1f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setFilterTouchListener() {

        val rect = Rect()
        val tvInfo = infoView.rootView.findViewById<TextView>(R.id.tvInfo)

        filterView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val touchInfo = findView(event.x, event.y, rootInActiveWindow)
                touchInfo.getBoundsInScreen(rect)

                val info = "addViewTouchListener -> \nX : ${event.x}\n" +
                        "Y : ${event.y}\n" +
                        "viewId : ${touchInfo.viewIdResourceName}\n" +
                        "text : ${touchInfo.text}\n" +
                        "className : ${touchInfo.className}\n" +
                        "position : [startX : ${rect.left}, endX : ${rect.right}, topY : ${rect.top}, bottomY : ${rect.bottom}"

                Log.d(">>>", "addViewTouchListener -> $info")

                tvInfo.text = info
            }
            false
        }
    }

    private fun setFilterToggleButtonClickListener() {

        var filterActivation = true
        val button = buttonView.rootView.findViewById<Button>(R.id.btnToggle)

        button.setOnClickListener {
            if(filterActivation) {
                filterActivation = false
                button.text = "Filter Off"
            }
            else {
                filterActivation = true
                button.text = "Filter On"
            }
            setFilterActivation(filterActivation)
        }
    }

    private fun setFilterActivation(filterActivation: Boolean) {

        if(filterActivation) {
            filterLp.flags =LayoutParams.FLAG_NOT_FOCUSABLE
            filterView.setBackgroundColor(ContextCompat.getColor(this, R.color.black_alpha5))
        }
        else {
            filterLp.flags =LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE
            filterView.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
        }

        filterWm.updateViewLayout(filterView, filterLp)
    }

    private fun setDoneButtonClickListener() {

        buttonView.rootView.findViewById<Button>(R.id.btnDone).setOnClickListener {
            filterWm.removeView(filterView)
            infoWm.removeView(infoView)
            buttonWm.removeView(buttonView)
        }
    }

    private fun findView(x:Float, y: Float, rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo {

        var viewInfo = rootNode
        for (i: Int in 0 until rootNode.childCount) {

            val childNode = rootNode.getChild(i)

            if (isContain(x, y, childNode)) {
                if (childNode.childCount == 0) {
                    return childNode
                } else {
                    viewInfo = findView(x, y, childNode)
                    if(viewInfo.childCount == 0) break
                }
            }
        }

        return viewInfo
    }

    private fun isContain(x:Float, y: Float, childNode: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        childNode.getBoundsInScreen(rect)

        if(rect.left < x && x < rect.right && rect.top < y && y  < rect.bottom) {
            return true
        }
        else {
            return false
        }
    }

}