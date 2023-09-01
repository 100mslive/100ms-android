package live.hms.roomkit.ui.notification

import androidx.recyclerview.widget.DiffUtil

class HMSNotificationDiffCallBack(
    private val old: List<HMSNotification>,
    private val new: List<HMSNotification>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return old.size
    }

    override fun getNewListSize(): Int {
        return new.size
    }

    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return old[oldPosition].id == new[newPosition].id
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return old[oldPosition] == new[newPosition]
    }

}
