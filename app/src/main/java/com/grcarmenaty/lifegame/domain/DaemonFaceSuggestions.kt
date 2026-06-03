package com.grcarmenaty.lifegame.domain

import androidx.annotation.DrawableRes
import com.grcarmenaty.lifegame.R

/**
 * Ethereal-abstract face icon per [VoicePreset]. Themes vary the
 * daemon's name but not its portrait, so the face is a pure function
 * of voice preset — each preset maps to a single VectorDrawable in
 * `res/drawable/face_<preset>.xml`.
 *
 * Stroke-only line-art at 48dp viewport; Compose tints them via
 * [androidx.compose.material3.Icon]. Surfacing the face elsewhere is
 * a one-liner: call [faceFor] wherever a daemon is rendered.
 */
object DaemonFaceSuggestions {

    @DrawableRes
    fun faceFor(preset: VoicePreset): Int = when (preset) {
        VoicePreset.DRILL_SERGEANT -> R.drawable.face_drill_sergeant
        VoicePreset.COACH -> R.drawable.face_coach
        VoicePreset.HERMIT -> R.drawable.face_hermit
        VoicePreset.POET -> R.drawable.face_poet
        VoicePreset.THERAPIST -> R.drawable.face_therapist
        VoicePreset.GENTLE_MENTOR -> R.drawable.face_gentle_mentor
        VoicePreset.ORACLE -> R.drawable.face_oracle
        VoicePreset.CHEERLEADER -> R.drawable.face_cheerleader
        VoicePreset.STOIC -> R.drawable.face_stoic
        VoicePreset.TRICKSTER -> R.drawable.face_trickster
    }
}
