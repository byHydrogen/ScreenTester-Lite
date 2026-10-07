package com.hydrogen.screentester.lite

import android.content.Intent
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(pageVisible: Boolean) {
    val isDark = when (ThemeSettings.darkModeState) {
        DarkModeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkModeConfig.LIGHT -> false
        DarkModeConfig.DARK -> true
    }
    val scrollState = rememberScrollState()
    val marketName = remember { DeviceUtils.getMarketName() }
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // 根据系统莫奈取色的色相，自动切换背景混色方案
    val backgroundBrush = DeviceUtils.backgroundBrush(isDark)

    val cardG2Shape = G2Shapes.aboutCard

    var isChangelogExp by remember { mutableStateOf(false) }
    val changelogArrowRotation by animateFloatAsState(targetValue = if (isChangelogExp) 180f else 0f, label = "changelogArrow")

    var isCreditsExp by remember { mutableStateOf(false) }
    val creditsArrowRotation by animateFloatAsState(targetValue = if (isCreditsExp) 180f else 0f, label = "creditsArrow")

    // === 更新状态管理 ===
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var hasNewVersion by GlobalUpdateState::hasNewVersion
    var latestVersionName by GlobalUpdateState::latestVersionName
    var latestChangelog by GlobalUpdateState::latestChangelog
    var checkButtonText by remember { mutableStateOf("检测更新") }
    var showLinkDialog by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 背景独立成层：动态混色（跟随全局开关）或原静态渐变。
        // clipToBounds：混色光斑绘制半径大于页面，必须裁剪在关于页范围内，
        // 否则溢出部分会盖到 Pager 里常驻组合的主页/设置页。
        // pageVisible 只用于暂停/恢复动画时间累计，离开页面冻结当前帧、回来无缝续播
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .then(
                    if (ThemeSettings.aboutDynamicMixEnabled) Modifier.dynamicMixBackground(isDark, pageVisible)
                    else Modifier.background(backgroundBrush)
                )
        )
        val screenHeight = this.maxHeight
        val context = LocalContext.current
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: "1.0.0"

        // 横屏（宽 > 高）时改为两栏：头部固定在左、卡片在右；竖屏一切照旧
        val isLandscape = maxWidth > maxHeight
        val contentWidth = minOf(maxWidth, DeviceUtils.ContentMaxWidth)
        // 横屏居中：用设备卡片"实测"高度算顶部留白（行数/字体缩放都会改高度，不能写死）
        val localDensity = androidx.compose.ui.platform.LocalDensity.current
        var deviceCardHeight by remember { mutableStateOf(0.dp) }
        val landscapeCenteringPad = ((screenHeight - deviceCardHeight) / 2).coerceAtLeast(0.dp)

        // 头部（Logo + 标题 + 徽章 + 副标题）抽成 lambda：竖屏在流内居中渲染，横屏由左侧固定栏渲染
        val headerContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val logoG2Shape = G2Shapes.logo

                Box(
                    Modifier
                        .size(110.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = logoG2Shape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "ScreenTester",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color(0xFFF8E7F0) else MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = G2Shapes.icon,
                    color = (if (isDark) Color(0xFFF8E7F0) else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Lite",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF8E7F0) else MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "$versionName | by Hydrogen",
                    fontSize = 16.sp,
                    color = if (isDark) Color.Gray.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // 横屏左栏：固定不随滚动，头部垂直居中
        if (isLandscape) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .width(contentWidth)
                    .padding(end = contentWidth * 0.58f),
                contentAlignment = Alignment.Center
            ) {
                headerContent()
            }
        }

        Column(
            modifier = Modifier
                // 大屏适配：内容限宽居中
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                // 竖屏按底栏宽度限宽，卡片边缘与底栏齐平
                .widthIn(
                    max = if (isLandscape) DeviceUtils.ContentMaxWidth
                    else DeviceUtils.NavBarMaxWidth + 48.dp
                )
                // 横屏：左侧让出固定头部那一栏
                .then(if (isLandscape) Modifier.padding(start = contentWidth * 0.42f) else Modifier)
                .verticalScroll(scrollState)
        ) {
            // 第一屏：主界面
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 竖屏：至少一屏高，把设备卡片压到底部
                    // 横屏：用上方精确算出的留白居中（见 landscapeCenteringPad）
                    .then(if (isLandscape) Modifier else Modifier.heightIn(min = screenHeight))
            ) {
                // 顶部留白 = 保底 + 动态两部分：
                // 保底为状态栏高度 + 16dp（独立固定 Spacer，weight 无法压缩它，小屏上图标也不会顶到状态栏）；
                // 动态部分按 weight 分配、连同保底合计最高 220dp（高屏机型如小米14 顶满，与原效果一致），
                // 屏幕偏矮的机型（如小米15）自动收缩、Logo 上移，空间不足时页面溢出滚动
                // 竖屏：顶部留白 + 压到底部的弹性留白
                if (!isLandscape) {
                    val topFloor = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
                    Spacer(modifier = Modifier.height(topFloor))
                    Spacer(modifier = Modifier.weight(3.5f).heightIn(max = 220.dp - topFloor))
                } else {
                    // 横屏：顶部留白把卡片中心推到中线
                    Spacer(modifier = Modifier.height(landscapeCenteringPad))
                }

                // 顶部 Logo 部分：竖屏在流内渲染；横屏改由上方左侧固定栏渲染
                if (!isLandscape) headerContent()

                // 版本文字与设备卡片之间的呼吸间距；顶部留白收缩/触顶后，剩余空间都会流到这里
                // 横屏跳过（由垂直居中处理）
                if (!isLandscape) Spacer(modifier = Modifier.weight(1f))

                // 设备信息卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 量出卡片真实高度，供横屏居中计算留白
                        .onSizeChanged { deviceCardHeight = with(localDensity) { it.height.toDp() } }
                        .padding(horizontal = 24.dp)
                        // 底部留白给悬浮底栏（竖屏需要）；横屏两边都不留（末尾已有收尾留白）
                        .padding(bottom = if (isLandscape) 0.dp else 120.dp),
                    shape = cardG2Shape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                ) {
                    Column(Modifier.padding(28.dp)) {
                        Text(
                            text = marketName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(24.dp))
                        // 信息项间距用 spacedBy 统一控制，保证卡片上下内边距一致（28dp）
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            DeviceInfoItem(label = "设备型号", value = Build.MODEL)
                            DeviceInfoItem(label = "Android 版本", value = Build.VERSION.RELEASE)
                            DeviceInfoItem(label = "OS 版本", value = DeviceUtils.getOSVersion())
                        }
                    }
                }
            }

            // 横屏：卡片与第二屏之间补呼吸间距
            if (isLandscape) Spacer(modifier = Modifier.height(16.dp))

            // 第二屏：开发者卡片
            val maxScrollVal = scrollState.maxValue.toFloat()
            val curScrollVal = scrollState.value.toFloat()
            val scrollProgress = if (maxScrollVal > 0) (curScrollVal / maxScrollVal * 1.2f).coerceIn(0f, 1f) else 1f

            var devCardVisible by remember { mutableStateOf(false) }
            var donateCardVisible by remember { mutableStateOf(false) }
            var qqCardVisible by remember { mutableStateOf(false) }
            var projCardVisible by remember { mutableStateOf(false) }
            var changelogCardVisible by remember { mutableStateOf(false) }
            var creditsCardVisible by remember { mutableStateOf(false) }

            LaunchedEffect(scrollProgress) {
                if (scrollProgress >= 0.10f) devCardVisible = true
                else if (scrollProgress <= 0.04f) devCardVisible = false
                if (scrollProgress >= 0.25f) projCardVisible = true
                else if (scrollProgress <= 0.18f) projCardVisible = false
                // "更新日志"卡片展开时跟随"开源项目地址"卡片出现；
                // 未展开时沿用原来的阈值
                val changelogRevealAt = if (isChangelogExp) 0.25f else 0.40f
                val changelogHideAt = if (isChangelogExp) 0.18f else 0.32f
                if (scrollProgress >= changelogRevealAt) changelogCardVisible = true
                else if (scrollProgress <= changelogHideAt) changelogCardVisible = false
                if (scrollProgress >= 0.55f) creditsCardVisible = true
                else if (scrollProgress <= 0.46f) creditsCardVisible = false
                if (scrollProgress >= 0.68f) donateCardVisible = true
                else if (scrollProgress <= 0.58f) donateCardVisible = false
                if (scrollProgress >= 0.78f) qqCardVisible = true
                else if (scrollProgress <= 0.68f) qqCardVisible = false
            }

            val devCardAlpha by animateFloatAsState(targetValue = if (devCardVisible) 1f else 0f, animationSpec = tween(400), label = "devCardAlpha")
            val devCardTransY by animateFloatAsState(targetValue = if (devCardVisible) 0f else 60f, animationSpec = tween(400), label = "devCardTransY")
            val donateCardAlpha by animateFloatAsState(targetValue = if (donateCardVisible) 1f else 0f, animationSpec = tween(400), label = "donateCardAlpha")
            val donateCardTransY by animateFloatAsState(targetValue = if (donateCardVisible) 0f else 60f, animationSpec = tween(400), label = "donateCardTransY")
            val qqCardAlpha by animateFloatAsState(targetValue = if (qqCardVisible) 1f else 0f, animationSpec = tween(400), label = "qqCardAlpha")
            val qqCardTransY by animateFloatAsState(targetValue = if (qqCardVisible) 0f else 60f, animationSpec = tween(400), label = "qqCardTransY")
            val projCardAlpha by animateFloatAsState(targetValue = if (projCardVisible) 1f else 0f, animationSpec = tween(400, delayMillis = if (!projCardVisible && isChangelogExp) 180 else 0), label = "projCardAlpha")
            val projCardTransY by animateFloatAsState(targetValue = if (projCardVisible) 0f else 60f, animationSpec = tween(400, delayMillis = if (!projCardVisible && isChangelogExp) 180 else 0), label = "projCardTransY")
            val changelogCardAlpha by animateFloatAsState(targetValue = if (changelogCardVisible) 1f else 0f, animationSpec = tween(400, delayMillis = if (changelogCardVisible && isChangelogExp) 180 else 0), label = "changelogCardAlpha")
            val changelogCardTransY by animateFloatAsState(targetValue = if (changelogCardVisible) 0f else 60f, animationSpec = tween(400, delayMillis = if (changelogCardVisible && isChangelogExp) 180 else 0), label = "changelogCardTransY")
            val creditsCardAlpha by animateFloatAsState(targetValue = if (creditsCardVisible) 1f else 0f, animationSpec = tween(400), label = "creditsCardAlpha")
            val creditsCardTransY by animateFloatAsState(targetValue = if (creditsCardVisible) 0f else 60f, animationSpec = tween(400), label = "creditsCardTransY")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    // -102dp 用于吃掉设备卡片的底部留白
                    // 横屏归零：120dp 已去掉，负偏移无可吃空间
                    .offset(y = if (isLandscape) 0.dp else (-102).dp)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = devCardAlpha; translationY = devCardTransY.dp.toPx() }
                        // 未入场（alpha=0）时屏蔽触摸
                        .blockTapsWhileHidden(!devCardVisible)
                ) {
                    DeveloperProfileCard(isDark = isDark)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = projCardAlpha; translationY = projCardTransY.dp.toPx() }
                        .blockTapsWhileHidden(!projCardVisible)
                ) {
                    ProjectSourceCard(isDark = isDark)
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 更新日志板块
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = changelogCardAlpha; translationY = changelogCardTransY.dp.toPx() }
                        .blockTapsWhileHidden(!changelogCardVisible)
                ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardG2Shape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    isChangelogExp = !isChangelogExp
                                }
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("更新日志", modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            if (hasNewVersion && !isChangelogExp) {
                                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { rotationZ = changelogArrowRotation },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        AnimatedVisibility(visible = isChangelogExp) {
                            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                val aboutButtonG2Shape = G2Shapes.aboutButton

                                // 版本更新卡片
                                var showUpdateSheet by remember { mutableStateOf(false) }
                                val newVersionCardShape = G2Shapes.newVersionCard

                                AnimatedVisibility(visible = hasNewVersion) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(newVersionCardShape).clickable { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); showUpdateSheet = true },
                                        shape = newVersionCardShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("发现新版本：$latestVersionName", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text("点击查看详情", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                            }
                                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                        }
                                    }
                                }

                                if (showUpdateSheet) {
                                    UpdateSheetDialog(onDismiss = { showUpdateSheet = false })
                                }

                                // 当前版本信息
                                Text(
                                    text = "版本 $versionName",
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                // 最新版本日志：与历史更新日志页共用同一份数据，首行 #归类 触发按模块分组
                                ChangelogGroupedContent(
                                    logText = changelogEntries.first().second,
                                    version = changelogEntries.first().first,
                                    isDark = isDark,
                                    accent = MaterialTheme.colorScheme.primary,
                                    useTagBadge = false,
                                    plainLine = true,
                                    compactStyle = true,
                                    bodyFontSize = 13.sp,
                                    bodyLineHeight = 18.sp,
                                    bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                // 检测更新按钮
                                Button(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        if (!isCheckingUpdate && checkButtonText != "已是最新版本") {
                                            isCheckingUpdate = true
                                            checkButtonText = "正在检查..."
                                            UpdateManager.checkUpdate(
                                                context,
                                                isManual = true,
                                                onResult = { hasUpdate, version, changelog, downloadUrl ->
                                                    isCheckingUpdate = false
                                                    if (hasUpdate && version != null) {
                                                        if (UpdateManager.isVersionGreater(version, versionName)) {
                                                            hasNewVersion = true
                                                            latestVersionName = version
                                                            latestChangelog = changelog ?: ""
                                                            GlobalUpdateState.latestDownloadUrl = downloadUrl
                                                            checkButtonText = "发现新版本"
                                                        } else {
                                                            checkButtonText = "已是最新版本"
                                                            scope.launch {
                                                                delay(2000)
                                                                if (checkButtonText == "已是最新版本") {
                                                                    checkButtonText = "检测更新"
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        checkButtonText = "已是最新版本"
                                                        scope.launch {
                                                            delay(2000)
                                                            if (checkButtonText == "已是最新版本") {
                                                                checkButtonText = "检测更新"
                                                            }
                                                        }
                                                    }
                                                },
                                                onError = {
                                                    isCheckingUpdate = false
                                                    checkButtonText = "检查失败"
                                                    scope.launch {
                                                        delay(2000)
                                                        if (checkButtonText == "检查失败") {
                                                            checkButtonText = "检测更新"
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    shape = aboutButtonG2Shape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    AnimatedContent(
                                        targetState = checkButtonText,
                                        transitionSpec = {
                                            val enter = slideInVertically(
                                                initialOffsetY = { it },
                                                animationSpec = tween(durationMillis = 300)
                                            ) + fadeIn(animationSpec = tween(300))

                                            val exit = slideOutVertically(
                                                targetOffsetY = { -it },
                                                animationSpec = tween(durationMillis = 300)
                                            ) + fadeOut(animationSpec = tween(300))

                                            enter togetherWith exit
                                        },
                                        label = "UpdateButtonTextAnimation"
                                    ) { currentText ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (currentText == "正在检查...") {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(text = currentText, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 历史更新按钮
                                Button(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        context.startActivity(Intent(context, AllChangelogActivity::class.java))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = aboutButtonG2Shape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = "查看历史更新日志",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 致谢板块
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = creditsCardAlpha; translationY = creditsCardTransY.dp.toPx() }
                        .blockTapsWhileHidden(!creditsCardVisible)
                ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardG2Shape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    isCreditsExp = !isCreditsExp
                                }
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "致谢",
                                modifier = Modifier.weight(1f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { rotationZ = creditsArrowRotation },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        AnimatedVisibility(visible = isCreditsExp) {
                            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(4.dp))

                                val credits = listOf(
                                    Triple("AndroidX Core KTX", "The Android Open Source Project", "https://developer.android.com/jetpack/androidx/releases/core"),
                                    Triple("AndroidX Lifecycle", "The Android Open Source Project", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
                                    Triple("Jetpack Compose", "The Android Open Source Project", "https://developer.android.com/jetpack/androidx/releases/compose"),
                                    Triple("AndroidX Activity", "The Android Open Source Project", "https://developer.android.com/jetpack/androidx/releases/activity"),
                                    Triple("AndroidX AppCompat", "The Android Open Source Project", "https://developer.android.com/jetpack/androidx/releases/appcompat"),
                                    Triple("Google Material Design", "Google", "https://github.com/material-components/material-components-android"),
                                    Triple("AndroidX ConstraintLayout", "The Android Open Source Project", "https://github.com/androidx/constraintlayout"),
                                    Triple("Compose Material Icons", "The Android Open Source Project", "https://developer.android.com/jetpack/androidx/releases/compose")
                                )

                                credits.forEach { (name, author, url) ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(G2Shapes.gridCard),
                                        color = Color.Transparent,
                                        shadowElevation = 0.dp,
                                        tonalElevation = 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                                    } catch (_: Exception) {}
                                                }
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                                                Text(
                                                    text = name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = author,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "打开链接",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            }
                        }
                    }
                }

                // 支持开发者卡片
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.graphicsLayer { alpha = donateCardAlpha; translationY = donateCardTransY.dp.toPx() }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardG2Shape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                context.startActivity(Intent(context, DonateActivity::class.java))
                            },
                        shape = cardG2Shape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.LocalCafe, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "支持开发者",
                                modifier = Modifier.weight(1f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 加入QQ交流群卡片
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.graphicsLayer { alpha = qqCardAlpha; translationY = qqCardTransY.dp.toPx() }) {
                    var showQQDialog by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardG2Shape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showQQDialog = true
                            },
                        shape = cardG2Shape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.QuestionAnswer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "加入QQ交流群",
                                modifier = Modifier.weight(1f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (showQQDialog) {
                        QQGroupDialog(onDismiss = { showQQDialog = false })
                    }
                }

                Spacer(modifier = Modifier.height(130.dp))
            }
        }

        // 链接确认弹窗
        if (showLinkDialog != null) {
            LinkConfirmDialog(
                url = showLinkDialog ?: "",
                onDismiss = { showLinkDialog = null }
            )
        }
    }
}

// 开发者卡片组件
@Composable
fun DeveloperProfileCard(isDark: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val cardG2Shape = G2Shapes.aboutCard

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardG2Shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    try {
                        val uri = android.net.Uri.parse("https://www.coolapk.com/u/18917701")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(brush = Brush.linearGradient(colors = listOf(color1, color2)), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "氢", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Hydrogen氢", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "前往酷安作者主页", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}

// 项目地址卡片组件
@Composable
fun ProjectSourceCard(isDark: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val cardG2Shape = G2Shapes.aboutCard

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardG2Shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    try {
                        val uri = android.net.Uri.parse("https://github.com/byHydrogen/ScreenTester-Lite/")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(color = if (isDark) Color(0xFF2D333B) else Color(0xFF24292F), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "开源项目地址", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "前往 GitHub 查阅源码", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun DeviceInfoItem(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// 隐藏时只拦"点击"、不拦滑动：抬手前没有明显位移才消费掉，
// 这样卡片空白处仍能起手滑动
private fun Modifier.blockTapsWhileHidden(hidden: Boolean): Modifier =
    if (!hidden) this else this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var isTap = true
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) isTap = false
                if (!change.pressed) {
                    if (isTap) change.consume()
                    break
                }
            }
        }
    }

// 关于页动态混色背景（HyperOS「我的设备」风格）：
// 三个饱和度更高、色相相互拉开的大柔光光斑，沿利萨如轨迹缓慢漂移、互相叠加混色。
// 动画时间用 withFrameNanos 手动累计：running=false 时冻结当前帧（不重绘、零开销），
// 恢复时从原相位无缝续播，因此切走再切回不会跳变；
// 轨迹频率全部取整数倍，保证相位环绕时 sin/cos 取值连续，长时间播放也不跳
@Composable
fun Modifier.dynamicMixBackground(isDark: Boolean, running: Boolean): Modifier {
    val colors = DeviceUtils.dynamicMixColors(isDark)

    // 累计播放时长（ms）：只在 running 时累加，暂停即冻结
    var animTimeMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var lastFrame = -1L
        while (true) {
            withFrameNanos { now ->
                if (lastFrame >= 0) animTimeMs += (now - lastFrame) / 1_000_000f
                lastFrame = now
            }
        }
    }

    return drawBehind {
        val w = size.width
        val h = size.height

        // 底色取第一个颜色，保证光斑间隙不露黑
        drawRect(color = colors[0])

        val tau = (2.0 * Math.PI).toFloat()
        val p1 = (animTimeMs / 11000f % 1f) * tau
        val p2 = (animTimeMs / 16000f % 1f) * tau
        val p3 = (animTimeMs / 13000f % 1f) * tau

        // 光斑半径约 0.6 倍屏幕对角线，中心按各自轨迹漂移；越界部分由外层 clipToBounds 裁掉
        val radius = kotlin.math.hypot(w, h) * 0.6f

        listOf(
            Triple(colors[1], w * (0.30f + 0.25f * kotlin.math.cos(p1)), h * (0.35f + 0.28f * kotlin.math.sin(2f * p1))),
            Triple(colors[2], w * (0.70f + 0.25f * kotlin.math.cos(2f * p2 + 0.8f)), h * (0.60f + 0.30f * kotlin.math.sin(p2 + 2.5f))),
            Triple(colors[3], w * (0.50f + 0.30f * kotlin.math.cos(2f * p3 + 4.4f)), h * (0.20f + 0.25f * kotlin.math.sin(p3 + 1.3f)))
        ).forEach { (color, cx, cy) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

