package com.yxy.cube

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.view.ViewGroup

import com.zybang.yike.mvp.playback.test.CubeView




class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<TextView>(R.id.textview).setOnClickListener {
            val cubeView = CubeView(this)
            val contentViewGroup: ViewGroup = findViewById(android.R.id.content)
            val rootView: View = contentViewGroup.getChildAt(0)
            contentViewGroup.removeView(rootView)
            cubeView.addView(rootView)
            contentViewGroup.addView(cubeView)
            cubeView.setCubeEnabled(true)
            cubeView.showViewId(true)
        }
    }
}