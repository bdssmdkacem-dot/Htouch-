package com.dtouch.app

import android.content.Context
import android.content.SharedPreferences

data class Setting(
    val key: String,
    val label: String,
    val hint: String,
    val min: Float,
    val max: Float,
    val def: Float,
    val decimals: Int = 2
)

/** Shared runtime state between the camera service and the accessibility service. */
object EngineState {
    @Volatile var paused = false
    @Volatile var gesture: Gesture = Gesture.NONE
    @Volatile var handVisible = false
}

object Prefs {
    private var sp: SharedPreferences? = null

    fun init(ctx: Context) {
        if (sp == null) {
            sp = ctx.applicationContext.getSharedPreferences("dtouch_prefs", Context.MODE_PRIVATE)
        }
    }

    val SMOOTHING = Setting("smoothing", "سرعة تتبع المؤشر",
        "أعلى = أسرع لكن أكثر اهتزازًا", 0.10f, 0.80f, 0.35f)
    val MARGIN = Setting("margin", "حجم حركة اليد المطلوبة",
        "أعلى = تحرّك يدك أقل للوصول إلى أطراف الشاشة", 0.05f, 0.30f, 0.15f)
    val PINCH = Setting("pinch", "حساسية القرص",
        "أعلى = يُحتسب القرص من مسافة أبعد بين الإصبعين", 0.20f, 0.60f, 0.35f)
    val CONFIRM = Setting("confirm", "ثبات الإيماءة (عدد الإطارات)",
        "أعلى = أخطاء أقل لكن استجابة أبطأ", 1f, 6f, 3f, 0)
    val HOLD = Setting("hold", "مدة تثبيت القبضة / روك (مللي ثانية)",
        "المدة اللازمة لتنفيذ الرجوع والرئيسية", 300f, 1500f, 600f, 0)
    val PALM = Setting("palm", "حساسية تمرير الكف",
        "أقل = يكفي تحريك اليد قليلًا", 0.05f, 0.25f, 0.10f)
    val SCROLL_GAIN = Setting("scroll_gain", "سرعة التمرير بإصبعين",
        "أعلى = تمرير أسرع مع نفس حركة اليد", 0.5f, 4f, 1.5f, 1)
    val CURSOR = Setting("cursor", "حجم المؤشر", "", 15f, 60f, 30f, 0)
    val FPS = Setting("fps", "عدد الإطارات في الثانية",
        "أقل = بطارية أقل لكن تتبع أقل سلاسة", 8f, 30f, 15f, 0)

    val ALL = listOf(SMOOTHING, MARGIN, PINCH, CONFIRM, HOLD, PALM, SCROLL_GAIN, CURSOR, FPS)

    fun get(s: Setting): Float = sp?.getFloat(s.key, s.def) ?: s.def
    fun set(s: Setting, v: Float) { sp?.edit()?.putFloat(s.key, v)?.apply() }
    fun reset() { sp?.edit()?.clear()?.apply() }
}
