package live.hms.vb_prebuilt

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.hms.prebuilt_themes.Variables
import live.hms.prebuilt_themes.Variables.Companion.Spacing1
import live.hms.prebuilt_themes.Variables.Companion.Spacing2


@Preview
@Composable
private fun Preview() {
    VirtualBackgroundOptions(close = {},
        allBackgrounds = listOf("a","b","c","d","d"),
        defaultBackground = "a",
        removeEffects = {},
        blur = {},
        backgroundSelected = {a,b ->},
        onBlurPercentageChanged = {},
        initialBlurPercentage = 30f,
        currentlySelectedVbMode = SelectedEffect.NO_EFFECT)
}

@Composable
fun VirtualBackgroundOptions(
    videoView: @Composable (modifier: Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier.then(
                Modifier.Companion
                    .clip(RectangleShape)
                    .background(Color.Gray)
            )
        )
    },
    allBackgrounds: List<String> = emptyList(),
    defaultBackground: String? = null,
    currentlySelectedVbMode : SelectedEffect,
    close: () -> Unit,
    removeEffects: () -> Unit,
    backgroundSelected: (String, Bitmap) -> Unit,
    blur: (Float) -> Unit,
    onBlurPercentageChanged: (Float) -> Unit,
    initialBlurPercentage: Float,
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
        Box(modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center) {
            videoView(
                Modifier
                    .width(166.dp)
                    .height(280.dp))
        }
        Text(
            text = "Effects", style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_regular)),
                fontWeight = FontWeight(600),
                color = Variables.OnSecondaryHigh,
                letterSpacing = 0.15.sp,
            )
        )

        var currentBackground by remember { mutableStateOf(defaultBackground) }

        var currentBlurPercentage by remember { mutableFloatStateOf(initialBlurPercentage) }
        var selectedEffect by remember { mutableStateOf(currentlySelectedVbMode) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing2)) {
            VbOptionButton(drawable = live.hms.vb_prebuilt.R.drawable.vb_cross_circle,
                "No effect", selectedEffect == SelectedEffect.NO_EFFECT) {
                selectedEffect = SelectedEffect.NO_EFFECT
                currentBackground = null
                removeEffects()
            }
            VbOptionButton(drawable = live.hms.vb_prebuilt.R.drawable.vb_blur_background,
                "Blur",selectedEffect == SelectedEffect.BLUR) {
                selectedEffect = SelectedEffect.BLUR
                currentBackground = null
                blur(currentBlurPercentage)
            }
        }

        if(selectedEffect == SelectedEffect.BLUR) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing1)
            ) {
                Image(
                    painter = painterResource(id = live.hms.vb_prebuilt.R.drawable.vb_slider_blur_people),
                    contentDescription = "effect slider"
                )
                Slider(
                    modifier = Modifier.weight(1f),
                    value = currentBlurPercentage,
                    onValueChange = {
                        currentBlurPercentage = it
                        onBlurPercentageChanged.invoke(it)
                    },
                    valueRange = 0f..100f,
                    steps = 100,
                    colors = SliderDefaults.colors(
                        thumbColor = Variables.PrimaryDefault,
                        activeTrackColor = Variables.PrimaryDefault,
                        inactiveTrackColor = Variables.SecondaryDefault,
                    )
                )
                Image(
                    painter = painterResource(id = live.hms.vb_prebuilt.R.drawable.vb_slider_blur_people_max),
                    contentDescription = "effect slider"
                )
            }
        }
        if(allBackgrounds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Variables.Spacing2))
            Text(
                text = "Backgrounds",
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_bold)),
                    fontWeight = FontWeight(600),
                    color = Variables.OnSurfaceHigh,
                    letterSpacing = 0.1.sp,
                )
            )
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            BackgroundListing(allBackgrounds, currentBackground) { selectedBackground ->
                currentBackground = selectedBackground
                selectedEffect = SelectedEffect.BACKGROUND
                // Running here instead of launched effect because it shouldn't run
                // the very first time we set current background to something.
                coroutineScope.launch {
                    launch(Dispatchers.IO) {
                        backgroundSelected(
                            selectedBackground,
                            Glide.with(context).asBitmap().load(selectedBackground).submit().get()
                        )
                    }
                }
            }
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
        itemsIndexed(backgrounds) { _, photoUrl ->
            val m = Modifier.clickable { onBackgroundSelected(photoUrl) }
                .clip(RoundedCornerShape(8.dp))

            val modifier = if(currentBackground == photoUrl) {
                m.then(Modifier.border(
                        2.dp, Variables.PrimaryDefault,
                shape = RoundedCornerShape(8.dp)
                ))
            } else {
                m
            }
            GlideImage(
                modifier = modifier,
                model = photoUrl,
                contentDescription = "background",
                // using the composable placeholder causes a lot of re-rendering
//                loading = placeholder(R.drawable.gray_round_stroked_drawable)
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
            painter = painterResource(id = live.hms.prebuilt_themes.R.drawable.outline_cross),
            contentDescription = "Close",
            contentScale = ContentScale.None
        )
    }
}
enum class SelectedEffect {
    NO_EFFECT,
    BLUR,
    BACKGROUND
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
            .background(
                shape = RoundedCornerShape(16.dp),
                color = Variables.SurfaceBright
            )
            .border(
                2.dp, if (selected) Variables.PrimaryDefault else (Variables.SurfaceBright),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(id = drawable), contentDescription = "background")
        Text(description,
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_regular)),
                fontWeight = FontWeight(400),
                color = Variables.OnSurfaceMedium,
                letterSpacing = 0.4.sp,
            ))
    }
}