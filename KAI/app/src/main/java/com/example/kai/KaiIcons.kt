package com.example.kai

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath

object KaiIcons {
    val Code = materialIcon("Filled.Code") {
        materialPath {
            moveTo(9.4f, 16.6f); lineTo(4.8f, 12.0f); lineTo(9.4f, 7.4f); lineTo(10.8f, 8.8f); lineTo(7.6f, 12.0f); lineTo(10.8f, 15.2f); close()
            moveTo(14.6f, 16.6f); lineTo(19.2f, 12.0f); lineTo(14.6f, 7.4f); lineTo(13.2f, 8.8f); lineTo(16.4f, 12.0f); lineTo(13.2f, 15.2f); close()
        }
    }

    val DeleteOutline = materialIcon("Outlined.DeleteOutline") {
        materialPath {
            moveTo(6.0f, 19.0f); curveTo(6.0f, 20.1f, 6.9f, 21.0f, 8.0f, 21.0f); horizontalLineTo(16.0f); curveTo(17.1f, 21.0f, 18.0f, 20.1f, 18.0f, 19.0f); verticalLineTo(7.0f); horizontalLineTo(6.0f); close()
            moveTo(8.0f, 9.0f); horizontalLineTo(16.0f); verticalLineTo(19.0f); horizontalLineTo(8.0f); close()
            moveTo(15.5f, 4.0f); horizontalLineTo(14.0f); lineTo(13.0f, 3.0f); horizontalLineTo(11.0f); lineTo(10.0f, 4.0f); horizontalLineTo(8.5f); curveTo(7.8f, 4.0f, 7.2f, 4.6f, 7.2f, 5.3f); verticalLineTo(6.0f); horizontalLineTo(16.8f); verticalLineTo(5.3f); curveTo(16.8f, 4.6f, 16.2f, 4.0f, 15.5f, 4.0f); close()
        }
    }

    val Apps = materialIcon("Filled.Apps") {
        materialPath {
            moveTo(4.0f, 8.0f); horizontalLineTo(8.0f); verticalLineTo(4.0f); horizontalLineTo(4.0f); close()
            moveTo(4.0f, 16.0f); horizontalLineTo(8.0f); verticalLineTo(12.0f); horizontalLineTo(4.0f); close()
            moveTo(12.0f, 8.0f); horizontalLineTo(16.0f); verticalLineTo(4.0f); horizontalLineTo(12.0f); close()
            moveTo(12.0f, 16.0f); horizontalLineTo(16.0f); verticalLineTo(12.0f); horizontalLineTo(12.0f); close()
        }
    }

    val AppsOutlined = materialIcon("Outlined.Apps") {
        materialPath {
            moveTo(4.0f, 8.0f); horizontalLineTo(8.0f); verticalLineTo(4.0f); horizontalLineTo(4.0f); close()
            moveTo(4.0f, 16.0f); horizontalLineTo(8.0f); verticalLineTo(12.0f); horizontalLineTo(4.0f); close()
            moveTo(12.0f, 8.0f); horizontalLineTo(16.0f); verticalLineTo(4.0f); horizontalLineTo(12.0f); close()
            moveTo(12.0f, 16.0f); horizontalLineTo(16.0f); verticalLineTo(12.0f); horizontalLineTo(12.0f); close()
        }
    }

    val Extension = materialIcon("Filled.Extension") {
        materialPath {
            moveTo(19.0f, 13.0f); curveTo(19.0f, 16.87f, 15.87f, 20.0f, 12.0f, 20.0f); curveTo(8.13f, 20.0f, 5.0f, 16.87f, 5.0f, 13.0f); curveTo(5.0f, 9.13f, 8.13f, 6.0f, 12.0f, 6.0f); curveTo(15.87f, 6.0f, 19.0f, 9.13f, 19.0f, 13.0f); close()
            moveTo(12.0f, 8.0f); curveTo(9.79f, 8.0f, 8.0f, 9.79f, 8.0f, 12.0f); curveTo(8.0f, 14.21f, 9.79f, 16.0f, 12.0f, 16.0f); curveTo(14.21f, 16.0f, 16.0f, 14.21f, 16.0f, 12.0f); curveTo(16.0f, 9.79f, 14.21f, 8.0f, 12.0f, 8.0f); close()
        }
    }

    val ExtensionOutlined = materialIcon("Outlined.Extension") {
        materialPath {
            moveTo(19.0f, 13.0f); curveTo(19.0f, 16.87f, 15.87f, 20.0f, 12.0f, 20.0f); curveTo(8.13f, 20.0f, 5.0f, 16.87f, 5.0f, 13.0f); curveTo(5.0f, 9.13f, 8.13f, 6.0f, 12.0f, 6.0f); curveTo(15.87f, 6.0f, 19.0f, 9.13f, 19.0f, 13.0f); close()
            moveTo(12.0f, 8.0f); curveTo(9.79f, 8.0f, 8.0f, 9.79f, 8.0f, 12.0f); curveTo(8.0f, 14.21f, 9.79f, 16.0f, 12.0f, 16.0f); curveTo(14.21f, 16.0f, 16.0f, 14.21f, 16.0f, 12.0f); curveTo(16.0f, 9.79f, 14.21f, 8.0f, 12.0f, 8.0f); close()
        }
    }

    // 新增：使用系统图标，无需自定义
    // Block, TextFields, ChevronRight, ArrowForward 使用 Icons.Default.*
}
