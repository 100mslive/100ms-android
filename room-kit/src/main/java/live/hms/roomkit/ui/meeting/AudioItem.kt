package live.hms.roomkit.ui.meeting

import android.graphics.PorterDuff
import android.os.Build
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemDeviceDetailBinding
import live.hms.roomkit.drawableEnd
import live.hms.roomkit.drawableStart
import live.hms.roomkit.setDrawables
import live.hms.roomkit.setOnSingleClickListener
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.video.audio.HMSAudioManager
import live.hms.video.audio.manager.AudioManagerUtil
import kotlin.math.roundToInt

class AudioItem(
    private var title: String,
    private var subTitle: String? = null,
    private val isSelected: Boolean,
    @DrawableRes private val drawableRes: Int,
    val type: HMSAudioManager.AudioDevice = HMSAudioManager.AudioDevice.AUTOMATIC,
    val id: Int? = null,
    private val onClick: (HMSAudioManager.AudioDevice, Int?) -> Unit,

    ) : BindableItem<ItemDeviceDetailBinding>() {


    override fun bind(binding: ItemDeviceDetailBinding, position: Int) {
        if (isSelected) binding.audioText.setDrawables(
            end = binding.audioText.context?.getDrawable(
                R.drawable.tick
            )
        )
        else binding.audioText.setDrawables(end = null)

        binding.audioText.setDrawables(
            start = binding.audioText.context?.getDrawable(
                drawableRes
            )
        )


        binding.audioText.text = buildSpannedString {
            append(title)
            if (subTitle.isNullOrEmpty().not() && type.toString() == AudioManagerUtil.AudioDevice.BLUETOOTH.toString()) {
                bold { append(" ( ${subTitle.orEmpty()} )") }
            }
        }

        binding.root.setOnSingleClickListener {
            onClick.invoke(type, id)
        }


        binding.audioText.setTextColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.audioText.drawableEnd?.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.audioText.drawableStart?.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )




        binding.border4.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.borderDefault,
                HMSPrebuiltTheme.getDefaults().border_bright
            )
        )
    }


    override fun getLayout(): Int = R.layout.item_device_detail


    override fun initializeViewBinding(view: View) = ItemDeviceDetailBinding.bind(view)

}