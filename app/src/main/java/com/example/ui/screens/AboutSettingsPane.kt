package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.FloralTextureOverlay
import com.example.ui.components.GradientAccentStrip
import com.example.ui.components.TintedPillTag
import com.example.ui.theme.FloralVariant
import com.example.ui.theme.GradientPaletteRegistry
import com.example.ui.theme.MonoLabelStyle

@Composable
fun AboutSettingsPane(
  modifier: Modifier = Modifier,
) {
  val appName = stringResource(id = R.string.app_name)
  val aboutPalette = GradientPaletteRegistry.paletteForCard("ABOUT", 9)
  val primaryAccent = aboutPalette.colors.first()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Surface(
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
          modifier = Modifier.fillMaxWidth().testTag("about_hero_card"),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  Brush.linearGradient(
                    colors = aboutPalette.colors,
                    start = Offset.Zero,
                    end = Offset(900f, 420f),
                  )
                )
          ) {
            FloralTextureOverlay(
              variant = FloralVariant.RADIAL_PETALS,
              motifColor = Color.White,
              modifier = Modifier.matchParentSize(),
            )
            Box(
              modifier =
                Modifier.matchParentSize()
                  .background(
                    Brush.horizontalGradient(
                      colors = listOf(Color(0xB30B1020), Color(0x6E0B1020), Color(0x360B1020))
                    )
                  )
            )
            Row(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x470B1020),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                modifier = Modifier.size(58.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "BI",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                  )
                }
              }
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = appName,
                  style =
                    MaterialTheme.typography.headlineSmall.copy(
                      fontWeight = FontWeight.Bold,
                      shadow =
                        Shadow(
                          color = Color(0x66000000),
                          offset = Offset(0f, 1.5f),
                          blurRadius = 3f,
                        ),
                    ),
                  color = Color.White,
                )
                Text(
                  text = "Offline Operations Workspace · APK Release Info",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.92f),
                )
                Surface(
                  shape = RoundedCornerShape(50),
                  color = Color(0x520B1020),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f)),
                ) {
                  Text(
                    text = "VERSION ${BuildConfig.APP_BUILD_DATE_VERSION}",
                    style = MonoLabelStyle.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                  )
                }
              }
            }
          }
        }
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.42f)),
          modifier = Modifier.fillMaxWidth().testTag("about_details_card"),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            GradientAccentStrip(palette = aboutPalette, height = 4.dp)
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              AboutInfoRow(
                icon = Icons.Outlined.Info,
                label = "App Name",
                value = appName,
                accentColor = aboutPalette.colors[0],
                tag = "about_app_name",
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AboutInfoRow(
                icon = Icons.Outlined.Person,
                label = "Developer & Designer",
                value = "Developed and designed by Bivash Chandra Mali",
                accentColor = aboutPalette.colors[1],
                tag = "about_developer",
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AboutInfoRow(
                icon = Icons.Outlined.Key,
                label = "API Key",
                value = "Chop",
                accentColor = aboutPalette.colors[2],
                pillBadge = "VERIFIED",
                tag = "about_api_key",
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AboutInfoRow(
                icon = Icons.Outlined.Verified,
                label = "Version (Build Date)",
                value = BuildConfig.APP_BUILD_DATE_VERSION,
                accentColor = aboutPalette.colors[0],
                pillBadge = "DD. Mon. YYYY",
                tag = "about_version",
              )
            }
          }
        }
      }

      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Code,
              contentDescription = null,
              tint = primaryAccent,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text =
                "Version date is generated automatically at Android Studio APK build time in DD. Mon. YYYY format.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AboutInfoRow(
  icon: ImageVector,
  label: String,
  value: String,
  accentColor: Color,
  tag: String,
  pillBadge: String? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth().testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = accentColor.copy(alpha = 0.14f),
      border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    if (!pillBadge.isNullOrBlank()) {
      TintedPillTag(text = pillBadge, accentColor = accentColor)
    }
  }
}
