package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPalette
import com.example.ui.theme.GradientPaletteRegistry
import com.example.ui.theme.MonoLabelStyle

data class DashboardCardSpec(
  val id: String,
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val testTag: String,
  val onClick: () -> Unit,
)

@Composable
fun DashboardScreen(
  onOpenAttendance: () -> Unit,
  onOpenOfficeIndex: () -> Unit,
  onOpenIctData: () -> Unit,
  onOpenTaskManager: () -> Unit,
  onOpenNotes: () -> Unit,
  onOpenUniversalSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cards =
    listOf(
      DashboardCardSpec(
        id = "attendance",
        title = "Attendance",
        subtitle = "Daily register, shifts & history",
        icon = Icons.AutoMirrored.Outlined.FactCheck,
        testTag = "dashboard_attendance_tile",
        onClick = onOpenAttendance,
      ),
      DashboardCardSpec(
        id = "office_index",
        title = "Office Index",
        subtitle = "FIR log, BNS/POCSO acts & I/O history",
        icon = Icons.Outlined.Inventory2,
        testTag = "dashboard_office_index_tile",
        onClick = onOpenOfficeIndex,
      ),
      DashboardCardSpec(
        id = "ict_data",
        title = "ICT Data",
        subtitle = "Categories, sub-categories & value logs",
        icon = Icons.Outlined.Analytics,
        testTag = "dashboard_ict_data_tile",
        onClick = onOpenIctData,
      ),
      DashboardCardSpec(
        id = "task_manager",
        title = "Task Manager",
        subtitle = "Categories, checklists & bulk comma add",
        icon = Icons.Outlined.Checklist,
        testTag = "dashboard_task_manager_tile",
        onClick = onOpenTaskManager,
      ),
      DashboardCardSpec(
        id = "notes",
        title = "Notes",
        subtitle = "Quick notes, search & field memos",
        icon = Icons.Outlined.EditNote,
        testTag = "dashboard_notes_tile",
        onClick = onOpenNotes,
      ),
    )

  Box(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 300.dp),
      modifier = Modifier.fillMaxSize().widthIn(max = 880.dp),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        DashboardHeader(onOpenUniversalSearch = onOpenUniversalSearch)
      }

      itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
        val palette = GradientPaletteRegistry.paletteForCard(card.id, index)
        val floralVariant = GradientPaletteRegistry.floralVariantForCard(card.id, index)
        ModularDashboardCard(
          spec = card,
          palette = palette,
          floralVariant = floralVariant,
        )
      }
    }
  }
}

@Composable
private fun DashboardHeader(
  onOpenUniversalSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(bottom = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = "Bivash Index",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "Offline Operations Workspace",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "BI",
            style = MonoLabelStyle.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenUniversalSearch)
            .testTag("dashboard_universal_search_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Universal Search",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Search",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
fun ModularDashboardCard(
  spec: DashboardCardSpec,
  palette: GradientPalette,
  floralVariant: FloralVariant,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(20.dp)
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = 124.dp)
        .shadow(elevation = 6.dp, shape = shape, clip = false)
        .clip(shape)
        .clickable(onClick = spec.onClick)
        .testTag(spec.testTag),
    shape = shape,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            brush =
              Brush.linearGradient(
                colors = palette.colors,
                start = Offset.Zero,
                end = Offset(900f, 450f),
              )
          ),
    ) {
      FloralTextureOverlay(
        variant = floralVariant,
        motifColor = Color.White,
        modifier = Modifier.matchParentSize(),
      )

      // Subtle directional contrast scrim so titles & subtitles remain crisp on all 30 vibrant palettes
      Box(
        modifier =
          Modifier.matchParentSize()
            .background(
              brush =
                Brush.horizontalGradient(
                  colors =
                    listOf(
                      Color(0x9E0B1020),
                      Color(0x5C0B1020),
                      Color(0x1F0B1020),
                    )
                )
            )
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = spec.title,
            style =
              MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(color = Color(0x66000000), offset = Offset(0f, 1.5f), blurRadius = 4f),
              ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = spec.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0x470B1020),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
          modifier = Modifier.size(56.dp),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
              imageVector = spec.icon,
              contentDescription = spec.title,
              tint = Color.White,
              modifier = Modifier.size(28.dp),
            )
          }
        }
      }
    }
  }
}
