package live.hms.roomkit.ui.notification

import live.hms.roomkit.R

class TranscriptionNotifications {
    private val removeDelay = 3000L

    fun transcriptionStarted() : HMSNotification {
        val message = "Closed Captioning enabled for everyone."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_cc_enable_success,
            autoRemoveTypeAfterMillis = removeDelay,
            type = HMSNotificationType.RealTimeTranscription
        )
    }

    fun transcriptionStopped() : HMSNotification {
        val message = "Closed Captioning disabled for everyone."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_cc_disable_success,
            autoRemoveTypeAfterMillis = removeDelay,
            type = HMSNotificationType.RealTimeTranscription
        )
    }
    fun startingTranscriptionsForEveryone() : HMSNotification{
        val message = "Enabling Closed Captioning for everyone."

        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_loader,
            type = HMSNotificationType.RealTimeTranscription
        )
    }

    fun stoppingTranscriptionsForEveryone() : HMSNotification {
        val message = "Disabling Closed Captioning for everyone."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_loader,
            type = HMSNotificationType.RealTimeTranscription
        )
    }

    fun unableToStopTranscription() : HMSNotification {
        val message = "Failed to disable Closed Captions."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_error_triangle,
            autoRemoveTypeAfterMillis = removeDelay,
            type = HMSNotificationType.RealTimeTranscription
        )
    }

    fun unableToStartTranscriptions() : HMSNotification {
        val message = "Failed to enable Closed Captions."
        return HMSNotification(
            title = message,
            icon = R.drawable.transcription_error_triangle,
            autoRemoveTypeAfterMillis = removeDelay,
            type = HMSNotificationType.RealTimeTranscription
        )
    }
}