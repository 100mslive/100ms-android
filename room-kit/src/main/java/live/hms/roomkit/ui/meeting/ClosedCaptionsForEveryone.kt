package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.compose.Variables

class ClosedCaptionsForEveryone : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when viewLifecycleOwner is destroyed
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                Display()
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

}

@Preview
@Composable
fun DisplayFirst() {
    Display()
}

@Composable
fun Display() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.SurfaceDim, shape = RoundedCornerShape(topStart = 16.dp,topEnd = 16.dp))
            .padding(start = Variables.Spacing3,
        end = Variables.Spacing3,
        top = Variables.Spacing3,
        bottom = Variables.Spacing4)
        ,
        verticalArrangement = Arrangement.spacedBy(Variables.Spacing2, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.Top,
        ) {

            Text(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                text = "Enable Closed Captions (CC) for this session?", style = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_bold)),
                    fontWeight = FontWeight(600),
                    color = Variables.OnSecondaryHigh,
                    letterSpacing = 0.15.sp,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                modifier = Modifier
                    .padding(1.dp)
                    .size(24.dp),
                painter = painterResource(id = live.hms.roomkit.R.drawable.outline_cross),
                contentDescription = "image description",
                contentScale = ContentScale.None
            )
        }
        EnableButton()
        DescriptionText()
    }
}

@Composable
fun EnableButton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = Variables.PrimaryDefault, shape = RoundedCornerShape(size = 8.dp))
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Enable for Everyone",

            // Desktop/Button-Semibold-16px
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_bold)),
                fontWeight = FontWeight(600),
                color = Variables.OnPrimaryHigh,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
            )
        )
    }

}
@Composable
fun DescriptionText() {
    Text(
        text = "This will enable Closed Captions for everyone in this room. You can disable it later.",

        // Desktop/Body 2-Regular-14px
        style = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
            fontWeight = FontWeight(400),
            color = Variables.OnSurfaceMedium,
            letterSpacing = 0.25.sp,
        )
    )
}