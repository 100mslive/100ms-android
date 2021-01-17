package live.hms.android100ms.ui.meeting

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MeetingOptionsMetadata(
    val isAudioEnabled: Boolean,
    val isVideoEnabled: Boolean,
) : Parcelable