package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AddProductActivity :
    AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        startActivity(
            Intent(
                this,
                AdminEditProductActivity::class.java
            ).apply {
                putExtra(
                    "IS_EDIT",
                    false
                )
            }
        )

        finish()
    }
}