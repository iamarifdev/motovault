package com.motovault.app.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import dev.sky.design.SkySvgIcon

/** App-owned v3 artwork. Unmapped existing controls keep their Sky asset. */
@Composable
internal fun V3Icon(
    path: String,
    description: String?,
    modifier: Modifier = Modifier,
    tint: Color? = TextPrimary,
    strokeWidth: Float = 2f
) {
    val name = path.substringAfterLast('/')
    val paths = v3IconPaths[name]
    if (paths == null) {
        SkySvgIcon(path, description, modifier, tint)
        return
    }
    val vector = remember(name, strokeWidth) {
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { data ->
                addPath(
                    pathData = addPathNodes(data),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()
    }
    Icon(vector, description, modifier, tint ?: Color.Unspecified)
}
