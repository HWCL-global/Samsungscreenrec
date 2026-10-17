package com.example.screenrec

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val reqProjection = 1
    private val reqPerms = 2
    private lateinit var mic: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }
        root.addView(TextView(this).apply {
            text = "Screen Rec"; textSize = 28f; gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "\nTap Start, choose \"Entire screen\", then go to your lockscreen, " +
                "home screen or any app. Stop from the notification.\n"
            gravity = Gravity.CENTER
        })
        mic = CheckBox(this).apply { text = "Record microphone"; isChecked = false }
        root.addView(mic)
        root.addView(Button(this).apply {
            text = "Start recording"
            setOnClickListener { begin() }
        })
        root.addView(Button(this).apply {
            text = "Stop recording"
            setOnClickListener {
                startService(Intent(this@MainActivity, RecorderService::class.java)
                    .setAction(RecorderService.ACTION_STOP))
            }
        })
        setContentView(root)
    }

    private fun begin() {
        val need = mutableListOf(Manifest.permission.POST_NOTIFICATIONS)
        if (mic.isChecked) need.add(Manifest.permission.RECORD_AUDIO)
        val missing = need.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), reqPerms) else askProjection()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (code == reqPerms) askProjection()
    }

    private fun askProjection() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), reqProjection)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqProjection && resultCode == RESULT_OK && data != null) {
            val i = Intent(this, RecorderService::class.java)
                .setAction(RecorderService.ACTION_START)
                .putExtra("code", resultCode)
                .putExtra("data", data)
                .putExtra("mic", mic.isChecked)
            startForegroundService(i)
            moveTaskToBack(true)
        }
    }
}
