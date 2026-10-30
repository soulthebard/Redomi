package com.acszo.redomi.model

import com.acszo.redomi.R

object Platform {

    val platforms = mapOf(
        "youtubeMusic" to AppDetails("Youtube Music", R.drawable.ic_youtube_music),
        "youtube" to AppDetails("Youtube", R.drawable.ic_youtube),
        "spotify" to AppDetails("Spotify", R.drawable.ic_spotify),
        "deezer" to AppDetails("Deezer", R.drawable.ic_deezer),
        "tidal" to AppDetails("Tidal", R.drawable.ic_tidal),
        "amazonMusic" to AppDetails("Amazon Music", R.drawable.ic_amazon_music),
        "appleMusic" to AppDetails("Apple Music", R.drawable.ic_apple_music),
        "soundcloud" to AppDetails("Soundcloud", R.drawable.ic_soundcloud),
        "napster" to AppDetails("Napster", R.drawable.ic_napster),
        "yandex" to AppDetails("Yandex Music", R.drawable.ic_yandex),
        "anghami" to AppDetails("Anghami", R.drawable.ic_anghami)
    )

}
