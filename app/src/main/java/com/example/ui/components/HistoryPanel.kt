package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ModelEntity
import com.example.ui.theme.MinimalAccentContainer
import com.example.ui.theme.MinimalBg
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalDark
import com.example.ui.theme.MinimalGrayText
import com.example.ui.theme.MinimalLightGray
import com.example.ui.theme.MinimalPrimary
import com.example.ui.theme.MinimalSecondaryBg
import com.example.ui.theme.MinimalText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryPanel(
    historicalModels: List<ModelEntity>,
    selectedModel: ModelEntity?,
    onModelSelect: (ModelEntity) -> Unit,
    onModelDelete: (ModelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MinimalSecondaryBg)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Scan History",
                tint = MinimalPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "扫描历史记录 (${historicalModels.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MinimalText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (historicalModels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "无历史记录，开始拍照或导入照片吧！",
                    color = MinimalLightGray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_list"),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historicalModels, key = { it.id }) { model ->
                    val isSelected = selectedModel?.id == model.id
                    HistoryItem(
                        model = model,
                        isSelected = isSelected,
                        onSelect = { onModelSelect(model) },
                        onDelete = { onModelDelete(model) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    model: ModelEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val dateString = sdf.format(Date(model.timestamp))

    Card(
        modifier = Modifier
            .width(170.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.fitBorderWidth(isSelected),
                color = if (isSelected) MinimalPrimary else MinimalBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() }
            .testTag("history_item_${model.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MinimalAccentContainer else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Picture thumbnail using Coil
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(model.imagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = model.title,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, topStart = 16.dp))
                    .background(MinimalSecondaryBg),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = model.title,
                    color = MinimalDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateString,
                    color = MinimalGrayText,
                    fontSize = 10.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "深度: ${(model.depthScale * 10f).toInt() / 10f}",
                    color = MinimalLightGray,
                    fontSize = 9.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(28.dp)
                    .testTag("delete_button_${model.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFBA1A1A).copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun Int.fitBorderWidth(isSelected: Boolean) = if (isSelected) 1.5.dp else 1.dp
