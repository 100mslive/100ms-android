package live.hms.roomkit.ui.notification

import live.hms.roomkit.R

class TranscriptionNotifications {

    fun transcriptionStarted() : HMSNotification {
        val message = "Closed Captioning enabled for everyone."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_cc_enable_success
        )
    }

    fun transcriptionStopped() : HMSNotification {
        val message = "Closed Captioning disabled for everyone."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_cc_disable_success
        )
    }
    fun startingTranscriptionsForEveryone() : HMSNotification{
        val message = "Enabling Closed Captioning for everyone\n" + "By continuing to be in this meeting, you consent to being recorded."

        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_loader
        )
    }

    fun stoppingTranscriptionsForEveryone() : HMSNotification {
        val message = "Disabling Closed Captioning for everyone."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_loader
        )
    }

    fun unableToStopTranscription() : HMSNotification {
        val message = "Failed to disable Closed Captions."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_error_triangle
        )
    }

    fun unableToStartTranscriptions() : HMSNotification {
        val message = "Failed to enable Closed Captions."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_error_triangle
        )
    }
}