package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.prebuilt_themes.Variables
import live.hms.video.sdk.models.TranscriptionState
import live.hms.video.sdk.models.TranscriptionsMode

class ClosedCaptionsForEveryone : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ClosedCaptionsForEveryoneBottomFragment"
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    private fun getCurrentScreen() : ScreenInfo {
        val transcriptionStarted = meetingViewModel.hmsSDK.getRoom()?.transcriptions?.find { it.mode == TranscriptionsMode.CAPTION && it.state == TranscriptionState.STARTED } != null
        return getScreen(transcriptionStarted)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when viewLifecycleOwner is destroyed
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                EnableCaptionsDisplay(
                    onEnableForEveryoneClicked = {
                        meetingViewModel.toggleCaptionsForEveryone(true)
                        dismissAllowingStateLoss()
                    },
                    hideForMeClicked = {
                        meetingViewModel.toggleCaptions()
                        dismissAllowingStateLoss()
                    },
                    disableForEveryoneClicked = {
                        meetingViewModel.toggleCaptionsForEveryone(false)
                        dismissAllowingStateLoss()
                    },
                    close = {
                        dismissAllowingStateLoss() },
                    screen = getCurrentScreen()
                )
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    private fun getScreen(isEnabled: Boolean) : ScreenInfo =
        if(isEnabled) {
            ScreenInfo(
                title = "Closed Captions (CC) ",
                description = "This will disable Closed Captions for everyone in this room. You can enable it again.",
                isEnable = false
            )
        } else {
            ScreenInfo(
                title = "Enable Closed Captions (CC) for this session?",
                description = "This will enable Closed Captions for everyone in this room. You can disable it later.",
                isEnable = true
            )
        }
}

@Composable
fun EnableCaptionsDisplay(onEnableForEveryoneClicked : () -> Unit,
                          hideForMeClicked : () -> Unit,
                          disableForEveryoneClicked : () -> Unit,
                          close : () -> Unit,
                          screen : ScreenInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = Variables.SurfaceDim,
                shape = RoundedCornerShape(topStart = Variables.Spacing2, topEnd = Variables.Spacing2)
            )
            .padding(
                start = Variables.Spacing3,
                end = Variables.Spacing3,
                top = Variables.Spacing3,
                bottom = Variables.Spacing4
            )
        ,
        verticalArrangement = Arrangement.spacedBy(Variables.Spacing2, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.Top,
        ) {

            Text(
                modifier = Modifier.weight(1f),
                text = screen.title, style = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_bold)),
                    fontWeight = FontWeight(600),
                    color = Variables.OnSecondaryHigh,
                    letterSpacing = 0.15.sp,
                )
            )
            Image(
                modifier = Modifier
                    .padding(1.dp)
                    .size(24.dp)
                    .clickable { close() },
                painter = painterResource(id = R.drawable.outline_cross),
                contentDescription = "Close",
                contentScale = ContentScale.None
            )
        }
        if(screen.isEnable) {
            EnableButton("Enable for Everyone", Variables.PrimaryDefault, onEnableForEveryoneClicked)
        } else {
            EnableButton(
                text = "Hide For Me",
                backgroundColor = Variables.SecondaryDefault,
                onEnableClicked = hideForMeClicked)
            EnableButton(
                text = "Disable For Everyone",
                backgroundColor = Variables.AlertErrorDefault,
                onEnableClicked = disableForEveryoneClicked)
        }
        DescriptionText(text = screen.description)
    }
}

@Composable
fun EnableButton(
    text: String,
    backgroundColor: Color,
    onEnableClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = backgroundColor, shape = RoundedCornerShape(size = 8.dp))
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.clickable { onEnableClicked.invoke() },
            text = text,

            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_bold)),
                fontWeight = FontWeight(600),
                color = Variables.OnPrimaryHigh,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
            )
        )
    }

}
@Composable
fun DescriptionText(text : String) {
    Text(
        text = text,

        // Desktop/Body 2-Regular-14px
        style = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_regular)),
            fontWeight = FontWeight(400),
            color = Variables.OnSurfaceMedium,
            letterSpacing = 0.25.sp,
        )
    )
}

data class ScreenInfo(
    val title : String,
    val description : String,
    val isEnable : Boolean
)
