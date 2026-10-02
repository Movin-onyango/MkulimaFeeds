
package com.mkulimafeeds

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mkulimafeeds.domain.model.notification.RemoteNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private var notifications: List<RemoteNotification>,
    private val onNotifClick: (RemoteNotification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotifViewHolder>() {

    class NotifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView =
            view.findViewById(R.id.tvNotifTitle)

        val tvMsg: TextView =
            view.findViewById(R.id.tvNotifMsg)

        val tvTime: TextView =
            view.findViewById(R.id.tvNotifTime)

        val ivIcon: ImageView =
            view.findViewById(R.id.ivNotifIcon)

        val iconContainer: MaterialCardView =
            view.findViewById(R.id.iconContainer)

        val rootCard: MaterialCardView =
            view as MaterialCardView
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotifViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_notification,
                    parent,
                    false
                )

        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NotifViewHolder,
        position: Int
    ) {
        val notification =
            notifications[position]

        holder.tvTitle.text =
            notification.title

        holder.tvMsg.text =
            notification.message

        holder.tvTime.text =
            formatTimestamp(notification.createdAt)

        val context =
            holder.itemView.context

        when (
            notification.type
                .trim()
                .uppercase()
        ) {

            "ORDER_ASSIGNED" -> {

                holder.ivIcon.setImageResource(
                    android.R.drawable.ic_dialog_map
                )

                holder.iconContainer
                    .setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.notif_bg_blue
                        )
                    )

                holder.ivIcon.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.notif_blue
                    )
                )

                holder.tvTitle.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.notif_blue
                    )
                )
            }

            "ORDER_STATUS_UPDATE" -> {

                holder.ivIcon.setImageResource(
                    android.R.drawable.stat_sys_download_done
                )

                holder.iconContainer
                    .setCardBackgroundColor(
                        Color.parseColor("#E6F4EA")
                    )

                holder.ivIcon.setColorFilter(
                    Color.parseColor("#1E8E3E")
                )

                holder.tvTitle.setTextColor(
                    Color.parseColor("#1E8E3E")
                )
            }

            "STOCK_ALERT" -> {

                holder.ivIcon.setImageResource(
                    android.R.drawable.stat_sys_warning
                )

                holder.iconContainer
                    .setCardBackgroundColor(
                        Color.parseColor("#FFF4E5")
                    )

                holder.ivIcon.setColorFilter(
                    Color.parseColor("#D97706")
                )

                holder.tvTitle.setTextColor(
                    Color.parseColor("#D97706")
                )
            }

            else -> {

                holder.ivIcon.setImageResource(
                    android.R.drawable.ic_popup_reminder
                )

                holder.iconContainer
                    .setCardBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.brand_blue_light
                        )
                    )

                holder.ivIcon.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.brand_blue
                    )
                )

                holder.tvTitle.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.brand_blue
                    )
                )
            }
        }

        if (!notification.isRead) {

            holder.rootCard.setStrokeColor(
                ContextCompat.getColor(
                    context,
                    R.color.brand_blue
                )
            )

            holder.rootCard.setStrokeWidth(2)

        } else {

            holder.rootCard.setStrokeColor(
                Color.parseColor("#F0F0F0")
            )

            holder.rootCard.setStrokeWidth(1)
        }

        holder.itemView.setOnClickListener {
            onNotifClick(notification)
        }
    }

    override fun getItemCount(): Int =
        notifications.size

    fun updateList(
        newList: List<RemoteNotification>
    ) {
        notifications = newList
        notifyDataSetChanged()
    }

    private fun formatTimestamp(
        timestamp: Long
    ): String {

        if (timestamp <= 0L) {
            return ""
        }

        return try {

            SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )

        } catch (_: Exception) {
            ""
        }
    }
}

