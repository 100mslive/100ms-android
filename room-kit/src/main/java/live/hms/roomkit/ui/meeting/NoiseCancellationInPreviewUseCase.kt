package live.hms.roomkit.ui.meeting

class NoiseCancellationInPreviewUseCase(
    private val krispEnabledInSettings : Boolean,
    private val setNoiseCancellation : (enable : Boolean) -> Unit) {
    private var state : NcInPreview = NcInPreview.UNSET
    enum class NcInPreview {
        UNSET,
        ON,
        OFF
    }
    fun getNcState() = state
    fun isEnabled() : Boolean =
        // Either it's just on
        state == NcInPreview.ON ||
                // Or its unset but krisp is enabled in settings.
            state == NcInPreview.UNSET && krispEnabledInSettings

    fun afterJoin() {
        when(state) {
            NcInPreview.ON -> {
                setNoiseCancellation(true)
            }
            NcInPreview.OFF -> {
                setNoiseCancellation(false)
            }
            NcInPreview.UNSET -> {} // nothing to do
        }
    }

    fun clickNcInPreview() {
        state = when(state) {
            NcInPreview.ON -> NcInPreview.OFF
            NcInPreview.UNSET,
            NcInPreview.OFF
            -> NcInPreview.ON
        }
    }

    fun setNcStateForPreview(value : Boolean?) {
        state = when(value) {
            true -> NcInPreview.ON
            false -> NcInPreview.ON
            null -> NcInPreview.UNSET
        }

    }
}