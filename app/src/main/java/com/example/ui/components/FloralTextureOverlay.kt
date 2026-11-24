package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.FloralVariant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun FloralTextureOverlay(
  variant: FloralVariant,
  modifier: Modifier = Modifier,
  motifColor: Color = Color.White,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas
    val unit = min(w, h)

    when (variant) {
      FloralVariant.CORNER_CLUSTER -> {
        drawStemVine(
          start = Offset(w * 0.62f, h * 1.05f),
          control = Offset(w * 0.78f, h * 0.62f),
          end = Offset(w * 0.96f, h * 0.16f),
          color = motifColor.copy(alpha = 0.22f),
          strokeWidth = unit * 0.02f,
        )
        drawLeaf(Offset(w * 0.72f, h * 0.76f), unit * 0.18f, -38f, motifColor.copy(alpha = 0.18f))
        drawLeaf(Offset(w * 0.84f, h * 0.48f), unit * 0.16f, 24f, motifColor.copy(alpha = 0.18f))
        drawBlossom(Offset(w * 0.88f, h * 0.26f), unit * 0.28f, 5, 12f, motifColor.copy(alpha = 0.22f))
        drawBlossom(Offset(w * 0.70f, h * 0.86f), unit * 0.20f, 6, 30f, motifColor.copy(alpha = 0.16f))
        drawBlossom(Offset(w * 0.56f, h * 0.20f), unit * 0.11f, 5, 0f, motifColor.copy(alpha = 0.14f))
      }

      FloralVariant.SCATTERED_BLOSSOMS -> {
        drawBlossom(Offset(w * 0.16f, h * 0.22f), unit * 0.17f, 6, 15f, motifColor.copy(alpha = 0.16f))
        drawBlossom(Offset(w * 0.48f, h * 0.78f), unit * 0.19f, 5, 22f, motifColor.copy(alpha = 0.17f))
        drawBlossom(Offset(w * 0.78f, h * 0.26f), unit * 0.26f, 6, 8f, motifColor.copy(alpha = 0.22f))
        drawBlossom(Offset(w * 0.90f, h * 0.76f), unit * 0.14f, 5, 40f, motifColor.copy(alpha = 0.18f))
        drawLeaf(Offset(w * 0.34f, h * 0.24f), unit * 0.14f, 32f, motifColor.copy(alpha = 0.14f))
        drawLeaf(Offset(w * 0.68f, h * 0.62f), unit * 0.16f, -28f, motifColor.copy(alpha = 0.15f))
      }

      FloralVariant.BOTANICAL_VINE -> {
        drawStemVine(
          start = Offset(w * 0.04f, h * 0.88f),
          control = Offset(w * 0.44f, h * 0.36f),
          end = Offset(w * 0.96f, h * 0.14f),
          color = motifColor.copy(alpha = 0.20f),
          strokeWidth = unit * 0.018f,
        )
        drawLeaf(Offset(w * 0.24f, h * 0.64f), unit * 0.16f, -32f, motifColor.copy(alpha = 0.17f))
        drawLeaf(Offset(w * 0.52f, h * 0.44f), unit * 0.17f, 22f, motifColor.copy(alpha = 0.18f))
        drawLeaf(Offset(w * 0.76f, h * 0.28f), unit * 0.15f, -20f, motifColor.copy(alpha = 0.17f))
        drawBlossom(Offset(w * 0.84f, h * 0.22f), unit * 0.22f, 5, 18f, motifColor.copy(alpha = 0.21f))
        drawBlossom(Offset(w * 0.36f, h * 0.56f), unit * 0.14f, 6, 10f, motifColor.copy(alpha = 0.15f))
      }

      FloralVariant.RADIAL_PETALS -> {
        drawBlossom(Offset(w * 0.86f, h * 0.50f), unit * 0.42f, 8, 12f, motifColor.copy(alpha = 0.18f))
        drawBlossom(Offset(w * 0.86f, h * 0.50f), unit * 0.20f, 8, 34f, motifColor.copy(alpha = 0.20f))
        drawBlossom(Offset(w * 0.14f, h * 0.84f), unit * 0.18f, 6, 0f, motifColor.copy(alpha = 0.14f))
        drawCircle(
          color = motifColor.copy(alpha = 0.18f),
          radius = unit * 0.045f,
          center = Offset(w * 0.62f, h * 0.22f),
        )
      }

      FloralVariant.BLOOMING_ARC -> {
        drawStemVine(
          start = Offset(w * 0.52f, h * 0.98f),
          control = Offset(w * 0.68f, h * 0.76f),
          end = Offset(w * 0.96f, h * 0.68f),
          color = motifColor.copy(alpha = 0.22f),
          strokeWidth = unit * 0.018f,
        )
        drawBlossom(Offset(w * 0.70f, h * 0.78f), unit * 0.22f, 6, 15f, motifColor.copy(alpha = 0.20f))
        drawBlossom(Offset(w * 0.90f, h * 0.72f), unit * 0.17f, 5, 5f, motifColor.copy(alpha = 0.19f))
        drawBlossom(Offset(w * 0.20f, h * 0.18f), unit * 0.16f, 5, 25f, motifColor.copy(alpha = 0.15f))
        drawLeaf(Offset(w * 0.58f, h * 0.88f), unit * 0.15f, -45f, motifColor.copy(alpha = 0.16f))
      }

      FloralVariant.LEAF_GARLAND -> {
        val points = listOf(0.18f to 0.24f, 0.38f to 0.18f, 0.62f to 0.82f, 0.80f to 0.72f, 0.90f to 0.26f)
        points.forEachIndexed { idx, (px, py) ->
          drawLeaf(
            center = Offset(w * px, h * py),
            length = unit * (0.15f + (idx % 2) * 0.04f),
            angleDeg = if (idx % 2 == 0) -28f else 34f,
            color = motifColor.copy(alpha = 0.17f),
          )
        }
        drawBlossom(Offset(w * 0.84f, h * 0.36f), unit * 0.24f, 6, 20f, motifColor.copy(alpha = 0.20f))
      }
    }
  }
}

private fun DrawScope.drawBlossom(
  center: Offset,
  radius: Float,
  petalCount: Int,
  rotationOffsetDeg: Float,
  color: Color,
) {
  val petalLen = radius * 0.72f
  val petalWidth = radius * 0.40f
  for (i in 0 until petalCount) {
    val angleDeg = rotationOffsetDeg + (360f / petalCount) * i
    val rad = angleDeg * (PI / 180.0)
    val petalCenter =
      Offset(
        x = center.x + (cos(rad) * radius * 0.44f).toFloat(),
        y = center.y + (sin(rad) * radius * 0.44f).toFloat(),
      )
    rotate(degrees = angleDeg, pivot = petalCenter) {
      drawOval(
        color = color,
        topLeft = Offset(petalCenter.x - petalLen * 0.5f, petalCenter.y - petalWidth * 0.5f),
        size = Size(petalLen, petalWidth),
      )
    }
  }
  drawCircle(
    color = color.copy(alpha = (color.alpha * 1.4f).coerceAtMost(0.45f)),
    radius = radius * 0.20f,
    center = center,
  )
}

private fun DrawScope.drawLeaf(
  center: Offset,
  length: Float,
  angleDeg: Float,
  color: Color,
) {
  val width = length * 0.46f
  rotate(degrees = angleDeg, pivot = center) {
    val path =
      Path().apply {
        moveTo(center.x - length * 0.5f, center.y)
        quadraticTo(center.x, center.y - width, center.x + length * 0.5f, center.y)
        quadraticTo(center.x, center.y + width, center.x - length * 0.5f, center.y)
        close()
      }
    drawPath(path = path, color = color)
    drawLine(
      color = color.copy(alpha = (color.alpha * 1.3f).coerceAtMost(0.42f)),
      start = Offset(center.x - length * 0.42f, center.y),
      end = Offset(center.x + length * 0.42f, center.y),
      strokeWidth = length * 0.06f,
      cap = StrokeCap.Round,
    )
  }
}

private fun DrawScope.drawStemVine(
  start: Offset,
  control: Offset,
  end: Offset,
  color: Color,
  strokeWidth: Float,
) {
  val path =
    Path().apply {
      moveTo(start.x, start.y)
      quadraticTo(control.x, control.y, end.x, end.y)
    }
  drawPath(
    path = path,
    color = color,
    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
  )
}
