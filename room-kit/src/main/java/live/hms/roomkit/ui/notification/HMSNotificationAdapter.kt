package live.hms.roomkit.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import live.hms.roomkit.R
import live.hms.roomkit.databinding.NotificationCardBinding
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.prebuilt_themes.setBackgroundAndColor

class HMSNotificationAdapter(
    private var notifications: List<HMSNotification> = emptyList(),
    val onDismissClicked: () -> Unit,
    val onActionButtonClicked: (HMSNotificationType) -> Unit
) : RecyclerView.Adapter<HMSNotificationAdapter.NotificationCardViewHolder>() {


    fun setItems(notifications: List<HMSNotification>) {
        this.notifications = notifications
    }

    fun getItems(): List<HMSNotification> = notifications


    inner class NotificationCardViewHolder(
        private val binding: NotificationCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notifications: HMSNotification) = binding.apply {
            applyTheme()
            heading.text = notifications.title
            icon.setImageResource(notifications.icon)
            icon.drawable.setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )

            actionButton.visibility =
                if (notifications.actionButtonText.isEmpty()) View.GONE else View.VISIBLE

            actionButton.text = notifications.actionButtonText


            actionButton.setBackgroundAndColor(
                if (notifications.isError) HMSPrebuiltTheme.getColours()?.alertErrorDefault else HMSPrebuiltTheme.getColours()?.secondaryDefault,
                HMSPrebuiltTheme.getDefaults().secondary_default,
                null
            )

            actionButton.setOnClickListener {
                onActionButtonClicked(notifications.type)
            }


            if (notifications.isError) ribbon.visibility = ViewGroup.VISIBLE
            else ribbon.visibility = ViewGroup.GONE

            if (notifications.isDismissible) crossIcon.visibility = ViewGroup.VISIBLE
            else crossIcon.visibility = ViewGroup.GONE

            crossIcon.setOnClickListener {
                onDismissClicked()
            }
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