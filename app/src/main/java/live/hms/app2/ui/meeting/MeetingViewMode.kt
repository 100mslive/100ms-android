package live.hms.app2.ui.meeting

import live.hms.app2.R

enum class MeetingViewMode {
  GRID,
  PINNED,
  ACTIVE_SPEAKER,
  AUDIO_ONLY,
  HLS;

  val titleResId: Int
    get() = when (this) {
      GRID -> R.string.grid_view
      PINNED -> R.string.hero_view
      ACTIVE_SPEAKER -> R.string.active_speaker_view
      AUDIO_ONLY -> R.string.audio_only_view
      HLS -> R.string.hls_view
    }
}