package live.hms.roomkit.ui.meeting
//import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.compose.Variables
import live.hms.video.sdk.models.HMSPeer

private const val PEER: String = "SwitchRoleForPeer"

class SwitchRoleBottomSheet : BottomSheetDialogFragment() {

    var changeRole: ((remotePeerId: String, toRoleName: String, force: Boolean) -> Unit)? = null

    companion object {
        const val TAG = "SwitchRoleBottomSheetTag"

        fun launch(
            childFragmentManager: FragmentManager,
            hmsPeer: HMSPeer,
            changeRole: ((remotePeerId: String, toRoleName: String, force: Boolean) -> Unit)
        ) {
            SwitchRoleBottomSheet().apply {
                    this.changeRole = changeRole
                }.show(
                    childFragmentManager, TAG
                )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    fun getPeer() = arguments?.getString(PEER)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when viewLifecycleOwner is destroyed
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                SwitchComponent()
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }
}

@Preview
@Composable
fun PreviewSwitch() {
    SwitchComponent()
}

@Composable
fun SwitchComponent(
    name: String = "Aniket",
    currentRoleName: String = "guest",
    availableRoleNames: List<String> = listOf(
        "broadcaster",
        "viewer-on-stage",
        "guest"
    )
) {
    fun getDescriptionText(): AnnotatedString {
        val nameStyle = SpanStyle(
            fontFamily = FontFamily(Font(R.font.inter_bold)),
        )
        return buildAnnotatedString {

            append("Switch the role of ")
            withStyle(nameStyle) {
                append("`")
                append(name)
                append("`")
            }
            append(" from ")
            withStyle(nameStyle) {
                append("`")
                append(currentRoleName)
                append("`")
            }
            append(" to")
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = Variables.SurfaceDim, shape = RoundedCornerShape(
                    topStart = Variables.Spacing2, topEnd = Variables.Spacing2
                )
            )
            .padding(
                start = Variables.Spacing3,
                end = Variables.Spacing3,
                top = Variables.Spacing3,
                bottom = Variables.Spacing4
            ),
        verticalArrangement = Arrangement.spacedBy(Variables.Spacing2, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.Top,
        ) {

            Text(
                modifier = Modifier.weight(1f), text = "Switch Role", style = TextStyle(
                    fontSize = 20.sp,
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
                    .clickable { /*close()*/ },
                painter = painterResource(id = R.drawable.outline_cross),
                contentDescription = "Close",
                contentScale = ContentScale.None
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(), text = getDescriptionText(), style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontWeight = FontWeight(400),
                color = Variables.OnSurfaceMedium,
                letterSpacing = 0.25.sp,
            )
        )

        Spacer(modifier = Modifier.height(Variables.Spacing3))
        var selected by remember { mutableStateOf(currentRoleName) }
        Spinner(items = availableRoleNames, selected) { selected = it }
        Spacer(modifier = Modifier.height(Variables.Spacing3))
        EnableButton("Switch Role", Variables.PrimaryDefault, {})
    }
}

@Composable
fun Spinner(items: List<String>, selectedItem: String, itemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    if (expanded) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.filter { it != selectedItem }.map {
            Text(
                text = it, Modifier.clickable {
                    itemSelected(it)
                    expanded = false
                }, style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontWeight = FontWeight(400),
                    color = Variables.OnSurfaceHigh
                )
            )
        }
    }
    else {
        Text(
            selectedItem, Modifier.clickable { expanded = !expanded }, style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontWeight = FontWeight(400),
                color = Variables.OnSurfaceHigh
            )
        )
    }
}

