package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.compose.Variables
import live.hms.video.sdk.models.HMSPeer
private const val PEER: String= "SwitchRoleForPeer"
class SwitchRoleBottomSheet : BottomSheetDialogFragment() {

    var changeRole : ((remotePeerId: String, toRoleName: String, force: Boolean) -> Unit)? = null
    companion object {
        const val TAG = "SwitchRoleBottomSheetTag"

        fun launch(childFragmentManager : FragmentManager, hmsPeer: HMSPeer,
                   changeRole : ((remotePeerId: String, toRoleName: String, force: Boolean) -> Unit)) {
            SwitchRoleBottomSheet()
                .apply {
                    this.changeRole = changeRole
                }
                .show(
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
fun SwitchComponent() {
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EnableButton(
            text = "Hide For Me",
            backgroundColor = Variables.SecondaryDefault,
            onEnableClicked = { })
    }
}