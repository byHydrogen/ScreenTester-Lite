package com.hydrogen.screentester.lite

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class DarkModeConfig { FOLLOW_SYSTEM, LIGHT, DARK }

// 预设方案枚举
enum class PresetScheme {
    RAINBOW,      // 彩虹色
    WARM,         // 暖色
    COOL,         // 冷色
    HIGH_CONTRAST, // 高对比
    BLUE_PINK,    // 蓝粉
    OCEAN         // 海洋
}

object ThemeSettings {
    var darkModeState by mutableStateOf(DarkModeConfig.FOLLOW_SYSTEM)
    var testLineColor by mutableIntStateOf(android.graphics.Color.WHITE)

    // 线条测试模式的自定义背景色（默认黑，与旧行为一致）
    var testLineBgColor by mutableIntStateOf(android.graphics.Color.BLACK)
    // 线条测试模式的文字颜色（默认白）
    var testLineTextColor by mutableIntStateOf(android.graphics.Color.WHITE)
    // 全局：所有文字是否跟随线条颜色（默认 false —— 默认使用自定义字体色）
    var textFollowsLine by mutableStateOf(false)
    // "黑边遮挡测试"那一行单独控制：true = 跟随字体色，false = 跟随线条色
    var titleFollowsText by mutableStateOf(false)
    // 精度模式是否也使用自定义背景色（默认 false）
    var precisionUsesCustomBg by mutableStateOf(false)
    // 测试页"机型"文字不透明度（%）：100 = 原始，范围 40~120
    var deviceNameOpacityPct by mutableIntStateOf(100)

    var isMaxBrightnessEnabled by mutableStateOf(false)
    var testBrightnessValue by mutableFloatStateOf(1.0f)

    // 测试页屏幕常亮开关
    var isKeepScreenOnEnabled by mutableStateOf(false)
    var userPresets by mutableStateOf<List<Int>>(emptyList())
    // 背景色卡片的独立用户预设（与线条色分开）
    var bgUserPresets by mutableStateOf<List<Int>>(emptyList())
    // 背景预设里被用户长按删除的内置颜色：删除后不再显示，重新「保存为预设」即恢复
    var bgRemovedPresets by mutableStateOf<Set<Int>>(emptySet())
    // 背景预设的 12 个内置色（面板显示与「是否内置」判断的唯一来源）
    val defaultBgPresets = listOf(
        -16777216, // 黑
        -13619152, // #303030
        -2565928,  // #D8D8D8
        -1,        // 白
        -9263105,  // 蓝
        -16711936, // 绿
        -7981735,  // 梅
        -11556712, // 松石青
        -7952269,  // 苔绿
        -2509733,  // 芥末黄
        -1533306,  // 珊瑚
        -5994791   // 藤紫
    )
    var useCustomRadius by mutableStateOf(false)
    var radiusTL by mutableFloatStateOf(-1f)
    var radiusTR by mutableFloatStateOf(-1f)
    var radiusBL by mutableFloatStateOf(-1f)
    var radiusBR by mutableFloatStateOf(-1f)

    // 圆角校准页：四角 X/Y 曲率修正值。继续使用原有 SharedPreferences key，兼容已有用户数据。
    var radiusTLX by mutableFloatStateOf(0f)
    var radiusTRX by mutableFloatStateOf(0f)
    var radiusBLX by mutableFloatStateOf(0f)
    var radiusBRX by mutableFloatStateOf(0f)
    var radiusTLY by mutableFloatStateOf(0f)
    var radiusTRY by mutableFloatStateOf(0f)
    var radiusBLY by mutableFloatStateOf(0f)
    var radiusBRY by mutableFloatStateOf(0f)

    // 圆角校准页：是否记忆上次使用的拖拽调整模式
    var isDragAdjustModeEnabled by mutableStateOf(false)
    var isCalibrationLinked by mutableStateOf(true)

    // 线条粗细存储，默认 5.0 像素
    var testLineThickness by mutableFloatStateOf(5f)

    var isAnimationEnabled by mutableStateOf(true)

    // 关于页动态混色开关（默认关闭；开启后关于页等界面背景色缓慢流动混合，关闭保持静态渐变）
    var aboutDynamicMixEnabled by mutableStateOf(false)

    // 设置页线条预览开关
    var isSettingsLinePreviewEnabled by mutableStateOf(true)

    // 精简黑边遮挡测试页文字开关状态
    var isCompactModeEnabled by mutableStateOf(false)

    // 黑边遮挡测试长按退出：开关 + 秒数（3-20，默认5）
    var longPressExitEnabled by mutableStateOf(true)
    var longPressExitSeconds by mutableIntStateOf(5)

    // 主页测试项视图模式：true=网格模式，false=列表模式
    var isGridView by mutableStateOf(false)

    // 渐变色条模式状态
    var isMultiColorMode by mutableStateOf(false)
    var multiColorSelectedColors by mutableStateOf<List<Int>>(emptyList()) // 勾选的颜色（最多8个）
    var multiColorSegmentLength by mutableFloatStateOf(0f) // 渐变颜色长度（0表示使用默认中间值）

    // 更新下载源
    var updateDownloadSource by mutableStateOf("gitee")

    // 自动检查更新开关（默认开启，关闭后只能手动检查）
    var autoCheckUpdateEnabled by mutableStateOf(true)

    fun saveUpdateSource(context: Context, source: String) {
        if (updateDownloadSource == source) return
        updateDownloadSource = source
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("update_source", source).apply()
        GlobalUpdateState.downloadState.cancel(context)
        GlobalUpdateState.latestDownloadUrl = null
        UpdateManager.checkUpdate(context, isManual = true,
            onResult = { has, ver, log, dlUrl ->
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                if (has && ver != null && UpdateManager.isVersionGreater(ver, pInfo.versionName ?: "")) {
                    GlobalUpdateState.hasNewVersion = true
                    GlobalUpdateState.latestVersionName = ver
                    GlobalUpdateState.latestChangelog = log ?: ""
                    GlobalUpdateState.latestDownloadUrl = dlUrl
                } else { GlobalUpdateState.hasNewVersion = false }
            }
        )
    }

    fun saveConfig(context: Context, config: DarkModeConfig) {
        darkModeState = config
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("dark_mode", config.name).apply()
    }

    // 保存自动检查更新开关
    fun saveAutoCheckUpdate(context: Context, enabled: Boolean) {
        autoCheckUpdateEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("auto_check_update_enabled", enabled).apply()
    }

    fun saveLineColor(context: Context, color: Int) {
        testLineColor = color
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("line_color", color).apply()
    }

    // 保存线条测试模式的自定义背景色
    fun saveLineBgColor(context: Context, color: Int) {
        testLineBgColor = color
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("test_line_bg_color", color).apply()
    }

    // 保存线条测试模式的自定义文字颜色
    fun saveLineTextColor(context: Context, color: Int) {
        testLineTextColor = color
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("test_line_text_color", color).apply()
    }

    // 全局：所有文字是否跟随线条颜色
    fun saveTextFollowsLine(context: Context, enabled: Boolean) {
        textFollowsLine = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("test_text_follows_line", enabled).apply()
    }

    // "黑边遮挡测试"那行字单独控制：true = 跟随字体色
    fun saveTitleFollowsText(context: Context, enabled: Boolean) {
        titleFollowsText = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("title_follows_text", enabled).apply()
    }

    // 精度模式是否也使用自定义背景色
    fun savePrecisionUsesCustomBg(context: Context, enabled: Boolean) {
        precisionUsesCustomBg = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("test_precision_uses_custom_bg", enabled).apply()
    }

    // 测试页"机型"文字不透明度（%）
    fun saveDeviceNameOpacityPct(context: Context, pct: Int) {
        deviceNameOpacityPct = pct
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("device_name_opacity_pct", pct).apply()
    }

    fun saveMaxBrightness(context: Context, enabled: Boolean) {
        isMaxBrightnessEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("max_brightness", enabled).apply()
    }

    fun saveTestBrightnessValue(context: Context, value: Float) {
        testBrightnessValue = value
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putFloat("test_brightness_val", value).apply()
    }

    // 保存测试页屏幕常亮开关
    fun saveKeepScreenOn(context: Context, enabled: Boolean) {
        isKeepScreenOnEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("keep_screen_on_enabled", enabled).apply()
    }

    // 保存线条粗细
    fun saveLineThickness(context: Context, value: Float) {
        testLineThickness = value.coerceIn(1f, 15f)
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putFloat("line_thickness", testLineThickness).apply()
    }

    fun saveAnimationConfig(context: Context, enabled: Boolean) {
        isAnimationEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("is_animation_enabled", enabled).apply()
    }

    // 保存关于页动态混色开关
    fun saveAboutDynamicMixConfig(context: Context, enabled: Boolean) {
        aboutDynamicMixEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("about_dynamic_mix_enabled", enabled).apply()
    }

    fun saveSettingsLinePreviewConfig(context: Context, enabled: Boolean) {
        isSettingsLinePreviewEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("settings_line_preview", enabled).apply()
    }

    // 保存精简模式设置
    fun saveCompactModeConfig(context: Context, enabled: Boolean) {
        isCompactModeEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("is_compact_mode_enabled", enabled).apply()
    }

    // 保存黑边遮挡测试长按退出设置
    fun saveLongPressExitConfig(context: Context, enabled: Boolean) {
        longPressExitEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("long_press_exit_enabled", enabled).apply()
    }

    fun saveLongPressExitSeconds(context: Context, seconds: Int) {
        longPressExitSeconds = seconds.coerceIn(3, 20)
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("long_press_exit_seconds", seconds.coerceIn(3, 20)).apply()
    }

    // 保存视图模式设置
    fun saveGridViewConfig(context: Context, enabled: Boolean) {
        isGridView = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("is_grid_view", enabled).apply()
    }

    // 保存渐变色条模式开关
    fun saveMultiColorMode(context: Context, enabled: Boolean) {
        isMultiColorMode = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("is_multi_color_mode", enabled).apply()
    }

    // 保存渐变色条模式勾选的颜色
    fun saveMultiColorSelectedColors(context: Context, colors: List<Int>) {
        multiColorSelectedColors = colors
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putString("multi_color_selected", colors.joinToString(","))
            .apply()
    }

    // 存储用户自定义的渐变方案
    var customGradientSchemes by mutableStateOf<List<Pair<String, List<Int>>>>(emptyList())

    // 保存自定义渐变方案
    fun saveCustomGradientSchemes(context: Context, schemes: List<Pair<String, List<Int>>>) {
        customGradientSchemes = schemes
        val str = schemes.joinToString("|") { "${it.first}:${it.second.joinToString(",")}" }
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("custom_gradient_schemes", str)
            .apply()
    }

    // 保存渐变颜色长度
    fun saveMultiColorSegmentLength(context: Context, length: Float) {
        multiColorSegmentLength = length
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putFloat("multi_color_segment_length", length)
            .apply()
    }

    // 应用预设方案
    fun applyPresetScheme(context: Context, scheme: PresetScheme) {
        val colors = when (scheme) {
            PresetScheme.RAINBOW -> listOf(
                android.graphics.Color.RED,
                android.graphics.Color.rgb(255, 165, 0), // 橙
                android.graphics.Color.YELLOW,
                android.graphics.Color.GREEN,
                android.graphics.Color.BLUE,
                android.graphics.Color.rgb(75, 0, 130), // 靛
                android.graphics.Color.rgb(139, 0, 255)  // 紫
            )
            PresetScheme.WARM -> listOf(
                android.graphics.Color.RED,
                android.graphics.Color.rgb(255, 165, 0), // 橙
                android.graphics.Color.YELLOW
            )
            PresetScheme.COOL -> listOf(
                android.graphics.Color.BLUE,
                android.graphics.Color.GREEN,
                android.graphics.Color.rgb(139, 0, 255)  // 紫
            )
            PresetScheme.HIGH_CONTRAST -> listOf(
                android.graphics.Color.RED,
                android.graphics.Color.GREEN,
                android.graphics.Color.BLUE
            )
            PresetScheme.BLUE_PINK -> listOf(
                0xFF72A7FF.toInt(), // 蓝
                0xFFFF83B6.toInt(), // 粉
            )
            PresetScheme.OCEAN -> listOf(
                0xFF008BFF.toInt(), // 蓝
                0xFF008B9E.toInt(), // 青
                0xFF00779D.toInt(), // 深蓝
            )
        }

        // 设置勾选状态（最多8个）
        multiColorSelectedColors = colors.take(8)
        saveMultiColorSelectedColors(context, multiColorSelectedColors)

        // 重置渐变颜色长度为默认值
        multiColorSegmentLength = 0f
        saveMultiColorSegmentLength(context, 0f)
    }

    fun addUserPreset(context: Context, color: Int) {
        if (!userPresets.contains(color)) {
            userPresets = userPresets + color
            savePresetsToLocal(context)
        }
    }

    fun removeUserPreset(context: Context, color: Int) {
        userPresets = userPresets - color
        savePresetsToLocal(context)
    }

    private fun savePresetsToLocal(context: Context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("user_presets", userPresets.joinToString(",")).apply()
    }

    // 添加背景色卡片的用户预设（重新保存曾被删除的内置色＝从删除名单移出、恢复内置显示；
    // 内置色不进用户预设，避免和内置列表重复显示）
    fun addBgUserPreset(context: Context, color: Int) {
        bgRemovedPresets = bgRemovedPresets - color
        if (color !in defaultBgPresets && !bgUserPresets.contains(color)) {
            bgUserPresets = bgUserPresets + color
        }
        saveBgPresetsToLocal(context)
    }

    // 从背景色卡片移除预设：内置色记入删除名单，用户预设直接移除
    fun removeBgPreset(context: Context, color: Int) {
        bgUserPresets = bgUserPresets - color
        bgRemovedPresets = bgRemovedPresets + color
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putString("test_line_bg_presets", bgUserPresets.joinToString(","))
            .putString("test_line_bg_removed_presets", bgRemovedPresets.joinToString(","))
            .apply()
    }

    private fun saveBgPresetsToLocal(context: Context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("test_line_bg_presets", bgUserPresets.joinToString(",")).apply()
    }

    fun saveCustomRadius(context: Context, enabled: Boolean, tl: Float, tr: Float, bl: Float, br: Float) {
        useCustomRadius = enabled
        radiusTL = tl; radiusTR = tr; radiusBL = bl; radiusBR = br
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
        prefs.putBoolean("use_custom_radius", enabled)
        prefs.putFloat("r_tl", tl)
        prefs.putFloat("r_tr", tr)
        prefs.putFloat("r_bl", bl)
        prefs.putFloat("r_br", br)
        prefs.apply()
    }

    // 保存圆角校准页四角 X/Y 曲率修正值。保留原有 key，确保旧版本数据无缝迁移。
    fun saveRadiusCorrections(
        context: Context,
        tlX: Float, trX: Float, blX: Float, brX: Float,
        tlY: Float, trY: Float, blY: Float, brY: Float
    ) {
        radiusTLX = tlX
        radiusTRX = trX
        radiusBLX = blX
        radiusBRX = brX
        radiusTLY = tlY
        radiusTRY = trY
        radiusBLY = blY
        radiusBRY = brY

        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putFloat("r_tl_x", radiusTLX)
            .putFloat("r_tr_x", radiusTRX)
            .putFloat("r_bl_x", radiusBLX)
            .putFloat("r_br_x", radiusBRX)
            .putFloat("r_tl_y", radiusTLY)
            .putFloat("r_tr_y", radiusTRY)
            .putFloat("r_bl_y", radiusBLY)
            .putFloat("r_br_y", radiusBRY)
            .apply()
    }

    // 圆角校准页：是否记忆上次使用的拖拽调整模式
    fun saveDragAdjustMode(context: Context, enabled: Boolean) {
        isDragAdjustModeEnabled = enabled
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("drag_adjust_mode_enabled", enabled).apply()
    }

    fun saveCalibrationLinkedMode(context: Context, linked: Boolean) {
        isCalibrationLinked = linked
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("calibration_is_linked", linked).apply()
    }

    // App每次启动时读取所有设置
    fun loadConfig(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        // 读取动效高级设置
        if (!prefs.contains("is_animation_enabled")) {
            val defaultEnabled = checkDevicePerformance(context)
            isAnimationEnabled = defaultEnabled
            prefs.edit().putBoolean("is_animation_enabled", defaultEnabled).apply()
        } else {
            isAnimationEnabled = prefs.getBoolean("is_animation_enabled", true)
        }

        // 读取关于页动态混色开关（首次默认跟随动效开关的性能检测线）
        if (!prefs.contains("about_dynamic_mix_enabled")) {
            val defaultMix = checkDevicePerformance(context)
            aboutDynamicMixEnabled = defaultMix
            prefs.edit().putBoolean("about_dynamic_mix_enabled", defaultMix).apply()
        } else {
            aboutDynamicMixEnabled = prefs.getBoolean("about_dynamic_mix_enabled", false)
        }

        // 读取精简黑边遮挡测试页文字设置
        isCompactModeEnabled = prefs.getBoolean("is_compact_mode_enabled", false)

        // 读取黑边遮挡测试长按退出设置
        longPressExitEnabled = prefs.getBoolean("long_press_exit_enabled", true)
        longPressExitSeconds = prefs.getInt("long_press_exit_seconds", 5).coerceIn(3, 20)

        // 读取视图模式设置
        isGridView = prefs.getBoolean("is_grid_view", false)

        // 读取设置页线条预览开关
        isSettingsLinePreviewEnabled = prefs.getBoolean("settings_line_preview", true)

        // 读取渐变色条模式设置
        isMultiColorMode = prefs.getBoolean("is_multi_color_mode", false)
        val selectedStr = prefs.getString("multi_color_selected", null)
        if (selectedStr != null && selectedStr.isNotEmpty()) {
            multiColorSelectedColors = selectedStr.split(",").mapNotNull { it.toIntOrNull() }
        }

        // 读取渐变颜色长度
        multiColorSegmentLength = prefs.getFloat("multi_color_segment_length", 0f)

        // 读取更新下载源
        updateDownloadSource = prefs.getString("update_source", "gitee") ?: "gitee"

        // 读取自动检查更新开关
        autoCheckUpdateEnabled = prefs.getBoolean("auto_check_update_enabled", true)

        // 读取外观设置
        val savedDark = prefs.getString("dark_mode", DarkModeConfig.FOLLOW_SYSTEM.name)
        darkModeState = try { DarkModeConfig.valueOf(savedDark ?: DarkModeConfig.FOLLOW_SYSTEM.name) } catch (e: Exception) { DarkModeConfig.FOLLOW_SYSTEM }

        // 读取线条颜色
        testLineColor = prefs.getInt("line_color", android.graphics.Color.WHITE)

        // 读取测试页背景色 / 文字色及其联动设置
        testLineBgColor = prefs.getInt("test_line_bg_color", android.graphics.Color.BLACK)
        testLineTextColor = prefs.getInt("test_line_text_color", android.graphics.Color.WHITE)
        textFollowsLine = prefs.getBoolean("test_text_follows_line", false)
        titleFollowsText = prefs.getBoolean("title_follows_text", false)
        precisionUsesCustomBg = prefs.getBoolean("test_precision_uses_custom_bg", false)
        deviceNameOpacityPct = prefs.getInt("device_name_opacity_pct", 100)

        val customSchemesStr = prefs.getString("custom_gradient_schemes", "") ?: ""
        if (customSchemesStr.isNotEmpty()) {
            customGradientSchemes = customSchemesStr.split("|").mapNotNull { s ->
                try {
                    if (s.contains(":")) {
                        val parts = s.split(":")
                        val name = parts[0]
                        val colors = parts[1].split(",").mapNotNull { it.toIntOrNull() }
                        if (colors.isNotEmpty()) name to colors else null
                    } else {
                        val colors = s.split(",").mapNotNull { it.toIntOrNull() }
                        if (colors.isNotEmpty()) "自定义" to colors else null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        // 读取亮度设置
        isMaxBrightnessEnabled = prefs.getBoolean("max_brightness", false)
        testBrightnessValue = prefs.getFloat("test_brightness_val", 1.0f)

        // 读取测试页屏幕常亮开关
        isKeepScreenOnEnabled = prefs.getBoolean("keep_screen_on_enabled", false)

        // 读取线条粗细设置
        testLineThickness = prefs.getFloat("line_thickness", 5f)

        // 每次打开 App 自动恢复上次调好的圆角数据
        useCustomRadius = prefs.getBoolean("use_custom_radius", true)
        radiusTL = prefs.getFloat("r_tl", -1f)
        radiusTR = prefs.getFloat("r_tr", -1f)
        radiusBL = prefs.getFloat("r_bl", -1f)
        radiusBR = prefs.getFloat("r_br", -1f)

        // 读取圆角校准页四角 X/Y 曲率修正值：沿用旧 key，不丢失已有用户数据
        radiusTLX = prefs.getFloat("r_tl_x", 0f)
        radiusTRX = prefs.getFloat("r_tr_x", 0f)
        radiusBLX = prefs.getFloat("r_bl_x", 0f)
        radiusBRX = prefs.getFloat("r_br_x", 0f)
        radiusTLY = prefs.getFloat("r_tl_y", 0f)
        radiusTRY = prefs.getFloat("r_tr_y", 0f)
        radiusBLY = prefs.getFloat("r_bl_y", 0f)
        radiusBRY = prefs.getFloat("r_br_y", 0f)

        // 读取圆角校准页最近一次使用的拖拽调整模式
        isDragAdjustModeEnabled = prefs.getBoolean("drag_adjust_mode_enabled", false)
        isCalibrationLinked = prefs.getBoolean("calibration_is_linked", true)

        // 读取预设列表：首次运行初始化默认 12 色；
        // 3.0 预设扩充迁移：往已有列表追加新增的 6 色（已手动添加过的不重复，
        // 用户删过的旧默认色不恢复）；一次性执行，之后删掉的新色不会再被补回
        val presetStr = prefs.getString("user_presets", null)
        if (presetStr == null) {
            userPresets = listOf(
                -1, // 白色
                -9263105, // 莫奈蓝
                -1845525, // TertiaryContainer
                -1254181, // PrimaryContainer
                -7981735, // Primary
                -5431481, // Error 红
                -7679029, // 青
                -9579124, // 莫奈绿
                -663924, // 奶黄
                -19045, // 蜜桃橙
                -3561493, // 藕紫
                -11900006 // 深蓝（浅色背景下的线条用）
            )
            savePresetsToLocal(context)
        } else if (presetStr.isNotEmpty()) {
            userPresets = presetStr.split(",").mapNotNull { it.toIntOrNull() }
            if (!prefs.getBoolean("user_presets_expanded_12", false)) {
                val addedPresets = listOf(-7679029, -9579124, -663924, -19045, -3561493, -11900006)
                    .filter { it !in userPresets }
                if (addedPresets.isNotEmpty()) {
                    userPresets = userPresets + addedPresets
                    savePresetsToLocal(context)
                }
                prefs.edit().putBoolean("user_presets_expanded_12", true).apply()
            }
        }

        // 背景色卡片独立预设（无默认值）+ 被删除的内置预设名单
        val bgPresetStr = prefs.getString("test_line_bg_presets", "") ?: ""
        bgUserPresets = bgPresetStr.split(",").mapNotNull { it.toIntOrNull() }
        val bgRemovedStr = prefs.getString("test_line_bg_removed_presets", "") ?: ""
        bgRemovedPresets = bgRemovedStr.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    // 硬件性能检测算法
    private fun checkDevicePerformance(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager

        // 1. 低内存设备（Low RAM），默认关闭动画
        if (am.isLowRamDevice) return false

        // 2. 利用 Performance Class 辨别性能层级（需要 API 31+）
        //    若设备不支持 Performance Class，则用 CPU 核心数 and 内存兜底判断
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S
            || android.os.Build.VERSION.MEDIA_PERFORMANCE_CLASS < android.os.Build.VERSION_CODES.S) {
            val info = android.app.ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalRamGb = info.totalMem / (1024 * 1024 * 1024f)
            if (Runtime.getRuntime().availableProcessors() < 4 || totalRamGb < 4f) {
                return false
            }
        }
        return true
    }
}