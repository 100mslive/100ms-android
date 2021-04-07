package live.hms.app2.util

import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val TAG = "ViewBindingLifecycleExtension"

/**
 * An extension to bind and unbind a value based on the view lifecycle of a Fragment.
 * The binding will be unbound in onDestroyView.
 *
 * @throws IllegalStateException If the getter is invoked before the binding is set,
 *                               or after onDestroyView an exception is thrown.
 */

fun <T : ViewBinding> Fragment.viewLifecycle(): ReadWriteProperty<Fragment, T> =
  object : ReadWriteProperty<Fragment, T>, DefaultLifecycleObserver {

    private var binding: T? = null

    override fun onDestroy(owner: LifecycleOwner) {
      super.onDestroy(owner)
      crashlyticsLog(TAG, "Removing reference to $binding during onDestroy($owner)")

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
      Log.d(TAG, "Updated binding to $binding (source: $thisRef)")

      // Observe the view lifecycle of the Fragment.
      // The view lifecycle owner is null before onCreateView and after onDestroyView.
      // The observer is automatically removed after the onDestroy event.
      thisRef
        .viewLifecycleOwnerLiveData
        .observe(thisRef.viewLifecycleOwner) {
          it.lifecycle.addObserver(this)
        }

      crashlyticsLog(
        TAG,
        "Observing lifecycle of $thisRef -- " +
            "binding reference will be removed when view gets destroyed"
      )
    }
  }

/**
 * An overloaded inline function to bind and unbind a value in an Activity.
 */

inline fun <T : ViewBinding> AppCompatActivity.viewLifecycle(
  crossinline bindingInflater: (LayoutInflater) -> T
) =
  lazy(LazyThreadSafetyMode.NONE) {
    bindingInflater.invoke(layoutInflater)
  }
