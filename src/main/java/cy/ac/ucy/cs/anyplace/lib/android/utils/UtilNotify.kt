package cy.ac.ucy.cs.anyplace.lib.android.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

/**
 * Some sound and vibration notifications
 */
class UtilNotify(val ctx: Context) {

  private fun beep() {
    val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    // toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE) // NICE
    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP) // Beep sound..
  }

  private fun beepMsgReceived() {
    val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE)
  }

  private fun beepAlertReceived() {
    val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT)
  }

  private fun vibrate() {
    val buzzer = ctx.getSystemService<Vibrator>()
    val pattern = longArrayOf(0, 200, 100, 300)
    buzzer?.let {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        buzzer.vibrate(VibrationEffect.createWaveform(pattern, -1))
      } else {
        //deprecated in API 26
        buzzer.vibrate(pattern, -1)
      }
    }
  }


  private fun vibrateAlert() {
    val buzzer = ctx.getSystemService<Vibrator>()
    val pattern = longArrayOf(50, 500, 50, 500, 50, 500)
    buzzer?.let {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        buzzer.vibrate(VibrationEffect.createWaveform(pattern, -1))
      } else {
        //deprecated in API 26
        buzzer.vibrate(pattern, -1)
      }
    }
  }

  fun msgReceived() {
    if (DBG.notificationsDisabled) return

    beepMsgReceived()
    vibrate()
  }

  fun alertReceived() {
    if (DBG.notificationsDisabled) return
    beepAlertReceived()
    vibrateAlert()
    beepAlertReceived()
  }
}