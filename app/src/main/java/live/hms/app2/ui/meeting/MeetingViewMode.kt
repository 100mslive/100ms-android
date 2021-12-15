package live.hms.app2.ui.meeting

import live.hms.app2.R

sealed class MeetingViewMode {
  object GRID : MeetingViewMode() {
    override val titleResId: Int = R.string.grid_view
  }

  object PINNED : MeetingViewMode() {
    override val titleResId: Int = R.string.hero_view
  }

  object ACTIVE_SPEAKER : MeetingViewMode() {
    override val titleResId: Int = R.string.active_speaker_view
  }

  object AUDIO_ONLY : MeetingViewMode() {
    override val titleResId: Int = R.string.audio_only_view
  }

  data class HLS(val url : String) : MeetingViewMode() {
    override val titleResId: Int = R.string.hls_view
  }

  abstract val titleResId: Int
}