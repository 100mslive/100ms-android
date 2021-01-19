package live.hms.android100ms.ui.home.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import live.hms.android100ms.databinding.FragmentSettingsBinding
import live.hms.android100ms.util.viewLifecycle

class SettingsFragment : Fragment() {

  private var binding by viewLifecycle<FragmentSettingsBinding>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentSettingsBinding.inflate(inflater, container, false)
    initButtons()
    return binding.root
  }

  private fun initButtons() {
    binding.containerAdvanced.apply {
      visibility = View.GONE // Default to hidden

      binding.buttonAdvancedSettings.setOnClickListener {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
      }
    }
  }
}