package live.hms.vb_prebuilt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import live.hms.prebuilt_themes.Variables
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.prebuilt_themes.Variables.Companion.Spacing1
import live.hms.prebuilt_themes.Variables.Companion.Spacing2

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

//    override fun getTheme(): Int {
//        return R.style.AppBottomSheetDialogTheme
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when viewLifecycleOwner is destroyed
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                VirtualBackgroundOptions(
                    close = {
                    dismissAllowingStateLoss() },
                    removeEffects = {},
                    blur = {},
                    backgroundSelected = {},
                    onSliderValueChanged = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    VirtualBackgroundOptions(close = {},
        allBackgrounds = listOf("a","b","c","d","d"),
        defaultBackground = "a",
        removeEffects = {},
        blur = {},
        backgroundSelected = {},
        onSliderValueChanged = {})
}

@Composable
fun VirtualBackgroundOptions(
    videoView : @Composable () -> Unit = { Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RectangleShape)
            .background(Color.Gray)
    )},
    allBackgrounds : List<String> = emptyList(),
    defaultBackground: String? = null,
    close : () -> Unit,
    removeEffects :() -> Unit,
    blur : () -> Unit,
    backgroundSelected : (String) -> Unit,
    onSliderValueChanged : (Float) -> Unit,
    ) {
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

        var selectedEffect by remember { mutableStateOf(SelectedEffect.NO_EFFECT) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing2)) {
            VbOptionButton(drawable = live.hms.vb_prebuilt.R.drawable.vb_cross_circle,
                "No effect", selectedEffect == SelectedEffect.NO_EFFECT) {
                selectedEffect = SelectedEffect.NO_EFFECT
                removeEffects()
            }
            VbOptionButton(drawable = live.hms.vb_prebuilt.R.drawable.vb_blur_background,
                "Blur",selectedEffect == SelectedEffect.BLUR) {
                selectedEffect = SelectedEffect.BLUR
                blur()
            }
        }
        var sliderPosition by remember { mutableFloatStateOf(30f) }
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing1)) {
            Image(painter = painterResource(id = live.hms.vb_prebuilt.R.drawable.vb_slider_blur_people),
                contentDescription = "effect slider")
            Slider(
                modifier = Modifier.weight(1f),
                value = sliderPosition,
                onValueChange = { sliderPosition = it
                    onSliderValueChanged.invoke(it)},
                valueRange = 0f..100f,
                steps = 1,
                colors = SliderDefaults.colors(
                    thumbColor = Variables.PrimaryDefault,
                    activeTrackColor = Variables.PrimaryDefault,
                    inactiveTrackColor = Variables.SecondaryDefault,
                )
            )
            Image(painter = painterResource(id = live.hms.vb_prebuilt.R.drawable.vb_slider_blur_people_max),
                contentDescription = "effect slider")
        }
        Spacer(modifier = Modifier.height(Variables.Spacing3))
        Text(
            text = "Backgrounds",
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontWeight = FontWeight(600),
                color = Variables.OnSurfaceHigh,
                letterSpacing = 0.1.sp,
            )
        )
        var currentBackground by remember { mutableStateOf<String?>(defaultBackground) }
        BackgroundListing(allBackgrounds, currentBackground) { selectedBackground ->
            currentBackground = selectedBackground
            backgroundSelected(selectedBackground)
        }
    }
}

@Preview
@Composable
private fun BackgroundGallery() {
    BackgroundListing(
        backgrounds = listOf("https://img-vb.100ms.live/layouts/defaults/vb-1.jpg",
            "https://img-vb.100ms.live/layouts/defaults/vb-2.jpg"),
        currentBackground = "https://img-vb.100ms.live/layouts/defaults/vb-1.jpg"
    ) {

    }
}

/**
 * Takes a list of background images
 * Notifies when one of them is selected.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BackgroundListing(backgrounds : List<String>,
    currentBackground : String?,
    onBackgroundSelected : (String) -> Unit) {
    LazyVerticalGrid(
        horizontalArrangement = Arrangement.spacedBy(Spacing2),
        verticalArrangement = Arrangement.spacedBy(Spacing2),
        columns = GridCells.Fixed(3)
    ) {
        itemsIndexed(backgrounds) { photo, _ ->

            GlideImage(
                model = photo,
                contentDescription = "background",
                loading = placeholder { CircularProgressIndicator() }
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
                fontFamily = FontFamily(Font(R.font.inter_bold)),
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
enum class SelectedEffect {
    NO_EFFECT,
    BLUR
}
@Composable
fun VbOptionButton(@DrawableRes drawable :  Int,
                   description : String,
                   selected : Boolean,
                   onClick : () -> Unit) {
    Column(
        Modifier
            .width(103.33334.dp)
            .height(86.dp)
            .clickable { onClick() }
            .background(shape = RoundedCornerShape(16.dp),
                color = Variables.SurfaceBright)
            // todo possibly improve
            .border(2.dp, if(selected) Variables.PrimaryDefault else (Variables.SurfaceBright),
                shape = RoundedCornerShape(16.dp))
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(id = drawable), contentDescription = "background")
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