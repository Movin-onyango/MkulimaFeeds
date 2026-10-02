/*package com.mkulimafeeds

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView

object BottomNavHelper {

    private const val INACTIVE_COLOR = "#757575"
    private const val ACTIVE_COLOR = "#1B3A31"

    fun wire(
        activity: Activity,
        activeTab: Int
    ) {

        val tabs = mapOf(
            R.id.navHome to HomeActivity::class.java,
            R.id.navCatalog to CatalogActivity::class.java,
            R.id.navCart to CartActivity::class.java,
            R.id.navOrders to MyOrdersActivity::class.java,
            R.id.navAccount to AccountActivity::class.java
        )

        tabs.forEach { (tabId, destination) ->

            val tabView = activity.findViewById<View>(tabId)
                ?: return@forEach

            tabView.setOnClickListener {

                if (tabId == activeTab) {
                    return@setOnClickListener
                }

                activity.startActivity(
                    Intent(activity, destination).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                )

                activity.overridePendingTransition(0, 0)
            }
        }

        highlight(activity, activeTab)
    }

    private fun highlight(
        activity: Activity,
        activeTab: Int
    ) {

        listOf(
            R.id.navHome,
            R.id.navCatalog,
            R.id.navCart,
            R.id.navOrders,
            R.id.navAccount
        ).forEach { tabId ->
            applyTabState(
                activity = activity,
                tabId = tabId,
                isActive = (tabId == activeTab)
            )
        }
    }

    private fun applyTabState(
        activity: Activity,
        tabId: Int,
        isActive: Boolean
    ) {

        val iconResId: Int
        val labelResId: Int

        when (tabId) {

            R.id.navHome -> {
                iconResId = R.id.icHome
                labelResId = R.id.tvHome
            }

            R.id.navCatalog -> {
                iconResId = R.id.icCatalog
                labelResId = R.id.tvCatalog
            }

            R.id.navCart -> {
                iconResId = R.id.icCart
                labelResId = R.id.tvCart
            }

            R.id.navOrders -> {
                iconResId = R.id.icOrders
                labelResId = R.id.tvOrders
            }

            R.id.navAccount -> {
                iconResId = R.id.icAccount
                labelResId = R.id.tvAccount
            }

            else -> return
        }

        val color = Color.parseColor(
            if (isActive) ACTIVE_COLOR else INACTIVE_COLOR
        )

        activity.findViewById<ImageView>(iconResId)
            ?.setColorFilter(color)

        activity.findViewById<TextView>(labelResId)?.apply {
            setTextColor(color)
            setTypeface(
                null,
                if (isActive) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    fun setCartBadge(
        activity: Activity,
        count: Int
    ) {

        val badge = activity.findViewById<TextView>(
            R.id.tvCartBadge
        ) ?: return

        if (count > 0) {
            badge.text = if (count > 9) "9+" else count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }
}
*/
