package com.mkulimafeeds.account

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mkulimafeeds.R

/**
 * Renders a flat list of account menu items.
 * Section headers are inserted as distinct view types.
 */
class AccountMenuAdapter(
    private val items: List<AccountMenuItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MENU = 0
    }

    class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvLabel: TextView = view.findViewById(R.id.tvLabel)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_menu_row, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val menuHolder = holder as MenuViewHolder
        val item = items[position]

        menuHolder.tvLabel.text = item.label

        // Icon
        menuHolder.ivIcon.setImageResource(item.iconRes)
        val tint = item.tintColor
        if (tint != null) {
            menuHolder.ivIcon.setColorFilter(tint)
        }

        // Subtitle
        if (item.subtitle.isNullOrBlank()) {
            menuHolder.tvSubtitle.visibility = View.GONE
        } else {
            menuHolder.tvSubtitle.visibility = View.VISIBLE
            menuHolder.tvSubtitle.text = item.subtitle
        }

        // Badge
        if (item.badgeText.isNullOrBlank()) {
            menuHolder.tvBadge.visibility = View.GONE
        } else {
            menuHolder.tvBadge.visibility = View.VISIBLE
            menuHolder.tvBadge.text = item.badgeText
            val color = item.badgeColor ?: Color.parseColor("#E8F5E9")
            menuHolder.tvBadge.backgroundTintList = ColorStateList.valueOf(color)
        }

        // Click
        menuHolder.root.setOnClickListener { item.onTap() }
    }

    override fun getItemCount(): Int = items.size
}