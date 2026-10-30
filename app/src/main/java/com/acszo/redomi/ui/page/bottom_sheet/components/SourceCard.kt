package com.acszo.redomi.ui.page.bottom_sheet.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import com.acszo.redomi.R
import com.acszo.redomi.conversion.ConversionSource
import com.acszo.redomi.model.Platform.platforms
import com.acszo.redomi.parser.MusicContentType

/**
 * The sheet header. It only states what the link itself tells: the service it comes from and,
 * where known, the kind of content. There is no title, artist or cover because none is available.
 */
@Composable
fun SourceCard(
    source: ConversionSource,
    isUpdateAvailable: Boolean
) {
    val app = platforms[source.service.platformKey]!!
    val contentType = when (source.contentType) {
        MusicContentType.TRACK -> R.string.content_track
        MusicContentType.VIDEO -> R.string.content_video
        MusicContentType.ALBUM -> R.string.content_album
        MusicContentType.PLAYLIST -> R.string.content_playlist
        MusicContentType.ARTIST -> R.string.content_artist
        null -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = app.icon),
            modifier = Modifier
                .size(80.dp)
                .clip(MaterialTheme.shapes.medium),
            contentDescription = app.title
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = app.title,
                modifier = Modifier.basicMarquee(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall.copy(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    ),
                )
            )
            Text(
                text = contentType?.let { stringResource(id = it) } ?: "",
                modifier = Modifier.basicMarquee(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    ),
                ),
            )
        }

        SettingsButton(isUpdateAvailable = isUpdateAvailable)
    }
}
