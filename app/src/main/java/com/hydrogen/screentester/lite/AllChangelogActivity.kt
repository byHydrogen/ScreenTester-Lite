package com.hydrogen.screentester.lite

import android.os.Bundle
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AllChangelogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 关闭三键导航栏的半透明对比遮罩
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val isDark = when (ThemeSettings.darkModeState) {
                DarkModeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                DarkModeConfig.LIGHT -> false
                DarkModeConfig.DARK -> true
            }

            val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()

            // 状态栏/导航栏图标跟随「应用内」深色设置
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as android.app.Activity).window
                    androidx.core.view.WindowInsetsControllerCompat(window, view).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }
            }

            MaterialTheme(colorScheme = colorScheme) {
                AllChangelogScreen(isDark = isDark) { finish() }
            }
        }
    }
}

data class LogLineItem(val tag: String?, val mainText: String, val subText: String?)

internal val changelogEntries = listOf(
            "3.0" to "#归类\n重构 底部弹窗（改为自绘实现，发现新版本 / 跳转链接 / 加入QQ群 等弹窗共用）\n新增 OOBE 悬浮底栏\n新增 OOBE 欢迎页 设备支持完整版 引导卡片（仅 Android 12+ 显示）\n新增 OOBE 圆角校准步骤 拖拽调整模式\n新增 OOBE 测试线条步骤 底部「更多设置」入口\n新增 OOBE 界面高级动效步骤 背景动态混色 开关\n新增 OOBE 动态混色背景\n新增 校准车间页 拖拽调整模式\n新增 黑边遮挡测试 支持自定义背景颜色\n新增 黑边遮挡测试 支持自定义文字颜色\n新增 设置页 自定义测试线条粗细和颜色 底部「更多设置」入口（点击后进入线条与配色页）\n新增 设置页 界面高级动效 背景动态混色 开关\n新增 线条与配色页（可调整黑边遮挡测试的线条 / 背景 / 文字并实时预览调整效果）\n新增 线条与配色页 线条 / 背景 色彩预设扩充至 12 色\n新增 线条与配色页 背景色彩预设支持长按删除\n新增 关于页 动态混色背景\n新增 关于页 更新日志板块 支持更新日志按模块自动归类\n新增 历史更新日志页 动态混色背景\n新增 历史更新日志页 支持更新日志按模块自动归类（点分类只看该模块 / 长按可叠加多个 / 点击全部显示完整更新日志）\n新增 赞赏页 动态混色背景\n优化 平板部分页面显示布局\n优化 黑边遮挡测试 圆角/精度模式切换动画\n优化 设置页 下载与更新 更新下载源选择UI\n优化 关于页 设备信息卡片排版\n优化 关于页 部分设备布局显示问题\n优化 线条与配色页 色彩预设折叠阈值（4 行起折叠）\n修改 黑边遮挡测试 精度模式下 居中文本（带黑边膜挡屏测试 → 黑边遮挡测试）\n修改 黑边遮挡测试 精度模式下 长按退出提示文本（退出倒计时：x 秒 → 请继续按住 x 秒...）\n修复 黑边遮挡测试 精度模式下 部分设备居中文本超出分区下沿的问题\n修复 关于页 未滚动到的卡片仍可点击的问题\n修复 部分设备机型 OS 版本读取异常的问题\n修复 OOBE 圆角校准引导 高亮卡片被遮挡时未自动抬高的问题\n修复 了一些已知问题\n移除 OOBE 检查更新步骤",
            "2.5" to "重构 主页 网格/列表\n新增 主页 全新 卡片视图切换动画\n优化 主页 搜索测试项结果卡片动画\n修复 主页 网格视图下搜索时卡片未重排的问题\n修复 Android 15 以下 Android 版本 主界面 导航条有半透明遮罩的问题\n修复 Android 15 以下 Android 版本 历史更新日志页 导航条有半透明遮罩的问题\n修复 Android 15 以下 Android 版本 赞赏页 导航条有半透明遮罩的问题\n修复 部分 Android 设备 测试页始终显示刘海的问题（渲染进摄像头挖孔区域，避免全屏时挖孔处出现黑条）\n修复 了一些已知问题",
            "2.0" to "新增 测试亮度设置 测试页屏幕常亮 开关\n修复 黑边遮挡测试 精度模式 遮挡宽度毫米数值（改为按设备屏幕实际密度动态计算）\n修复 了一些已知问题",
            "1.2" to "新增 设置页 自定义黑边遮挡测试 长按退出 开关\n新增 设置页 自定义黑边遮挡测试 自定义退出时长 滑块\n新增 设置页 下载与更新 自动检查更新开关\n优化 主页 首次启动时的「加入QQ交流群」弹窗（改为显示横幅，30秒后自动消失）\n修复 系统导航为导航键时 底栏被遮挡的问题\n修复 了一些已知问题",
            "1.1" to "新增 支持应用内下载更新包\n新增 Gitee 更新下载源\n新增 设置页 下载与更新卡片（切换下载源）\n新增 设置页 渐变色条 蓝粉预设方案\n新增 设置页 渐变色条 海洋预设方案\n移除 设置页 渐变色条 莫奈色预设方案\n修复 黑边遮挡测试 渐变色条和设置预览时显示不一致的问题\n修改 关于页 更新日志卡片 版本更新卡片",
            "1.0" to "ScreenTester Lite 首个版本"
)


// 顶部内容渐隐：不着色，只把内容按竖向遮罩淡出
internal fun Modifier.fadeOutAtTop(fadeEnd: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                (fadeEnd.toPx() / size.height).coerceIn(0.01f, 1f) to Color.Black
            ),
            blendMode = BlendMode.DstIn
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllChangelogScreen(isDark: Boolean, onBack: () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    val systemMonetPrimary = remember(isDark) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
        } else {
            if (isDark) darkColorScheme().primary else lightColorScheme().primary
        }
    }

    val backgroundBrush = DeviceUtils.backgroundBrush(isDark)

    val cardG2Shape = remember {
        object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
                val path = androidx.compose.ui.graphics.Path()
                val w = size.width; val h = size.height
                val radius = with(density) { 24.dp.toPx() }
                val p = (1.4f * radius).coerceAtMost(h / 2f)
                val safeRadius = p / 1.4f; val c = 0.45f * safeRadius
                path.moveTo(p, 0f); path.lineTo(w - p, 0f); path.cubicTo(w - c, 0f, w, c, w, p); path.lineTo(w, h - p); path.cubicTo(w, h - c, w - c, h, w - p, h); path.lineTo(p, h); path.cubicTo(c, h, 0f, h - c, 0f, h - p); path.lineTo(0f, p); path.cubicTo(0f, c, c, 0f, p, 0f); path.close()
                return androidx.compose.ui.graphics.Outline.Generic(path)
            }
        }
    }

    val changelogs = changelogEntries

    var expandedVersions by rememberSaveable {
        mutableStateOf(changelogs.take(2).map { it.first }.toSet())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        // 背景层：动态混色（跟随全局开关）或原静态渐变
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (ThemeSettings.aboutDynamicMixEnabled) Modifier.dynamicMixBackground(isDark, running = true)
                    else Modifier.background(backgroundBrush)
                )
        )

        // 内容占满全屏：滚动时可从顶栏下方穿过
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // 平板限宽居中
                .widthIn(max = DeviceUtils.NavBarMaxWidth)
                .fillMaxSize()
                // 动态混色开启时顶栏不铺色，改用内容顶部渐隐避免文字顶到状态栏
                .then(
                    if (ThemeSettings.aboutDynamicMixEnabled) Modifier.fadeOutAtTop(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp
                    ) else Modifier
                ),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                items(changelogs) { (version, logText) ->
                    val isItemExpanded = expandedVersions.contains(version)
                    val arrowRotation by animateFloatAsState(targetValue = if (isItemExpanded) 180f else 0f, label = "arrow")

                    val cardContainerColor = DeviceUtils.cardContainerColor(isDark)
                    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Transparent

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardG2Shape,
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        expandedVersions = if (isItemExpanded) expandedVersions - version else expandedVersions + version
                                    }
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "版本 $version",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (version == currentVersionName) systemMonetPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = arrowRotation },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }

                            AnimatedVisibility(visible = isItemExpanded) {
                                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )

                                    // 日志首行写 #归类 时按模块分组展示（点分类只看该模块 / 长按叠加多选），否则平铺
                                    ChangelogGroupedContent(
                                        logText = logText,
                                        version = version,
                                        isDark = isDark,
                                        accent = systemMonetPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 顶栏 overlay：动态混色开启时完全透明（不压流动背景），关闭时保持原同色渐变
            val topBarBaseColor = DeviceUtils.backgroundBaseColor(isDark)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .then(
                        if (ThemeSettings.aboutDynamicMixEnabled) Modifier
                        else Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    topBarBaseColor,
                                    topBarBaseColor.copy(alpha = 0.95f),
                                    topBarBaseColor.copy(alpha = 0.60f),
                                    topBarBaseColor.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            )
                        )
                    )
                    .padding(bottom = 28.dp)
            ) {
                TopAppBar(
                    title = { Text("历史更新日志", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
    }
}

@Composable
fun ChangelogRowRenderer(
    item: LogLineItem,
    isDark: Boolean,
    useTagBadge: Boolean = true,
    bodyFontSize: TextUnit = 14.sp,
    bodyLineHeight: TextUnit = 20.sp,
    bodyColor: Color = MaterialTheme.colorScheme.onSurface,
    plainLine: Boolean = false
) {
    // 整行一个 Text：标签与正文之间用原文的空格，间距/基线天然一致
    if (plainLine) {
        Text(
            text = buildString {
                if (item.tag != null) append(item.tag).append(' ')
                append(item.mainText)
                if (item.subText != null) append('（').append(item.subText).append('）')
            },
            fontSize = bodyFontSize,
            lineHeight = bodyLineHeight,
            color = bodyColor
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (item.tag != null && useTagBadge) {
            TagBadge(
                tag = item.tag,
                isDark = isDark,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .alignByBaseline()
        ) {
            Text(
                text = item.mainText,
                fontSize = bodyFontSize,
                fontWeight = FontWeight.Medium,
                color = bodyColor,
                lineHeight = bodyLineHeight
            )
            if (item.subText != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.subText,
                    fontSize = 11.5.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun TagBadge(tag: String, isDark: Boolean, modifier: Modifier = Modifier) {
    val containerColor = when (tag) {
        "新增" -> if (isDark) Color(0xFF2A3A2E) else Color(0xFFD2E7D6)
        "优化" -> if (isDark) Color(0xFF25354A) else Color(0xFFD2E4FF)
        "重构" -> if (isDark) Color(0xFF1F3A38) else Color(0xFFD2F0ED)
        "修复" -> if (isDark) Color(0xFF422B2D) else Color(0xFFFAD8D8)
        "调整" -> if (isDark) Color(0xFF332B45) else Color(0xFFE9DFF5)
        "补充" -> if (isDark) Color(0xFF3D3228) else Color(0xFFFAE3CB)
        "修改" -> if (isDark) Color(0xFF3A2E1A) else Color(0xFFF5E1C0)
        "移除" -> if (isDark) Color(0xFF4A2222) else Color(0xFFE8C8C8)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (tag) {
        "新增" -> if (isDark) Color(0xFFACD3B6) else Color(0xFF386B49)
        "优化" -> if (isDark) Color(0xFFADC7EF) else Color(0xFF3C5E8E)
        "重构" -> if (isDark) Color(0xFF9FDCD6) else Color(0xFF2E7A72)
        "修复" -> if (isDark) Color(0xFFF3B9BA) else Color(0xFF904A4A)
        "调整" -> if (isDark) Color(0xFFDBBFFE) else Color(0xFF6B4EA2)
        "补充" -> if (isDark) Color(0xFFF3C497) else Color(0xFF825525)
        "修改" -> if (isDark) Color(0xFFE8C885) else Color(0xFF8B6914)
        "移除" -> if (isDark) Color(0xFFE8A0A0) else Color(0xFF8B4848)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .background(color = containerColor, shape = RoundedCornerShape(9.dp))
            .padding(horizontal = 6.5.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = tag,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}