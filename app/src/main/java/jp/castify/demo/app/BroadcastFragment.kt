package jp.castify.demo.app


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import jp.castify.api.*

import jp.castify.demo.R
import jp.castify.demo.app
import kotlinx.android.synthetic.main.fragment_broadcast.*

class BroadcastFragment : Fragment(), Broadcaster.Callback, SurfaceHolder.Callback {

  companion object {
    val mirror = FloatArray(16).also {
      Matrix.setIdentityM(it, 0)
      Matrix.scaleM(it, 0, -1f, 1f, 1f)
    }
  }

  private val broadcaster by lazy {
    requireContext()
      .app.castifyApp
      .newBroadcaster(lifecycle)
  }

  private object RequestCode {
    const val PERMISSIONS = 50
  }

  private val requiredPermissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
  )

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_broadcast, container, false)

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    // broadcaster
    broadcaster.previews.add(videoDisplay.holder)
    broadcaster.audioEncoderSetting = AudioEncoderSetting.AAC()
    broadcaster.videoEncoderSetting = VideoEncoderSetting.H264(
      bps = 3_000_000,
      profile = VideoEncoderSetting.H264.Profile.Main
    )
    broadcaster.callback = this

    frontOrBack.setOnCheckedChangeListener { _, _ -> resetMedia() }
    isAudioEnabled.setOnCheckedChangeListener { _, _ -> resetMedia() }
    isVideoEnabled.setOnCheckedChangeListener { _, _ -> resetMedia() }
    toggleBroadcast.setOnCheckedChangeListener { _, checked ->
      if (checked)
        broadcaster.start() else
        broadcaster.pause()
    }
    videoDisplay.holder.addCallback(this)
    requestRequiredPermissions()
  }

  override fun onDestroy() {
    super.onDestroy()
    broadcaster.close()
  }

  private fun queryAbsentPermissions() =
    requiredPermissions.filter {
      ActivityCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_DENIED
    }

  private fun requestRequiredPermissions() {
    val absentPermissions = queryAbsentPermissions()
    if (absentPermissions.isNotEmpty()) {
      ActivityCompat.requestPermissions(requireActivity(), absentPermissions.toTypedArray(), RequestCode.PERMISSIONS)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == RequestCode.PERMISSIONS && queryAbsentPermissions().isNotEmpty()) {
      Toast
        .makeText(requireContext(), "The requested permissions were not granted.", Toast.LENGTH_LONG)
        .show()
    }
  }

  override fun onStateChange(host: Broadcaster, state: Broadcaster.State, cause: Throwable?) {
    if (cause != null) {
      Log.e("castify", "closed on an error.", cause)
    }
    else {
      Log.i("castify", "state=$state")
    }
  }

  override fun onBroadcast(host: Broadcaster, broadcast: Broadcast) {
  }

  @Suppress("ConstantConditionIf")
  private fun resetMedia() {
    broadcaster.audio =
      AudioSource.Amp(
        base = AudioSource.Microphone(),
        gain = if (isAudioEnabled.isChecked)
          1.0 else
          0.0
      )

    if (isVideoEnabled.isChecked) {
      val useFrontCamera = frontOrBack.isChecked
      broadcaster.previews.transform = if (useFrontCamera) mirror else null
      broadcaster.video = createVideoSource(front = useFrontCamera)
    }
    else {
      broadcaster.video = null
    }
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    resetMedia()
  }

  override fun surfaceCreated(holder: SurfaceHolder) {}

  override fun surfaceDestroyed(holder: SurfaceHolder) {}

  private fun createVideoSource(front: Boolean = true): VideoSource? =
    VideoSource.Camera
      .findByFacing(
        if (front)
          VideoSource.Camera.Facing.FRONT else
          VideoSource.Camera.Facing.BACK
      )
      ?.copy(
        resolution = PointF(
          videoDisplay.width .toFloat(),
          videoDisplay.height.toFloat()
        ),
        rotation = requireActivity().windowManager.defaultDisplay.rotation
      )
}
