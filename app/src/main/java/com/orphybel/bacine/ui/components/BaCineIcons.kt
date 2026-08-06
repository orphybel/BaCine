package com.orphybel.bacine.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The handful of icons the app needs, drawn here rather than pulled from
 * `material-icons`. That artifact has churned across Compose releases and it is
 * not worth a dependency for six glyphs.
 *
 * Paths use a 24x24 viewport. The white fill is only the vector's own default —
 * callers tint via `Icon(tint = …)`.
 */
object BaCineIcons {

    private fun icon(
        name: String,
        fillType: PathFillType = PathFillType.NonZero,
        pathData: PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White), pathFillType = fillType, pathBuilder = pathData)
    }.build()

    val Home: ImageVector = icon("Home") {
        moveTo(12f, 3.2f); lineTo(2.5f, 11f); lineTo(4.3f, 13.1f); lineTo(5.5f, 12.1f)
        lineTo(5.5f, 20.5f); lineTo(10f, 20.5f); lineTo(10f, 14.8f); lineTo(14f, 14.8f)
        lineTo(14f, 20.5f); lineTo(18.5f, 20.5f); lineTo(18.5f, 12.1f); lineTo(19.7f, 13.1f)
        lineTo(21.5f, 11f); close()
    }

    val Grid: ImageVector = icon("Grid") {
        moveTo(3f, 3f); lineTo(10.2f, 3f); lineTo(10.2f, 10.2f); lineTo(3f, 10.2f); close()
        moveTo(13.8f, 3f); lineTo(21f, 3f); lineTo(21f, 10.2f); lineTo(13.8f, 10.2f); close()
        moveTo(3f, 13.8f); lineTo(10.2f, 13.8f); lineTo(10.2f, 21f); lineTo(3f, 21f); close()
        moveTo(13.8f, 13.8f); lineTo(21f, 13.8f); lineTo(21f, 21f); lineTo(13.8f, 21f); close()
    }

    // Even-odd so the inner circle punches a hole instead of filling the lens.
    val Search: ImageVector = icon("Search", PathFillType.EvenOdd) {
        moveTo(10.5f, 2.5f)
        arcTo(8f, 8f, 0f, true, true, 10.49f, 2.5f)
        close()
        moveTo(10.5f, 5.5f)
        arcTo(5f, 5f, 0f, true, true, 10.49f, 5.5f)
        close()
        moveTo(15.4f, 17.1f); lineTo(17.5f, 15f); lineTo(21.8f, 19.3f)
        lineTo(19.7f, 21.4f); close()
    }

    val Settings: ImageVector = icon("Settings", PathFillType.EvenOdd) {
        moveTo(10.3f, 1.6f); lineTo(13.7f, 1.6f); lineTo(14.2f, 4.4f)
        lineTo(16.1f, 5.2f); lineTo(18.4f, 3.8f); lineTo(20.8f, 6.2f)
        lineTo(19.4f, 8.5f); lineTo(20.2f, 10.4f); lineTo(23f, 10.9f)
        lineTo(23f, 14.3f); lineTo(20.2f, 14.8f); lineTo(19.4f, 16.7f)
        lineTo(20.8f, 19f); lineTo(18.4f, 21.4f); lineTo(16.1f, 20f)
        lineTo(14.2f, 20.8f); lineTo(13.7f, 23.6f); lineTo(10.3f, 23.6f)
        lineTo(9.8f, 20.8f); lineTo(7.9f, 20f); lineTo(5.6f, 21.4f)
        lineTo(3.2f, 19f); lineTo(4.6f, 16.7f); lineTo(3.8f, 14.8f)
        lineTo(1f, 14.3f); lineTo(1f, 10.9f); lineTo(3.8f, 10.4f)
        lineTo(4.6f, 8.5f); lineTo(3.2f, 6.2f); lineTo(5.6f, 3.8f)
        lineTo(7.9f, 5.2f); lineTo(9.8f, 4.4f); close()
        moveTo(12f, 8.2f)
        arcTo(3.8f, 3.8f, 0f, true, true, 11.99f, 8.2f)
        close()
    }

    val Play: ImageVector = icon("Play") {
        moveTo(7f, 4.5f); lineTo(19.5f, 12f); lineTo(7f, 19.5f); close()
    }

    val Star: ImageVector = icon("Star") {
        moveTo(12f, 2.6f); lineTo(14.9f, 8.7f); lineTo(21.4f, 9.6f)
        lineTo(16.7f, 14.3f); lineTo(17.8f, 21f); lineTo(12f, 17.8f)
        lineTo(6.2f, 21f); lineTo(7.3f, 14.3f); lineTo(2.6f, 9.6f)
        lineTo(9.1f, 8.7f); close()
    }

    val ChevronDown: ImageVector = icon("ChevronDown") {
        moveTo(6.4f, 8.6f); lineTo(12f, 14.2f); lineTo(17.6f, 8.6f)
        lineTo(19.7f, 10.7f); lineTo(12f, 18.4f); lineTo(4.3f, 10.7f); close()
    }

    // A bookmark ribbon, notched at the bottom — the "À voir" list.
    val Bookmark: ImageVector = icon("Bookmark") {
        moveTo(6f, 2.5f); lineTo(18f, 2.5f); lineTo(18f, 21.5f); lineTo(12f, 16.5f)
        lineTo(6f, 21.5f); close()
    }
}
