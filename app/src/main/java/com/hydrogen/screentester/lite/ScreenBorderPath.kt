package com.hydrogen.screentester.lite

import android.content.Context
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.RoundedCorner
import android.view.WindowInsets

/**
 * 屏幕边框路径的统一构建：测试页真实渲染与预览裁切共用。
 * lite 版只有旧版曲线：自定义半径（G2/椭圆角）+ 普通圆角矩形。
 *
 * @param scale 半径的缩放系数：1f = 真机原尺寸；预览按比例传入。
 *              ⚠️ 自定义半径的存储单位是 px，必须一起乘 scale。
 * @param forClip true = 生成线条外缘的路径（供预览裁切）：矩形不外缩、半径多半线宽，整条线才不被切一半。
 */
internal fun buildScreenBorderPath(
    widthPx: Float,
    heightPx: Float,
    insets: WindowInsets?,
    thickness: Float,
    context: Context,
    scale: Float = 1f,
    forClip: Boolean = false
): Path {
    val path = Path()
    if (widthPx <= 0f || heightPx <= 0f) return path

    val adj = thickness / 2f
    val inset = if (forClip) 0f else adj
    val rect = RectF(inset, inset, widthPx - inset, heightPx - inset)

    if (ThemeSettings.useCustomRadius) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isG2Enabled = prefs.getBoolean("is_g2_enabled", false)

        fun r(base: Float, fix: Float) =
            ((base + fix).coerceAtLeast(0f)) * scale + (if (forClip) adj else 0f)

        val tlX = r(ThemeSettings.radiusTL, ThemeSettings.radiusTLX)
        val tlY = r(ThemeSettings.radiusTL, ThemeSettings.radiusTLY)
        val trX = r(ThemeSettings.radiusTR, ThemeSettings.radiusTRX)
        val trY = r(ThemeSettings.radiusTR, ThemeSettings.radiusTRY)
        val brX = r(ThemeSettings.radiusBR, ThemeSettings.radiusBRX)
        val brY = r(ThemeSettings.radiusBR, ThemeSettings.radiusBRY)
        val blX = r(ThemeSettings.radiusBL, ThemeSettings.radiusBLX)
        val blY = r(ThemeSettings.radiusBL, ThemeSettings.radiusBLY)

        if (isG2Enabled) {
            // G2 平滑：三次贝塞尔
            val p = 1.4f
            val c = 0.45f
            path.moveTo(rect.left + p * tlX, rect.top)
            path.lineTo(rect.right - p * trX, rect.top)
            path.cubicTo(rect.right - c * trX, rect.top, rect.right, rect.top + c * trY, rect.right, rect.top + p * trY)
            path.lineTo(rect.right, rect.bottom - p * brY)
            path.cubicTo(rect.right, rect.bottom - c * brY, rect.right - c * brX, rect.bottom, rect.right - p * brX, rect.bottom)
            path.lineTo(rect.left + p * blX, rect.bottom)
            path.cubicTo(rect.left + c * blX, rect.bottom, rect.left, rect.bottom - c * blY, rect.left, rect.bottom - p * blY)
            path.lineTo(rect.left, rect.top + p * tlY)
            path.cubicTo(rect.left, rect.top + c * tlY, rect.left + c * tlX, rect.top, rect.left + p * tlX, rect.top)
            path.close()
        } else {
            // 普通圆角：四角半径 + X/Y 修正（椭圆角）
            path.addRoundRect(
                rect,
                floatArrayOf(tlX, tlY, trX, trY, brX, brY, blX, blY),
                Path.Direction.CW
            )
        }
    } else {
        // 旧版曲线：普通圆角矩形（Android 12+ 可读到系统圆角半径，低版本回退 100f）
        var systemR = 100f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && insets != null) {
            systemR = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat() ?: 100f
        }
        systemR *= scale
        if (forClip) systemR += adj
        path.addRoundRect(rect, systemR, systemR, Path.Direction.CW)
    }

    return path
}
