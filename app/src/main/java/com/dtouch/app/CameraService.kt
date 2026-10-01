package com.dtouch.app
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.Executors
class CameraService:LifecycleService(){
 companion object{@Volatile var running=false;const val ACTION_STOP="com.dtouch.app.STOP";private const val CHANNEL="dtouch";private const val NOTIF_ID=1}
 private val executor=Executors.newSingleThreadExecutor();private var tracker:HandTracker?=null;private var provider:ProcessCameraProvider?=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{super.onStartCommand(intent,flags,startId);if(intent?.action==ACTION_STOP){stopSelf();return START_NOT_STICKY};if(running)return START_NOT_STICKY;startInForeground();startCamera();running=true;return START_NOT_STICKY}
 private fun startInForeground(){val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel(CHANNEL,"DTOUCH",NotificationManager.IMPORTANCE_LOW));val stop=PendingIntent.getService(this,0,Intent(this,CameraService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT);val n=NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("DTOUCH يعمل").setContentText("التحكم بالإيماءات نشط").setOngoing(true).addAction(0,"إيقاف",stop).build();ServiceCompat.startForeground(this,NOTIF_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)}
 private fun startCamera(){val future=ProcessCameraProvider.getInstance(this);future.addListener({try{val p=future.get();provider=p;val t=HandTracker(this){s->DTouchAccessibilityService.instance?.onHand(s)};tracker=t;val analysis=ImageAnalysis.Builder().setTargetResolution(Size(480,640)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();analysis.setAnalyzer(executor,t);p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_FRONT_CAMERA,analysis)}catch(e:Exception){stopSelf()}},ContextCompat.getMainExecutor(this))}
 override fun onDestroy(){running=false;try{provider?.unbindAll()}catch(e:Exception){};tracker?.close();executor.shutdown();DTouchAccessibilityService.instance?.onHand(null);super.onDestroy()}
}
