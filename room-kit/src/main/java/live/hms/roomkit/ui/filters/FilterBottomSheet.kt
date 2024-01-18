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
import android.widget.TextView
import android.widget.Toast
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
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle


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
        val pickerLayoutManager =
            PickerLayoutManager(context, PickerLayoutManager.HORIZONTAL, false).apply {
                isChangeAlpha = true;
                scaleDownBy = 0.99f;
                scaleDownDistance = 0.8f;
            }


        val adapters = PickerAdapter(requireContext(), getData(0, 100), binding.list)
        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.list)
        binding.list.apply {
            layoutManager = pickerLayoutManager
            adapter = adapters
        }


        meetingViewModel.setupFilterVideoPlugin()


        pickerLayoutManager.setOnScrollStopListener { view ->

            val text = view.findViewById<TextView>(R.id.picker_item).text.toString()
            if (text.isNullOrEmpty().not() && text.toIntOrNull() != null) {

                val number = text.toInt()
                when (currentSelectedFilter) {
                    is VideoFilter.Brightness -> meetingViewModel.filterPlugin.setBrightness(number)
                    is VideoFilter.Sharpness -> meetingViewModel.filterPlugin.setSharpness(number)
                    is VideoFilter.Saturation -> meetingViewModel.filterPlugin.setSaturation(number)
                    VideoFilter.Hue -> meetingViewModel.filterPlugin.setHue(number)
                    VideoFilter.Contrast -> meetingViewModel.filterPlugin.setContrast(number)
                    VideoFilter.Exposure -> meetingViewModel.filterPlugin.setExposure(number)
                    null -> {}
                }

            }
        }



        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.tag) {
                    is VideoFilter.Brightness -> {
                        currentSelectedFilter = VideoFilter.Brightness
                        binding.list.smoothScrollToPosition(
                            (meetingViewModel.filterPlugin.getBrightnessProgress() + padding - 1).coerceIn(
                                0, 100
                            )
                        )
                    }

                    is VideoFilter.Sharpness -> {
                        currentSelectedFilter = VideoFilter.Sharpness
                        binding.list.smoothScrollToPosition(
                            (meetingViewModel.filterPlugin.getSharpnessProgress() + padding - 1).coerceIn(
                                0, 100
                            )
                        )

                    }

                    is VideoFilter.Saturation -> {
                        currentSelectedFilter = VideoFilter.Saturation
                        binding.list.smoothScrollToPosition(
                            (meetingViewModel.filterPlugin.getSaturationProgress() + padding - 1).coerceIn(
                                0, 100
                            )
                        )

                    }

                    is VideoFilter.Hue -> {
                        currentSelectedFilter = VideoFilter.Hue
                        binding.list.smoothScrollToPosition(
                            (meetingViewModel.filterPlugin.getHueProgress() + padding - 1).coerceIn(
                                0, 100
                            )
                        )

                    }

                    is VideoFilter.Contrast -> {
                        currentSelectedFilter = VideoFilter.Contrast
                        binding.list.smoothScrollToPosition(
                            (meetingViewModel.filterPlugin.getContrastProgress() + padding - 1).coerceIn(
                                0, 100
                            )
                        )

                    }

                    is VideoFilter.Exposure -> {
                        currentSelectedFilter = VideoFilter.Exposure
                        binding.list.smoothScrollToPosition(
                            (meetingViewModel.filterPlugin.getExposureProgress() + padding - 1).coerceIn(
                                0, 100
                            )
                        )

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
                this.newTab().setText("Brightness").setTag(VideoFilter.Brightness), true
            )
            addTab(
                this.newTab().setText("Saturation").setTag(VideoFilter.Saturation)
            )
            addTab(
                this.newTab().setText("Sharpness").setTag(VideoFilter.Sharpness)
            )
            addTab(
                this.newTab().setText("Contrast").setTag(VideoFilter.Contrast)
            )
            addTab(
                this.newTab().setText("Exposure").setTag(VideoFilter.Exposure)
            )
            addTab(this.newTab().setText("Hue").setTag(VideoFilter.Hue))
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