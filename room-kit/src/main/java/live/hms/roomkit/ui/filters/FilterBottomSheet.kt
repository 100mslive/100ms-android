package live.hms.roomkit.ui.filters


import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetVideoFilterBinding
import live.hms.roomkit.drawableEnd
import live.hms.roomkit.drawableStart
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.plugin.video.virtualbackground.VideoPluginMode


class FilterBottomSheet(
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<BottomSheetVideoFilterBinding>()


    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }


    var currentSelectedFilter: VideoFilter? = null

    val padding = 8

    override fun onStart() {
        super.onStart()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetVideoFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.root.background = resources.getDrawable(R.drawable.gray_shape_round_dialog).apply {
            val color = getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.backgroundDefault,
                HMSPrebuiltTheme.getDefaults().background_default
            )
            setColorFilter(color, PorterDuff.Mode.ADD);
        }

        var btnArray = arrayOf(
            binding.audioOt
        )

        val borders = arrayOf(
            binding.border5
        )

        borders.forEach {
            it.setBackgroundColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.borderDefault,
                    HMSPrebuiltTheme.getDefaults().border_bright
                )
            )
        }



        binding.closeBtn.drawable.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.closeBtn.setOnClickListener {
            dismissAllowingStateLoss()
        }




        (binding.pluginSwitch as SwitchCompat).isChecked = true
        meetingViewModel.setupFilterVideoPlugin()
        (binding.pluginSwitch as SwitchCompat).setTextColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )
        (binding.pluginSwitch as SwitchCompat).setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) meetingViewModel.setupFilterVideoPlugin()
            else meetingViewModel.removeVideoFilterPlugIn()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                when (currentSelectedFilter) {
                    is VideoFilter.Blur -> {
                        currentSelectedFilter = VideoFilter.Blur
                    }
                    is VideoFilter.Brightness -> meetingViewModel.filterPlugin.setBrightness(
                        progress / 100f
                    )

                    is VideoFilter.Sharpness -> meetingViewModel.filterPlugin.setSharpness(progress / 100f)
                    is VideoFilter.Contrast -> meetingViewModel.filterPlugin.setContrast(progress / 100f)
                    is VideoFilter.Redness -> meetingViewModel.filterPlugin.setRedness(progress / 100f)
                    is VideoFilter.Smoothness -> meetingViewModel.filterPlugin.setSmoothness(progress / 100f)
                    null -> {}
                    else -> {}
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })




        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.tag) {
//                    is VideoFilter.Brightness -> {
//                        currentSelectedFilter = VideoFilter.Brightness
//                        binding.seekBar.progress =
//                            (meetingViewModel.virtualBackGroundPlugin.getCurrentBlurPercentage())
//                        if (lastPluginMode != null) {
//                            meetingViewModel.isVbPlugin = lastPluginMode!!
//                        }
////                        update()
//                    }
//
//                    is VideoFilter.Sharpness -> {
//                        currentSelectedFilter = VideoFilter.Sharpness
//                        binding.seekBar.progress =
//                            (meetingViewModel.filterPlugin.getSharpnessProgress() * 100f).toInt()
//                    }
//
//
//                    is VideoFilter.Contrast -> {
//                        currentSelectedFilter = VideoFilter.Contrast
//                        binding.seekBar.progress =
//                            ( meetingViewModel.filterPlugin.getContrastProgress() * 100f).toInt()
//                    }
//
//                    is VideoFilter.Redness -> {
//                        currentSelectedFilter = VideoFilter.Redness
//                        binding.seekBar.progress =
//                            (meetingViewModel.filterPlugin.getRednessProgress() * 100f).toInt()
//                    }
//
//                    is VideoFilter.Smoothness -> {
//                        currentSelectedFilter = VideoFilter.Smoothness
//                        binding.seekBar.progress =
//                            ( meetingViewModel.filterPlugin.getSmoothnessProgress() * 100f).toInt()
//                    }
                    is VideoFilter.Blur -> {
                        meetingViewModel.isVbPlugin = VideoPluginMode.BLUR_BACKGROUND
                    }


                    else -> {

                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })



        binding.tabLayout.apply {
            setTabTextColors(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceLow,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                ), getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )

            )
            addTab(
            this.newTab().setText(meetingViewModel.isVbPlugin.toString())
                .setTag(VideoFilter.Blur), true
            )
            addTab(
                this.newTab().setText("Disable Effects").setTag(VideoFilter.Quality)
            )
            setSelectedTabIndicatorColor(Color.TRANSPARENT)
        }







        btnArray.forEach {
            it.setTextColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )

            it.drawableEnd?.setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )

            it.drawableStart?.setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )
        }

    }

    private fun getData(from: Int, to: Int): List<String> {
        val data: MutableList<String> = ArrayList()


        for (i in 0 until padding) {
            data.add("")
        }
        for (i in from until to) {
            data.add(i.toString())
        }

        for (i in 0 until padding) {
            data.add("")
        }
        return data
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

}