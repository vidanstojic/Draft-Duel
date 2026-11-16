package com.vidanstojic.draftduel.ui.components

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** Icons the material core set does not have, drawn on the same 24 × 24 grid. */
object DdIcons {

    /** Two vertical bars. */
    val Pause: ImageVector by lazy {
        materialIcon(name = "DdPause") {
            materialPath {
                moveTo(6f, 19f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                close()
                moveTo(14f, 5f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                close()
            }
        }
    }
}
