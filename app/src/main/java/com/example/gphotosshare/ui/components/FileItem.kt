package com.example.gphotosshare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.example.gphotosshare.data.FileModel

@Composable
fun FileListItem(
    file: FileModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (file.isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileThumbnail(file = file, modifier = Modifier.size(48.dp))
        
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!file.isDirectory) {
            Checkbox(
                checked = file.isSelected,
                onCheckedChange = { onClick() }
            )
        }
    }
}

@Composable
fun FileGridItem(
    file: FileModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable { onClick() }
            .border(
                width = if (file.isSelected) 3.dp else 0.dp,
                color = if (file.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                   FileThumbnail(file = file, modifier = Modifier.fillMaxSize())
                }
                
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(6.dp)
                )
            }
             if (file.isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
fun FileThumbnail(
    file: FileModel,
    modifier: Modifier = Modifier
) {
    if (file.isDirectory) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
             Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Directory",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    } else {
        val context = LocalContext.current
        val isVideo = file.extension in setOf("mp4", "mkv", "webm", "avi")
        
        val fallbackPainter = androidx.compose.ui.graphics.vector.rememberVectorPainter(
            image = if (isVideo) Icons.Default.VideoFile else Icons.Default.InsertDriveFile
        )

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file.file)
                .decoderFactory { result, options, _ ->
                     if (isVideo) VideoFrameDecoder(result.source, options) else null
                }
                .crossfade(true)
                .build(),
            contentDescription = "Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            error = fallbackPainter,
            placeholder = fallbackPainter,
            fallback = fallbackPainter
        )
    }
}
