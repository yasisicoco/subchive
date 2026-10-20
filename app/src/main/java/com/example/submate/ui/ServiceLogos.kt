package com.example.submate.ui

import androidx.annotation.DrawableRes
import com.example.submate.R

object ServiceLogos {
    @DrawableRes
    fun resource(key: String): Int? = when (key) {
        "netflix" -> R.drawable.service_logo_netflix
        "youtube" -> R.drawable.service_logo_youtube
        "disney" -> R.drawable.service_logo_disney
        "tving" -> R.drawable.service_logo_tving
        "wavve" -> R.drawable.service_logo_wavve
        "coupangplay" -> R.drawable.service_logo_coupangplay
        "spotify" -> R.drawable.service_logo_spotify
        "applemusic" -> R.drawable.service_logo_applemusic
        "melon" -> R.drawable.service_logo_melon
        "chatgpt" -> R.drawable.service_logo_chatgpt
        "claude" -> R.drawable.service_logo_claude
        "notion" -> R.drawable.service_logo_notion
        "microsoft" -> R.drawable.service_logo_microsoft
        "icloud" -> R.drawable.service_logo_icloud
        "googleone" -> R.drawable.service_logo_googleone
        "coupang" -> R.drawable.service_logo_coupang
        "naver" -> R.drawable.service_logo_naver
        "gamepass" -> R.drawable.service_logo_gamepass
        else -> null
    }
}
