package live.hms.vb_prebuilt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import live.hms.prebuilt_themes.Variables
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.prebuilt_themes.Variables.Companion.Spacing2
import live.hms.roomkit.R

class VirtualBackgroundBottomSheet : BottomSheetDialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
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
                VirtualBackgroundOptions(close = {
                    dismissAllowingStateLoss() },
                    removeEffects = {},
                    blur = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    VirtualBackgroundOptions(close = {},
        removeEffects = {},
        blur = {})
}

@Composable
fun VirtualBackgroundOptions(
    videoView : @Composable () -> Unit = { Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RectangleShape)
            .background(Color.Gray)
    )},
    close : () -> Unit,
    removeEffects :() -> Unit,
    blur : () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = Variables.SurfaceDim,
                shape = RoundedCornerShape(
                    topStart = Variables.Spacing2,
                    topEnd = Variables.Spacing2
                )
            )
            .padding(
                start = Variables.Spacing3,
                end = Variables.Spacing3,
                top = Variables.Spacing3,
                bottom = Variables.Spacing4
            )
        ,
        verticalArrangement = Arrangement.spacedBy(Variables.Spacing2, Alignment.Top),
//        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BottomSheetHeader(close)
        // the video item somehow
        Box(modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center) {
            videoView()
        }
        Text(
            text = "Effects", style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontWeight = FontWeight(600),
                color = Variables.OnSecondaryHigh,
                letterSpacing = 0.15.sp,
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing2)) {
            VbOptionButton(drawable = live.hms.vb_prebuilt.R.drawable.vb_cross_circle,
                "No effect",removeEffects)
            VbOptionButton(drawable = live.hms.vb_prebuilt.R.drawable.vb_blur_background,
                "Blur",blur)
        }
        var sliderPosition by remember { mutableFloatStateOf(30f) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = live.hms.vb_prebuilt.R.drawable.vb_slider_blur_people),
                contentDescription = "effect slider")
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                valueRange = 0f..100f,
                steps = 1,
                colors = SliderDefaults.colors(
                    thumbColor = Variables.PrimaryDefault,
                    activeTrackColor = Variables.PrimaryDefault,
                    inactiveTrackColor = Variables.SecondaryDefault,
                )
            )
        }
    }
}

@Composable
fun BottomSheetHeader(close : () -> Unit,) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.Top,
    ) {

        Text(
            modifier = Modifier.weight(1f),
            text = "Virtual Background", style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
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
}

@Composable
fun VbOptionButton(@DrawableRes drawable :  Int,
                   description : String,
                   onClick : () -> Unit) {
    Column(
        Modifier
            .width(103.33334.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = Variables.SurfaceBright)
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(id = drawable), contentDescription = "s")
        Text(description,
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontWeight = FontWeight(400),
                color = Variables.OnSurfaceMedium,
                letterSpacing = 0.4.sp,
            ))
    }
}