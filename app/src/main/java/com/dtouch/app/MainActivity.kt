package com.dtouch.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasCamera()) startEngine() else Toast.makeText(this, "صلاحية الكاميرا مطلوبة", Toast.LENGTH_LONG).show()
    }
    private fun hasCamera() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(24),dp(48),dp(24),dp(24)); layoutDirection=View.LAYOUT_DIRECTION_RTL }
        root.addView(TextView(this).apply { text="DTOUCH"; textSize=34f; setTextColor(Color.parseColor("#22D3EE")) })
        root.addView(TextView(this).apply { text="تحكّم بالشاشة بإيماءات يدك عبر الكاميرا"; textSize=15f; setTextColor(Color.parseColor("#94A3B8")) })
        status=TextView(this).apply { textSize=16f; setTextColor(Color.WHITE); setPadding(0,dp(24),0,dp(16)) }; root.addView(status)
        addBtn(root,"1) تفعيل خدمة إمكانية الوصول"){startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}
        addBtn(root,"إذا كان الخيار رماديًا: فتح معلومات التطبيق"){startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}
        addBtn(root,"2) تشغيل DTOUCH"){
            val perms=mutableListOf(Manifest.permission.CAMERA); if(Build.VERSION.SDK_INT>=33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            if(hasCamera()) startEngine() else permLauncher.launch(perms.toTypedArray())
        }
        addBtn(root,"إيقاف"){stopService(Intent(this,CameraService::class.java)); status.postDelayed({updateStatus()},500)}
        root.addView(TextView(this).apply {
            text="الإيماءات:\n\n☝️ السبابة: تحريك المؤشر\n🤏 قرص الإبهام والسبابة ثم إفلات: نقر\n🤏 قرص مطوّل (حوالي ثانية): ضغط مطوّل\n🤏 قرص + تحريك اليد ثم إفلات: سحب / تمرير\n🖐️ كف مفتوحة + تحريكها: تمرير أعلى/أسفل/يمين/يسار\n✊ قبضة (ثانية): رجوع\n✌️ علامة V (ثانية): الرئيسية"
            textSize=15f; setTextColor(Color.parseColor("#CBD5E1")); setPadding(0,dp(24),0,0)
        })
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#0F172A")); addView(root) })
    }
    override fun onResume(){super.onResume();updateStatus()}
    private fun addBtn(parent:LinearLayout,label:String,onClick:()->Unit){val b=Button(this).apply{text=label;setOnClickListener{onClick()}};parent.addView(b,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})}
    private fun startEngine(){if(DTouchAccessibilityService.instance==null){Toast.makeText(this,"فعّل خدمة إمكانية الوصول أولاً",Toast.LENGTH_LONG).show();return};ContextCompat.startForegroundService(this,Intent(this,CameraService::class.java));status.postDelayed({updateStatus()},800)}
    private fun updateStatus(){val a11y=DTouchAccessibilityService.instance!=null;val cam=CameraService.running;status.text=(if(a11y)"✅ خدمة الوصول: مفعّلة" else "❌ خدمة الوصول: غير مفعّلة")+"\n"+(if(cam)"✅ الكاميرا: تعمل" else "⏸️ الكاميرا: متوقفة")}
}
