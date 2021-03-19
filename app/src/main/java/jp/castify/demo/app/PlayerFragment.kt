package jp.castify.demo.app


import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isGone
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import jp.castify.api.*

import jp.castify.demo.R
import jp.castify.demo.app
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.coroutines.launch
import java.lang.Exception

class PlayerFragment : Fragment(), Player.Callback, SeekBar.OnSeekBarChangeListener {

  private val args by navArgs<PlayerFragmentArgs>()

  private val source by lazy {
    requireContext()
      .app.castifyApp
      .newSource(args.broadcastId)
  }

  private val player by lazy {
    requireContext()
      .app.castifyApp
      .newPlayer(Player.Config(lowLatency = false))
  }

  data class TimeValue(
    val duration: Double = 0.0,
    val playHead: Double = 0.0
  )

  private val timeValue by lazy {
    MutableLiveData(TimeValue())
  }

  private val isSeekBarGrabbed by lazy {
    MutableLiveData(false)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_player, container, false)

  @SuppressLint("SetTextI18n")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    audioOnOffButton.isChecked = true
    videoOnOffButton.isChecked = true

    // player
    player.previews.add(playerDisplay.holder)
    player.previews.resizeMode = ResizeMode.Contain
    player.previews.align = PointF(0.5f, 0.5f)
    player.callback = this

    audioOnOffButton.run { setOnClickListener { player.isAudioEnabled = isChecked } }
    videoOnOffButton.run { setOnClickListener { player.isVideoEnabled = isChecked } }
    pauseToggle.run { setOnClickListener { player.paused = isChecked } }

    playbackPosition.setOnSeekBarChangeListener(this)

    timeValue.observe(viewLifecycleOwner) {
      timeIndicator.text = it.playHead.toDurationString() + "/" + it.duration.toDurationString()
      playbackPosition.apply {
        val isArchive = it.duration > 0.0
        if (isArchive) {
          max      = it.duration.toInt()
          progress = it.playHead.toInt()
        }
        else {
          max      = 1
          progress = 1
        }
        isEnabled = isArchive
      }
    }

    playbackPosition.isEnabled = true
  }

  override fun onResume() {
    super.onResume()

    lifecycleScope.launch {
      try {
        val info = source.load()
        if (info.stoppedAt == null) { // live
          player.seek()
        }
        else {
          val duration = (info.stoppedAt!! - info.startedAt) / 1000
          timeValue.value = TimeValue(duration = duration.toDouble())
          player.seek(0.0)
        }
      }
      catch (ex: Exception) {
        Toast
          .makeText(context, "Failed to load info of the broadcast.", Toast.LENGTH_SHORT)
          .show()
      }
    }

    player.source = source
    player.isAudioEnabled = audioOnOffButton.isChecked
    player.isVideoEnabled = videoOnOffButton.isChecked
    player.paused = pauseToggle.isChecked
  }

  override fun onDestroy() {
    super.onDestroy()
    player.close()
  }

  override fun onStateChange(host: Player, state: Player.State, cause: Throwable?) {
    requireActivity().runOnUiThread {
      loadingIndicator?.isGone = state != Player.State.WIP
    }
    cause
      ?.let { it as? CastifyException }
      ?.also {
        Log.d("castify", "error: webhookReply=" + it.webhookReply)
      }
  }

  override fun onTimer(host: Player, time: Double) {
    if (isSeekBarGrabbed.value == true) {
      return
    }
    timeValue.apply {
      postValue(value?.copy(playHead = time))
    }
  }

  override fun onError(cause: Throwable) {
  }

  override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
    if (fromUser) {
      timeValue.apply {
        value = value?.copy(playHead = progress.toDouble())
      }
    }
  }

  override fun onStartTrackingTouch(seekBar: SeekBar) {
    isSeekBarGrabbed.value = true
  }

  override fun onStopTrackingTouch(seekBar: SeekBar) {
    isSeekBarGrabbed.value = false
    timeValue.value
      ?.playHead
      ?.also(player::seek)
  }

  private fun Double.toDurationString(): String {
    val s = this.toInt()
    return "%02d:%02d:%02d".format(
      s / 60 / 60,
      s / 60 % 60,
      s % 60
    )
  }
}
