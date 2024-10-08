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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.prebuilt_themes.Variables
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.role.HMSRole

private const val PEER: String = "SwitchRoleForPeerInfo"

class SwitchRoleBottomSheet : BottomSheetDialogFragment() {

    var changeRole: ((remotePeerId: String, toRoleName: String, force: Boolean) -> Unit)? = null

    companion object {
        const val TAG = "SwitchRoleBottomSheetTag"

        fun launch(
            childFragmentManager: FragmentManager,
            hmsPeer: HMSPeer,
            allRoles: List<String>,
            changeRole: ((remotePeerId: String, toRoleName: String, force: Boolean) -> Unit)
        ) {

            val args = bundleOf(
                "name" to hmsPeer.name,
                "allRoles" to allRoles,
                "currentRole" to hmsPeer.hmsRole.name,
                "peerId" to hmsPeer.peerID
            )
            SwitchRoleBottomSheet().apply {
                this.changeRole = changeRole
                arguments = args
            }.show(
                childFragmentManager, TAG
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        // If the fragment is recreated after the activity is killed, close it.
        if (changeRole == null) dismissAllowingStateLoss()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun getPeerInfo(): PeerInfo? {
        val name = arguments?.getString("name")
        val allRoles = arguments?.getStringArrayList("allRoles")
        val currentRole = arguments?.getString("currentRole")
        val peerId = arguments?.getString("peerId")
        return if (name != null && allRoles != null && currentRole != null && peerId != null) {
            PeerInfo(
                name, allRoles, currentRole, peerId
            )
        } else {
            null
        }
    }

    data class PeerInfo(
        val name: String, val roles: List<String>, val currentRole: String, val peerId: String
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when viewLifecycleOwner is destroyed
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                val peerInfo by remember { mutableStateOf<PeerInfo?>(getPeerInfo()) }
                if (peerInfo == null) {
                    dismissAllowingStateLoss()
                } else {
                    SwitchComponent(name = peerInfo?.name ?: "",
                        currentRoleName = peerInfo?.currentRole ?: "",
                        availableRoleNames = peerInfo?.roles ?: emptyList(),
                        {
                            changeRole?.invoke(peerInfo?.peerId ?: "", it, true)
                        }) {
                        dismissAllowingStateLoss()
                    }
                }
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
    val list = listOf(
        "broadcaster", "viewer-on-stage", "guest"
    )
    SwitchComponent(name = "args", currentRoleName = list[0], availableRoleNames = list, {}) {}
}

@Composable
fun SwitchComponent(
    name: String,
    currentRoleName: String,
    availableRoleNames: List<String>,
    changeRole: (String) -> Unit,
    dismiss: () -> Unit
) {
    fun getDescriptionText(): AnnotatedString {
        val nameStyle = SpanStyle(
            fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_bold)),
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
        verticalArrangement = Arrangement.spacedBy(Variables.Spacing3, Alignment.Top),
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
                fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_regular)),
                fontWeight = FontWeight(400),
                color = Variables.OnSurfaceMedium,
                letterSpacing = 0.25.sp,
            )
        )

        var selected by remember { mutableStateOf(startingSelectedRole(availableRoleNames, currentRoleName)) }
        var enabled by remember { mutableStateOf(currentRoleName != selected) }
        Spinner(items = availableRoleNames, selected) {
            selected = it
            enabled = currentRoleName != selected
        }
        ChangeRoleButton(enabled, "Switch Role") {
            changeRole(selected)
            dismiss()
        }
    }
}

fun startingSelectedRole(availableRoleNames: List<String>, currentRoleName: String): String =
    if(availableRoleNames.size == 2)
        availableRoleNames.firstOrNull { it != currentRoleName } ?: currentRoleName
    else
        currentRoleName

@Composable
fun Spinner(items: List<String>, selectedItem: String, itemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    if (expanded) {
        MinimalDialog(items = items.filter { it != selectedItem }, onItemClick = {
            itemSelected(it)
            expanded = false
        }) {
            expanded = false
        }
    }
    SpinnerHeader(selectedItem) { expanded = true }
}

@Composable
fun SpinnerHeader(selectedItem: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(color = Variables.SurfaceDefault, shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpinnerText(it = selectedItem,
            Modifier
                .clickable { onClick() }
                .weight(1f))
        Image(
            painter = painterResource(id = R.drawable.chevron_down),
            contentDescription = "expand",
            contentScale = ContentScale.None
        )
    }
}

@Composable
fun SpinnerText(it: String, modifier: Modifier) {
    Text(
        text = it, modifier, style = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontFamily = FontFamily(Font(live.hms.prebuilt_themes.R.font.inter_regular)),
            fontWeight = FontWeight(400),
            color = Variables.OnSurfaceHigh
        )
    )
}

@Preview
@Composable
fun SingleOptionDialog() {
    MinimalDialog(listOf("host"), {}, {})
}

@Preview
@Composable
fun FourOptionDialog() {
    MinimalDialog(listOf("host","broadcaster","carts","beans"), {}, {})
}

@Composable
fun MinimalDialog(
    items: List<String>, onItemClick: (String) -> Unit, onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            colors = CardDefaults.cardColors(Variables.SurfaceDefault),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(min= 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                items.forEachIndexed { index, text ->
                    if (index > 0) {
                        Divider()
                    }
                    Button({},
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(Variables.SurfaceDefault)) {
                        SpinnerText(text,
                            Modifier
                                .padding(12.dp)
                                .clickable { onItemClick(text) })
                    }

                }
            }
        }
    }
}

@Composable
fun ChangeRoleButton(
    enabled : Boolean,
    text: String, onEnableClicked: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = if(enabled) Variables.PrimaryDefault else Variables.SecondaryDefault, shape = RoundedCornerShape(size = 8.dp))
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.clickable { onEnableClicked.invoke(text) }, text = text,

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