package com.acszo.redomi.ui.page.bottom_sheet.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.acszo.redomi.conversion.Destination
import com.acszo.redomi.conversion.DestinationKind
import com.acszo.redomi.data.ListOrientation
import com.acszo.redomi.model.Platform.platforms
import com.acszo.redomi.model.Song
import com.acszo.redomi.R
import com.acszo.redomi.ui.common.ClickableItem

/** What one tile shows: an icon, a name of up to two words and an optional small caption. */
private data class AppEntry(
    @DrawableRes val icon: Int,
    val title: String,
    val caption: String? = null
)

@Composable
fun AppsList(
    listOrientation: ListOrientation,
    songs: List<Song>,
    iconShape: Shape,
    gridSize: Int,
    onClick: (song: Song) -> Unit
) {
    EntriesList(
        listOrientation = listOrientation,
        entries = songs.map {
            val app = platforms[it.platform]!!
            AppEntry(icon = app.icon, title = app.title)
        },
        iconShape = iconShape,
        gridSize = gridSize,
        onClick = { onClick(songs[it]) }
    )
}

/** The conversion sheet's tiles: each destination with its service icon and what kind it is. */
@Composable
fun DestinationsList(
    listOrientation: ListOrientation,
    destinations: List<Destination>,
    iconShape: Shape,
    gridSize: Int,
    onClick: (destination: Destination) -> Unit
) {
    val musicLink = stringResource(id = R.string.musiclink)
    val captions = mapOf(
        DestinationKind.ORIGINAL to stringResource(id = R.string.destination_original),
        DestinationKind.SAME_ID to stringResource(id = R.string.destination_same_id),
        DestinationKind.RESOLVER to stringResource(id = R.string.destination_resolver)
    )

    EntriesList(
        listOrientation = listOrientation,
        entries = destinations.map {
            val app = it.service?.let { service -> platforms[service.platformKey] }
            AppEntry(
                icon = app?.icon ?: R.drawable.ic_musiclink,
                title = app?.title ?: musicLink,
                caption = captions[it.kind]
            )
        },
        iconShape = iconShape,
        gridSize = gridSize,
        onClick = { onClick(destinations[it]) }
    )
}

@Composable
private fun EntriesList(
    listOrientation: ListOrientation,
    entries: List<AppEntry>,
    iconShape: Shape,
    gridSize: Int,
    onClick: (index: Int) -> Unit
) {
    when (listOrientation) {
        ListOrientation.HORIZONTAL -> {
            LazyRow(
                modifier = Modifier
                    .padding(vertical = 15.dp)
                    .heightIn(min = 150.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                itemsIndexed(items = entries) { index, entry ->
                    AppItem(entry = entry, iconShape = iconShape, onClick = { onClick(index) })
                }
            }
        }

        ListOrientation.VERTICAL -> {
            LazyVerticalGrid(
                modifier = Modifier.padding(vertical = 15.dp),
                columns = GridCells.Fixed(gridSize),
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                gridItemsIndexed(items = entries) { index, entry ->
                    AppItem(entry = entry, iconShape = iconShape, onClick = { onClick(index) })
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    entry: AppEntry,
    iconShape: Shape,
    onClick: () -> Unit
) {
    val titleWords = entry.title.split(' ')

    ClickableItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = entry.icon),
            modifier = Modifier
                .size(80.dp)
                .padding(8.dp)
                .clip(iconShape),
            contentDescription = entry.title,
        )
        Text(text = titleWords[0])
        Text(text = if (titleWords.size > 1) titleWords[1] else "")
        entry.caption?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
