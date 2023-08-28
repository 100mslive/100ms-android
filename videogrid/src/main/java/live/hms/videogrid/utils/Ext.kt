package live.hms.videogrid.utils

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import live.hms.video.media.tracks.HMSVideoTrack
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun HMSVideoTrack?.isValid(): Boolean {
    return !(this == null || this.isMute || this.isDegraded)
}

fun visibilityOpacity(show: Boolean) = if (show) {
    1.0f
} else {
    0.0f
}


fun <T : ViewBinding> Fragment.viewLifecycle(): ReadWriteProperty<Fragment, T> =
    object : ReadWriteProperty<Fragment, T>, DefaultLifecycleObserver {

        private var binding: T? = null

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)


            binding = null
            owner.lifecycle.removeObserver(this)
        }

        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            return this.binding ?: error("Called before onCreateView or after onDestroyView.")
        }

        override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
            if (this.binding != null) {
                error("ViewBindingLifecycleExtension: binding already initialized to $binding")
            }

            this.binding = value

            // Observe the view lifecycle of the Fragment.
            // The view lifecycle owner is null before onCreateView and after onDestroyView.
            // The observer is automatically removed after the onDestroy event.
            thisRef.viewLifecycleOwnerLiveData.observe(thisRef.viewLifecycleOwner) {
                    it.lifecycle.addObserver(this)
                }

        }
    }

fun Fragment.contextSafe(funCall: (context: Context, activity: FragmentActivity) -> Unit) {
    if (context != null && activity != null && activity?.isFinishing == false && isAdded) {
        funCall.invoke(context!!, activity!!)
    }
}

