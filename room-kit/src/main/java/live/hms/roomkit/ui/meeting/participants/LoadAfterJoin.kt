package live.hms.roomkit.ui.meeting.participants

import androidx.lifecycle.LifecycleOwner
import live.hms.roomkit.ui.meeting.MeetingViewModel

/**
 * Many fragments need to load data out of the MeetingViewModel or the ChatViewModel when the view
 * is created. However, if they do this just in onViewCreated, then if the app is destroyed by
 * going in the background, they won't wait until the app has joined the room to load this data.
 * Which means they will crash at some point.
 * Everything covered in this class has been tested by
 * 1. Turning on kill `background activities`
 * 2. Leave the app, so it's in the background and open it again.
 *
 * Fragments particularly must have empty constructors only.
 * This can be resolved by:
 * 1. For methods, make them lateinit and apply them after the fragment is created.
 * 2. For data, pass it in an argument.
 */
class LoadAfterJoin(meetingViewModel: MeetingViewModel, viewLifecycleOwner: LifecycleOwner, afterJoin: () -> Unit ) {
    private var inited = false
    init {
        meetingViewModel.joined.observe(viewLifecycleOwner) { joined ->
            if(!inited && joined) {
                inited = true
                afterJoin()
            }
        }
    }
}