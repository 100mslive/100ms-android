package live.hms.app2.util

import android.util.Log
import android.view.View
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.video.utils.SharedEglContext
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

object SurfaceViewRendererUtil {

  private const val TAG = "SurfaceViewRendererUtil"

  /**
   * Counter used to analyse the number of [EglBase.Context] allocated in
   * the memory.
   *
   * @warning Make sure that this counter is synchronized
   */
  private var initializedContextCount = 0

  /**
   * Add [view] as sink for [item]'s video and initialize [EglBase.Context]
   *
   * @param view SurfaceViewRenderer instance which will be initialized with context
   * @param item VideoTrack source, in case the videoTrack is null the context will
   *  not be initialized
   * @param metadata Optional extra data which will be logged when context is
   *  successfully initialized
   */
  fun bind(
    view: SurfaceViewRenderer,
    item: MeetingTrack,
    metadata: String = "",
  ): Boolean {
    if (item.video == null) return false

    Log.v(TAG, "bind called :: ${item.peer.name}")

    view.apply {
      View.VISIBLE
      val context: EglBase.Context = SharedEglContext.context

      init(context, null)
      ++initializedContextCount

      item.video!!.addSink(this)
    }

    crashlyticsLog(
      TAG,
      "[count=$initializedContextCount, $metadata] Initialized EglContext for item=$item"
    )
    return true
  }

  /**
   * Remove [view] as sink for [item]'s video and release [EglBase.Context]
   *
   * @param view SurfaceViewRenderer instance which will release the context
   * @param item VideoTrack source, in case the videoTrack is null the context will
   *  not be released
   * @param metadata Optional extra data which will be logged when context is
   *  successfully released
   */
  fun unbind(view: SurfaceViewRenderer, item: MeetingTrack, metadata: String = ""): Boolean {
    if (item.video == null) return false

    Log.v(TAG, "unbind called :: ${item.peer.name}")
    view.apply {
      // NOTE: We don't dispose off the MediaStreamTrack here as it can
      // be re-used by the any other view

      item.video!!.removeSink(this)
      release()
      --initializedContextCount
    }

    crashlyticsLog(
      TAG,
      "[count=$initializedContextCount, $metadata] Released EglContext for item=$item"
    )
    return true
  }
}