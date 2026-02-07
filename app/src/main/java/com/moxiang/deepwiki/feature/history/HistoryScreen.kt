package com.moxiang.deepwiki.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moxiang.deepwiki.core.ui.components.EmptyView
import com.moxiang.deepwiki.core.ui.theme.AccentBlue
import com.moxiang.deepwiki.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onRepositoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 示例历史记录数据 (分组)
    val historySections = remember {
        listOf(
            HistorySection(
                title = "Today",
                items = listOf(
                    HistoryItem("tensorflow/tensorflow", "An Open Source Machine Learning Framework for Everyone", 0.8f, "10:30 AM"),
                    HistoryItem("pytorch/pytorch", "Tensors and Dynamic neural networks in Python with strong GPU acceleration", 0.45f, "9:15 AM")
                )
            ),
            HistorySection(
                title = "Yesterday",
                items = listOf(
                    HistoryItem("huggingface/transformers", "State-of-the-art Machine Learning for Pytorch, TensorFlow, and JAX.", 0.2f, "Yesterday"),
                    HistoryItem("google/jax", "Composable transformations of Python+NumPy programs: differentiate, vectorize, JIT to GPU/TPU, and more", 0.1f, "Yesterday")
                )
            )
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Browse History",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF18181B) // Zinc 900
                        )
                    )

                    Text(
                        text = "Clear All",
                        modifier = Modifier.clickable { /* TODO: Clear History */ },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFEC4899) // Pink
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        if (historySections.isEmpty()) {
            EmptyView(
                icon = Icons.Filled.History,
                title = "暂无浏览历史",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                historySections.forEach { section ->
                    item {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF71717A) // Zinc 500
                            ),
                            modifier = Modifier.padding(bottom = 0.dp)
                        )
                    }

                    items(section.items) { item ->
                        HistoryCard(
                            fullName = item.fullName,
                            description = item.description,
                            progress = item.readProgress,
                            time = item.visitTime,
                            onClick = { onRepositoryClick(item.fullName) },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    fullName: String,
    description: String,
    progress: Float,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF4F4F5) // Zinc 100
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF18181B)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF71717A)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF52525B) // Zinc 600
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AccentBlue,
                    trackColor = Color(0xFFE4E4E7) // Zinc 200
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF71717A)
                    )
                )
            }
        }
    }
}

private data class HistorySection(
    val title: String,
    val items: List<HistoryItem>
)

private data class HistoryItem(
    val fullName: String,
    val description: String,
    val readProgress: Float,
    val visitTime: String
)
