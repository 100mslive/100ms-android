package live.hms.roomkit.ui.filters


import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            addTab(this.newTab().setText("Brightness"))
            addTab(this.newTab().setText("Saturation"))
            addTab(this.newTab().setText("Sharpness"))
        }

        var pickerLayoutManager =
            PickerLayoutManager(context, PickerLayoutManager.HORIZONTAL, false).apply {
                isChangeAlpha = true;
                scaleDownBy = 0.99f;
                scaleDownDistance = 0.8f;
            }

        val adapters = PickerAdapter(requireContext(), getData(100), binding.list)
        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.list)
        binding.list.apply {
            layoutManager = pickerLayoutManager
            adapter = adapters
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })



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

    private fun getData(count: Int): List<String> {
        val data: MutableList<String> = ArrayList()
        for (i in 0 until count) {
            data.add(i.toString())
        }
        return data
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

}