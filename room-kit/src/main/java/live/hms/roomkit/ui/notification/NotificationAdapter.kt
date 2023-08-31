package live.hms.roomkit.ui.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import live.hms.roomkit.databinding.NotificationCardBinding
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.video.sdk.models.HMSPeer

class NotificationAdapter(
    private var notifications: List<HMSNotification> = emptyList()
) : RecyclerView.Adapter<NotificationAdapter.NotificationCardViewHolder>() {


    fun setItems(notifications: List<HMSNotification>) {
        this.notifications = notifications
    }

    fun getItems(): List<HMSNotification> = notifications


    inner class NotificationCardViewHolder(
        private val binding: NotificationCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notifications: HMSNotification) = binding.apply {
            applyTheme()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationCardViewHolder {
        val binding = NotificationCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationCardViewHolder, position: Int) {
        holder.bind(notifications[position])
    }


    override fun onBindViewHolder(
        holder: NotificationCardViewHolder, position: Int, payloads: MutableList<Any>
    ) {
//        if (payloads.contains(AudioCollectionDiffUtil.PayloadKey.ITEMS)) {
//            holder.updateWithCollection(notifications[position])
//            return
//        }

        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int = notifications.size
}