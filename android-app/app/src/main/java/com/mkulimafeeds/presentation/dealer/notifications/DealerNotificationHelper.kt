package com.mkulimafeeds.presentation.dealer.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mkulimafeeds.R
import com.mkulimafeeds.domain.model.dealer.DealerOrder
import com.mkulimafeeds.domain.model.dealer.DealerOrderStatus
import java.util.Locale

object DealerNotificationHelper {

    private const val CHANNEL_ID =
        "dealer_order_updates"

    private const val CHANNEL_NAME =
        "Dealer Order Updates"

    private const val CHANNEL_DESCRIPTION =
        "Notifications about assigned dealer orders"

    fun showOrderStatusUpdated(
        context: Context,
        order: DealerOrder
    ) {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                return
            }
        }

        createNotificationChannel(context)

        val status =
            formatStatus(
                order.status
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    "Order #${order.id} updated"
                )
                .setContentText(
                    "Order #${order.id} is now $status."
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "Order #${order.id} for " +
                                    "${order.customerName} " +
                                    "is now $status."
                        )
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                order.id.toInt(),
                notification
            )
    }

    private fun createNotificationChannel(
        context: Context
    ) {

        if (
            android.os.Build.VERSION.SDK_INT <
            android.os.Build.VERSION_CODES.O
        ) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    CHANNEL_DESCRIPTION
            }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.createNotificationChannel(
            channel
        )
    }

    private fun formatStatus(
        status: DealerOrderStatus
    ): String {

        return status.name
            .lowercase(
                Locale.getDefault()
            )
            .replaceFirstChar {
                it.uppercase()
            }
    }
}