package com.dtouch.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs
import kotlin.math.hypot

class CursorView(ctx: Context) : View(ctx) {
    var cx=0f;var cy=0f;var shown=false;var pinching=false;var radius=30f
    private val fill=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.FILL}
    private val ring=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=5f;color=Color.WHITE}
    private val loc=IntArray(2)
    override fun onDraw(c:Canvas){if(!shown)return;getLocationOnScreen(loc);val x=cx-loc[0];val y=cy-loc[1];fill.color=Color.parseColor(if(pinching)"#AAFF3B30"else"#AA22D3EE");c.drawCircle(x,y,if(pinching)radius*.65f else radius,fill);c.drawCircle(x,y,radius,ring)}
}

class DTouchAccessibilityService:AccessibilityService(){
    companion object{@Volatile var instance:DTouchAccessibilityService?=null}
    private val main=Handler(Looper.getMainLooper());private lateinit var wm:WindowManager;private var view:CursorView?=null
    private var sw=1080f;private var sh=1920f;private var cx=540f;private var cy=960f
    private var last=Gesture.NONE;private var since=0L;private var fired=false;private var lastAction=0L;private var lastScroll=0L
    private var pinchT=0L;private var pinchX=0f;private var pinchY=0f;private var longFired=false;private var refX=0f;private var refY=0f
    override fun onServiceConnected(){super.onServiceConnected();instance=this;wm=getSystemService(Context.WINDOW_SERVICE)as WindowManager;val v=CursorView(this);val lp=WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);wm.addView(v,lp);view=v}
    override fun onAccessibilityEvent(event:AccessibilityEvent?){}
    override fun onInterrupt(){}
    override fun onUnbind(intent:android.content.Intent?):Boolean{instance=null;view?.let{try{wm.removeView(it)}catch(e:Exception){}};view=null;return super.onUnbind(intent)}
    fun onHand(s:HandState?){main.post{process(s)}}
    @Suppress("DEPRECATION") private fun updateScreen(){if(Build.VERSION.SDK_INT>=30){val b=wm.maximumWindowMetrics.bounds;sw=b.width().toFloat();sh=b.height().toFloat()}else{val m=DisplayMetrics();wm.defaultDisplay.getRealMetrics(m);sw=m.widthPixels.toFloat();sh=m.heightPixels.toFloat()}}
    private fun map(v:Float):Float{val m=Prefs.get(Prefs.MARGIN);return((v-m)/(1f-2f*m)).coerceIn(0f,1f)}
    @Suppress("DEPRECATION") private fun buzz(pattern:LongArray){try{val vib=getSystemService(Context.VIBRATOR_SERVICE)as Vibrator;vib.vibrate(VibrationEffect.createWaveform(pattern,-1))}catch(e:Exception){}}
    private fun togglePause(){EngineState.paused=!EngineState.paused;buzz(if(EngineState.paused)longArrayOf(0,150)else longArrayOf(0,50,80,50))}
    private fun process(s:HandState?){
        val v=view?:return;val now=SystemClock.uptimeMillis();updateScreen()
        if(s==null){last=Gesture.NONE;v.shown=false;v.invalidate();return}
        val g=s.gesture
        if(g!=last){if(last==Gesture.PINCH&&!EngineState.paused)finishPinch(s);if(g==Gesture.PINCH){pinchT=now;pinchX=s.x;pinchY=s.y;longFired=false};if(g==Gesture.OPEN_PALM||g==Gesture.PEACE){refX=s.x;refY=s.y};last=g;since=now;fired=false}
        if(g==Gesture.THREE&&!fired&&now-since>=1000){fired=true;togglePause()}
        if(EngineState.paused){v.shown=false;v.invalidate();return}
        val hold=Prefs.get(Prefs.HOLD).toLong()
        when(g){
            Gesture.POINT->{cx=map(s.x)*sw;cy=map(s.y)*sh}
            Gesture.PINCH->{val moved=hypot((s.x-pinchX)*sw,(s.y-pinchY)*sh);if(!longFired&&now-pinchT>=800&&moved<sw*.05f){longFired=true;buzz(longArrayOf(0,40));tap(cx,cy,700)}}
            Gesture.OPEN_PALM->palm(s,now)
            Gesture.PEACE->scroll(s,now)
            Gesture.FIST->if(!fired&&now-since>=hold){fired=true;buzz(longArrayOf(0,30));performGlobalAction(GLOBAL_ACTION_BACK)}
            Gesture.ROCK->if(!fired&&now-since>=hold){fired=true;buzz(longArrayOf(0,30));performGlobalAction(GLOBAL_ACTION_HOME)}
            else->{}
        }
        v.radius=Prefs.get(Prefs.CURSOR);v.cx=cx;v.cy=cy;v.shown=true;v.pinching=g==Gesture.PINCH;v.invalidate()
    }
    private fun finishPinch(s:HandState){if(longFired){longFired=false;return};val dx=(s.x-pinchX)*sw*1.6f;val dy=(s.y-pinchY)*sh*1.6f;if(hypot(dx,dy)<sw*.06f){buzz(longArrayOf(0,20));tap(cx,cy,50)}else swipe(cx,cy,(cx+dx).coerceIn(0f,sw),(cy+dy).coerceIn(0f,sh),300)}
    private fun palm(s:HandState,now:Long){if(now-lastAction<600)return;val th=Prefs.get(Prefs.PALM);val dx=(s.x-refX)*sw;val dy=(s.y-refY)*sh;val vertical=abs(dy)>=abs(dx);if(vertical&&abs(dy)>sh*th){val d=if(dy>0)1f else -1f;swipe(sw/2,sh/2-d*sh*.2f,sw/2,sh/2+d*sh*.2f,250)}else if(!vertical&&abs(dx)>sw*th){val d=if(dx>0)1f else -1f;swipe(sw/2-d*sw*.3f,sh/2,sw/2+d*sw*.3f,sh/2,250)}else return;refX=s.x;refY=s.y;lastAction=now}
    private fun scroll(s:HandState,now:Long){if(now-lastScroll<230)return;val gain=Prefs.get(Prefs.SCROLL_GAIN);val dx=(s.x-refX)*sw*gain;val dy=(s.y-refY)*sh*gain;if(abs(dy)>=abs(dx)){if(abs(dy)<sh*.03f)return;val d=dy.coerceIn(-sh*.4f,sh*.4f);swipe(sw/2,sh/2-d/2,sw/2,sh/2+d/2,200)}else{if(abs(dx)<sw*.03f)return;val d=dx.coerceIn(-sw*.4f,sw*.4f);swipe(sw/2-d/2,sh/2,sw/2+d/2,sh/2,200)};refX=s.x;refY=s.y;lastScroll=now}
    private fun tap(x:Float,y:Float,dur:Long){val p=Path().apply{moveTo(x,y)};stroke(p,dur)}
    private fun swipe(x1:Float,y1:Float,x2:Float,y2:Float,dur:Long){val p=Path().apply{moveTo(x1,y1);lineTo(x2,y2)};stroke(p,dur)}
    private fun stroke(p:Path,dur:Long){try{val g=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,dur)).build();dispatchGesture(g,null,null)}catch(e:Exception){}}
}
