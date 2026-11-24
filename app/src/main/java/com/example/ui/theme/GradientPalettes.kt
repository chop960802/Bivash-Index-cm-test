package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

data class GradientPalette(
  val id: String,
  val name: String,
  val colors: List<Color>,
)

enum class FloralVariant {
  CORNER_CLUSTER,
  SCATTERED_BLOSSOMS,
  BOTANICAL_VINE,
  RADIAL_PETALS,
  BLOOMING_ARC,
  LEAF_GARLAND,
}

object GradientPaletteRegistry {
  val PALETTE_01 =
    GradientPalette("PALETTE_01", "Electric Sunset", listOf(Color(255, 59, 107), Color(255, 154, 0), Color(255, 230, 0)))
  val PALETTE_02 =
    GradientPalette("PALETTE_02", "Cyber Neon", listOf(Color(0, 245, 212), Color(155, 93, 229), Color(241, 91, 181)))
  val PALETTE_03 =
    GradientPalette("PALETTE_03", "Tropical Punch", listOf(Color(0, 187, 249), Color(0, 245, 212), Color(254, 228, 64)))
  val PALETTE_04 =
    GradientPalette("PALETTE_04", "Berry Pop", listOf(Color(255, 0, 127), Color(121, 40, 202), Color(255, 126, 179)))
  val PALETTE_05 =
    GradientPalette("PALETTE_05", "Citrus Splash", listOf(Color(128, 237, 153), Color(87, 204, 153), Color(255, 190, 11)))
  val PALETTE_06 =
    GradientPalette("PALETTE_06", "Sunset Lagoon", listOf(Color(255, 107, 107), Color(78, 205, 196), Color(255, 230, 109)))
  val PALETTE_07 =
    GradientPalette("PALETTE_07", "Hyper Violet", listOf(Color(109, 40, 217), Color(215, 255, 0), Color(255, 42, 133)))
  val PALETTE_08 =
    GradientPalette("PALETTE_08", "Coral Reef", listOf(Color(255, 90, 95), Color(0, 166, 153), Color(252, 100, 45)))
  val PALETTE_09 =
    GradientPalette("PALETTE_09", "Candy Arcade", listOf(Color(255, 77, 109), Color(89, 13, 34), Color(0, 245, 212)))
  val PALETTE_10 =
    GradientPalette("PALETTE_10", "Solar Flare", listOf(Color(255, 84, 0), Color(255, 189, 0), Color(158, 0, 89)))
  val PALETTE_11 =
    GradientPalette("PALETTE_11", "Minty Electric", listOf(Color(6, 214, 160), Color(17, 138, 178), Color(255, 209, 102)))
  val PALETTE_12 =
    GradientPalette("PALETTE_12", "Magenta Pulse", listOf(Color(247, 37, 133), Color(114, 9, 183), Color(76, 201, 240)))
  val PALETTE_13 =
    GradientPalette("PALETTE_13", "Mango Tango", listOf(Color(255, 159, 28), Color(255, 191, 105), Color(46, 196, 182)))
  val PALETTE_14 =
    GradientPalette("PALETTE_14", "Neon Jungle", listOf(Color(1, 255, 31), Color(0, 120, 255), Color(227, 255, 0)))
  val PALETTE_15 =
    GradientPalette("PALETTE_15", "Bubblegum Sky", listOf(Color(255, 112, 166), Color(255, 151, 112), Color(112, 214, 255)))
  val PALETTE_16 =
    GradientPalette("PALETTE_16", "Laser Lime", listOf(Color(170, 204, 0), Color(85, 166, 48), Color(0, 180, 216)))
  val PALETTE_17 =
    GradientPalette("PALETTE_17", "Velvet Crush", listOf(Color(181, 23, 158), Color(72, 12, 168), Color(67, 97, 238)))
  val PALETTE_18 =
    GradientPalette("PALETTE_18", "Peach Sorbet", listOf(Color(255, 140, 97), Color(255, 209, 102), Color(239, 71, 111)))
  val PALETTE_19 =
    GradientPalette("PALETTE_19", "Aqua Spark", listOf(Color(0, 180, 219), Color(0, 131, 176), Color(255, 107, 107)))
  val PALETTE_20 =
    GradientPalette("PALETTE_20", "Grape Soda", listOf(Color(131, 56, 236), Color(255, 0, 110), Color(255, 190, 11)))
  val PALETTE_21 =
    GradientPalette("PALETTE_21", "Tangerine Rush", listOf(Color(251, 86, 7), Color(255, 0, 110), Color(255, 190, 11)))
  val PALETTE_22 =
    GradientPalette("PALETTE_22", "Emerald Glow", listOf(Color(0, 200, 150), Color(0, 119, 182), Color(144, 224, 239)))
  val PALETTE_23 =
    GradientPalette("PALETTE_23", "Fuchsia Blaze", listOf(Color(255, 0, 85), Color(189, 0, 255), Color(0, 229, 255)))
  val PALETTE_24 =
    GradientPalette("PALETTE_24", "Sunny Meadow", listOf(Color(255, 214, 10), Color(56, 176, 0), Color(0, 119, 182)))
  val PALETTE_25 =
    GradientPalette("PALETTE_25", "Crimson Electric", listOf(Color(230, 57, 70), Color(247, 127, 0), Color(252, 191, 73)))
  val PALETTE_26 =
    GradientPalette("PALETTE_26", "Cobalt Pop", listOf(Color(0, 120, 255), Color(0, 229, 255), Color(255, 154, 0)))
  val PALETTE_27 =
    GradientPalette("PALETTE_27", "Plum Fiesta", listOf(Color(157, 78, 221), Color(255, 84, 0), Color(255, 189, 0)))
  val PALETTE_28 =
    GradientPalette("PALETTE_28", "Kiwi Fizz", listOf(Color(112, 224, 0), Color(56, 176, 0), Color(255, 77, 109)))
  val PALETTE_29 =
    GradientPalette("PALETTE_29", "Ultramarine Vibe", listOf(Color(58, 12, 163), Color(76, 201, 240), Color(247, 37, 133)))
  val PALETTE_30 =
    GradientPalette("PALETTE_30", "Papaya Burst", listOf(Color(255, 123, 0), Color(255, 170, 0), Color(0, 187, 249)))

  val all: List<GradientPalette> =
    listOf(
      PALETTE_01,
      PALETTE_02,
      PALETTE_03,
      PALETTE_04,
      PALETTE_05,
      PALETTE_06,
      PALETTE_07,
      PALETTE_08,
      PALETTE_09,
      PALETTE_10,
      PALETTE_11,
      PALETTE_12,
      PALETTE_13,
      PALETTE_14,
      PALETTE_15,
      PALETTE_16,
      PALETTE_17,
      PALETTE_18,
      PALETTE_19,
      PALETTE_20,
      PALETTE_21,
      PALETTE_22,
      PALETTE_23,
      PALETTE_24,
      PALETTE_25,
      PALETTE_26,
      PALETTE_27,
      PALETTE_28,
      PALETTE_29,
      PALETTE_30,
    )

  /**
   * Assigns a distinct 3-stop palette per card slot.
   * Uses coprime stride 7 over 30 palettes so indices 0..29 never collide,
   * while remaining deterministic for a given card index/key.
   */
  fun paletteForCard(cardKey: String, index: Int): GradientPalette {
    val safeIndex = abs(index)
    val slot = if (safeIndex < all.size) (safeIndex * 7 + 11) % all.size else abs(cardKey.hashCode()) % all.size
    return all[slot]
  }

  /**
   * Assigns a distinct botanical floral texture variant per card index.
   */
  fun floralVariantForCard(cardKey: String, index: Int): FloralVariant {
    val variants = FloralVariant.entries
    val safeIndex = abs(index)
    val slot =
      if (safeIndex < variants.size) safeIndex
      else (safeIndex + abs(cardKey.hashCode())) % variants.size
    return variants[slot]
  }
}
