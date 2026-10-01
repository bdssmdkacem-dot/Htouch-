package com.dtouch.app

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.Locale
import kotlin.math.roundToInt

class SettingsActivity : ComponentActivity() {
    private lateinit var live: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() { live.text = liveText(); handler.postDelayed(this, 200) }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun gestureName(g: Gesture) = when (g) {
        Gesture.NONE -> "غير معروفة"; Gesture.POINT -> "☝️ سبابة"; Gesture.PINCH -> "🤏 قرص"
        Gesture.OPEN_PALM -> "🖐️ كف مفتوحة"; Gesture.FIST -> "✊ قبضة"; Gesture.PEACE -> "✌️ إصبعان"
        Gesture.THREE -> "3️⃣ ثلاثة أصابع"; Gesture.ROCK -> "🤘 روك"
    }
    private fun liveText(): String = when {
        !CameraService.running -> "شغّل DTOUCH من الشاشة الرئيسية لترى الإيماءة مباشرة أثناء الضبط"
        EngineState.paused -> "⏸️ متوقف مؤقتًا"
        !EngineState.handVisible -> "🔍 لا توجد يد أمام الكاميرا"
        else -> "الإيماءة الحالية: " + gestureName(EngineState.gesture)
    }
    private fun fmt(s: Setting, v: Float): String =
        if (s.decimals == 0) v.roundToInt().toString()
        else String.format(Locale.US, "%." + s.decimals + "f", v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(40), dp(24), dp(32))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root.addView(TextView(this).apply { text="إعدادات الحساسية"; textSize=26f; setTextColor(Color.parseColor("#22D3EE")) })
        live = TextView(this).apply {
            textSize=16f; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(dp(12),dp(12),dp(12),dp(12))
        }
        root.addView(live, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(12) })
        for (s in Prefs.ALL) addSetting(root,s)
        root.addView(Button(this).apply { text="إعادة الضبط الافتراضي"; setOnClickListener { Prefs.reset(); recreate() } },
            LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(24) })
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#0F172A")); addView(root) })
    }
    private fun addSetting(root: LinearLayout, s: Setting) {
        val cur=Prefs.get(s)
        val label=TextView(this).apply{text=s.label+": "+fmt(s,cur);textSize=15f;setTextColor(Color.WHITE);setPadding(0,dp(18),0,0)}
        root.addView(label)
        if(s.hint.isNotEmpty()) root.addView(TextView(this).apply{text=s.hint;textSize=12f;setTextColor(Color.parseColor("#94A3B8"))})
        val bar=SeekBar(this);bar.max=1000;bar.progress=(((cur-s.min)/(s.max-s.min))*1000f).roundToInt()
        bar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(sb:SeekBar?,p:Int,fromUser:Boolean){val v=s.min+(p/1000f)*(s.max-s.min);if(fromUser)Prefs.set(s,v);label.text=s.label+": "+fmt(s,v)}
            override fun onStartTrackingTouch(sb:SeekBar?){}
            override fun onStopTrackingTouch(sb:SeekBar?){}
        });root.addView(bar)
    }
    override fun onResume(){super.onResume();handler.post(poll)}
    override fun onPause(){handler.removeCallbacks(poll);super.onPause()}
}
