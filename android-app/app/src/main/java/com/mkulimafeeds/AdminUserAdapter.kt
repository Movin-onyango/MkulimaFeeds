package com.mkulimafeeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUserAdapter(
    private var users: List<User>,
    private val onUserClick: (User) -> Unit,
    private val onDeactivateUser: (User) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val ivAvatar: ShapeableImageView =
            view.findViewById(R.id.ivUserAvatar)

        val tvName: TextView =
            view.findViewById(R.id.tvUserName)

        val tvEmail: TextView =
            view.findViewById(R.id.tvUserEmail)

        val tvRoleBadge: TextView =
            view.findViewById(R.id.tvRoleBadge)

        val tvStatusBadge: TextView =
            view.findViewById(R.id.tvStatusBadge)

        val tvTotalOrders: TextView =
            view.findViewById(R.id.tvTotalOrders)

        val tvJoinedDate: TextView =
            view.findViewById(R.id.tvJoinedDate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_admin_user_card,
                    parent,
                    false
                )

        return UserViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: UserViewHolder,
        position: Int
    ) {

        val user = users[position]

        holder.tvName.text = user.name
        holder.tvEmail.text = user.email
        holder.tvTotalOrders.text = "${user.totalOrders} Orders"
        holder.tvJoinedDate.text = user.joinedDate
        holder.ivAvatar.setImageResource(user.avatarResId)

        // ---------------------------------------------------
        // ROLE BADGE
        // ---------------------------------------------------
        holder.tvRoleBadge.text = user.role.uppercase()

        val context = holder.itemView.context

        when (user.role.uppercase()) {
            "ADMIN" -> {
                holder.tvRoleBadge.setBackgroundResource(
                    R.drawable.bg_status_badge
                )
                holder.tvRoleBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.white)
                )
            }
            "STAFF" -> {
                holder.tvRoleBadge.setBackgroundResource(
                    R.drawable.bg_status_badge_light_blue
                )
                holder.tvRoleBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.brand_blue)
                )
            }
            "DEALER" -> {
                holder.tvRoleBadge.setBackgroundResource(
                    R.drawable.bg_status_in_stock
                )
                holder.tvRoleBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.brand_dark_green)
                )
            }
            else -> { // CUSTOMER
                holder.tvRoleBadge.setBackgroundResource(
                    R.drawable.bg_input_field
                )
                holder.tvRoleBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.text_grey)
                )
            }
        }

        // ---------------------------------------------------
        // STATUS BADGE
        // ---------------------------------------------------
        val status = user.status.uppercase()
        holder.tvStatusBadge.text = status

        when (status) {
            "ACTIVE" -> {
                holder.tvStatusBadge.setBackgroundResource(
                    R.drawable.bg_status_in_stock
                )
                holder.tvStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.brand_dark_green)
                )
            }
            "PENDING" -> {
                holder.tvStatusBadge.setBackgroundResource(
                    R.drawable.bg_status_pending
                )
                holder.tvStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.text_grey)
                )
            }
            "SUSPENDED" -> {
                holder.tvStatusBadge.setBackgroundResource(
                    R.drawable.bg_backend_status_error
                )
                holder.tvStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.white)
                )
            }
            "INACTIVE" -> {
                holder.tvStatusBadge.setBackgroundResource(
                    R.drawable.bg_status_pending
                )
                holder.tvStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.text_grey)
                )
            }
        }

        // ---------------------------------------------------
        // TAP = EDIT
        // ---------------------------------------------------
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }

        // ---------------------------------------------------
        // LONG PRESS = DEACTIVATE
        // ---------------------------------------------------
        holder.itemView.setOnLongClickListener {
            if (user.status.equals("INACTIVE", ignoreCase = true)) {
                false
            } else {
                onDeactivateUser(user)
                true
            }
        }
    }

    override fun getItemCount(): Int =
        users.size

    fun updateList(
        newList: List<User>
    ) {
        users = newList
        notifyDataSetChanged()
    }

    // =========================================================
    // DATE FORMATTER
    // =========================================================

    private fun formatDate(
        createdAt: Long
    ): String {

        return try {

            val formatter =
                SimpleDateFormat(
                    "d MMM yyyy",
                    Locale.getDefault()
                )

            formatter.format(
                Date(createdAt)
            )

        } catch (_: Exception) {

            "Unknown"
        }
    }
}