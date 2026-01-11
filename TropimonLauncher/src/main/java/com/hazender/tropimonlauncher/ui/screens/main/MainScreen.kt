/*
 * Tropimon Launcher
 * Copyright (C) 2025 Hazender
 *
 * Based on Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.hazender.tropimonlauncher.ui.screens.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.automirrored.rounded.ArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.hazender.tropimonlauncher.R
import com.hazender.tropimonlauncher.coroutine.Task
import com.hazender.tropimonlauncher.coroutine.TaskState
import com.hazender.tropimonlauncher.coroutine.TaskSystem
import com.hazender.tropimonlauncher.coroutine.TropiTaskSystem
import com.hazender.tropimonlauncher.game.download.game.TropiInstallManager
import com.hazender.tropimonlauncher.game.version.installed.Version
import com.hazender.tropimonlauncher.game.version.installed.VersionsManager
import com.hazender.tropimonlauncher.setting.AllSettings
import com.hazender.tropimonlauncher.ui.components.*
import com.hazender.tropimonlauncher.ui.screens.*
import com.hazender.tropimonlauncher.ui.screens.content.*
import com.hazender.tropimonlauncher.utils.animation.getAnimateTween
import com.hazender.tropimonlauncher.viewmodel.*
import kotlinx.coroutines.flow.combine
import androidx.core.net.toUri
import com.hazender.tropimonlauncher.game.server.ServerConfigViewModel

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    launchGameViewModel: LaunchGameViewModel,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // ViewModel centralisé pour toute la config serveur
    val serverConfigViewModel: ServerConfigViewModel = viewModel()
    val serverConfig by serverConfigViewModel.config.collectAsState()

    val tasks by combine(
        TaskSystem.tasksFlow,
        TropiTaskSystem.tasksFlow
    ) { normalTasks, tropiTasks ->
        when {
            tropiTasks.any { it.taskState == TaskState.PREPARING || it.taskState == TaskState.RUNNING } ->
                tropiTasks
            normalTasks.any { it.taskState == TaskState.PREPARING || it.taskState == TaskState.RUNNING } ->
                normalTasks
            else -> emptyList()
        }
    }.collectAsState(initial = emptyList())
    val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val toVersionSettings: () -> Unit = {
        VersionsManager.currentVersion?.let { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
    }

    val isBackgroundValid = LocalBackgroundViewModel.current?.isValid == true
    val launcherBackgroundOpacity = AllSettings.launcherBackgroundOpacity.state.toFloat() / 100f
    val topBarColor = MaterialTheme.colorScheme.surfaceContainer
    val backgroundColor = MaterialTheme.colorScheme.surface
    val currentKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = currentKey == null || currentKey is NormalNavKey.LauncherMain
    val surfaceAlpha = if (inLauncherScreen) 0f else 0.5f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor.copy(alpha = surfaceAlpha),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                color = if (isBackgroundValid) topBarColor.copy(alpha = launcherBackgroundOpacity) else topBarColor,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (0.5).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val shadowColor = Color.Black.copy(alpha = 0.4f)
                        val shadowBlur = 0.2.dp
                        val shadowOffset = 0.2.dp

                        Image(
                            painter = painterResource(id = R.drawable.sealcircle),
                            contentDescription = null,
                            modifier = Modifier
                                .height(36.dp)
                                .offset(x = shadowOffset, y = shadowOffset)
                                .blur(shadowBlur),
                            colorFilter = ColorFilter.tint(shadowColor)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.tropimon_text),
                            contentDescription = null,
                            modifier = Modifier
                                .height(34.dp)
                                .offset(x = shadowOffset, y = shadowOffset)
                                .blur(shadowBlur),
                            colorFilter = ColorFilter.tint(shadowColor)
                        )
                    }

                    TopBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        mainScreenKey = screenBackStackModel.mainScreen.currentKey,
                        taskRunning = tasks.isEmpty(),
                        isTasksExpanded = isTaskMenuExpanded,
                        color = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onScreenBack = { screenBackStackModel.mainScreen.backStack.removeFirstOrNull() },
                        toMainScreen = toMainScreen,
                        toSettingsScreen = {
                            screenBackStackModel.mainScreen.removeAndNavigateTo(
                                removes = screenBackStackModel.clearBeforeNavKeys,
                                screenKey = screenBackStackModel.settingsScreen
                            )
                        },
                        toVersionSettings = toVersionSettings,
                        onDiscordClick = {
                            val url = serverConfig?.links?.discord ?: "https://discord.gg/tropimon"
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    url.toUri()
                                ).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.OpenLink(url)
                                )
                            }
                        },
                        onShopClick = {
                            val url = serverConfig?.links?.shop ?: "https://tropimon.fr/shop/categories/credits"
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    url.toUri()
                                ).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.OpenLink(url)
                                )
                            }
                        }
                    ) {
                        changeTasksExpandedState()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                NavigationUI(
                    modifier = Modifier.fillMaxSize(),
                    screenBackStackModel = screenBackStackModel,
                    toMainScreen = toMainScreen,
                    launchGameViewModel = launchGameViewModel,
                    eventViewModel = eventViewModel,
                    submitError = submitError
                )

                TaskMenu(
                    tasks = tasks,
                    isExpanded = isTaskMenuExpanded,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .align(Alignment.CenterStart)
                        .padding(all = 6.dp)
                ) {
                    changeTasksExpandedState()
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    mainScreenKey: NavKey?,
    taskRunning: Boolean,
    isTasksExpanded: Boolean,
    modifier: Modifier = Modifier,
    color: Color,
    contentColor: Color,
    onScreenBack: () -> Unit,
    toMainScreen: () -> Unit,
    toSettingsScreen: () -> Unit,
    toVersionSettings: () -> Unit,
    onDiscordClick: () -> Unit = {},
    onShopClick: () -> Unit = {},
    changeExpandedState: () -> Unit = {}
) {
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain
    val inVersionSettingsScreen = mainScreenKey is NestedNavKey.VersionSettings
    val inSettingsScreen = mainScreenKey is NestedNavKey.Settings
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        tonalElevation = 0.dp
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (backCenter, title, endButtons) = createRefs()

            Row(
                modifier = Modifier
                    .constrainAs(backCenter) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .fillMaxHeight()
            ) {
                // Boutons Discord et Boutique (visible uniquement sur la page principale)
                AnimatedVisibility(visible = inLauncherScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy((-8).dp)
                    ) {
                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = onDiscordClick
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_discord),
                                contentDescription = "Discord",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = onShopClick
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shop),
                                contentDescription = "Shop",
                                modifier = Modifier
                                    .size(22.dp)
                                    .offset(y = (-2).dp)
                            )
                        }
                    }
                }

                // Boutons Retour et Accueil (visible uniquement hors page principale)
                AnimatedVisibility(visible = !inLauncherScreen) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy((-12).dp)
                    ) {
                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = {
                                if (!inLauncherScreen) {
                                    backDispatcher?.onBackPressed() ?: onScreenBack()
                                }
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                                contentDescription = stringResource(R.string.generic_back)
                            )
                        }

                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = { if (!inLauncherScreen) toMainScreen() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = stringResource(R.string.generic_main_menu)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .constrainAs(title) { centerTo(parent) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sealcircle),
                    contentDescription = null,
                    modifier = Modifier.height(36.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.tropimon_text),
                    contentDescription = null,
                    modifier = Modifier.height(34.dp)
                )
            }

            Row(
                modifier = Modifier
                    .constrainAs(endButtons) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end, margin = 12.dp)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = !(isTasksExpanded || taskRunning)) {
                    Row(
                        modifier = Modifier
                            .clip(shape = MaterialTheme.shapes.large)
                            .clickable { changeExpandedState() }
                            .padding(all = 8.dp)
                            .width(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(modifier = Modifier.weight(1f))
                        Icon(
                            modifier = Modifier.size(22.dp),
                            imageVector = Icons.Filled.Task,
                            contentDescription = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                TopBarRailItem(
                    selected = inVersionSettingsScreen,
                    icon = Icons.Filled.Extension,
                    text = stringResource(R.string.mods_manage),
                    onClick = { if (!inVersionSettingsScreen) toVersionSettings() },
                    color = contentColor
                )
                TopBarRailItem(
                    selected = inSettingsScreen,
                    icon = Icons.Filled.Settings,
                    text = stringResource(R.string.generic_setting),
                    onClick = { if (!inSettingsScreen) toSettingsScreen() },
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun TopBarRailItem(
    selected: Boolean,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    color: Color = MaterialTheme.colorScheme.onSurface,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium
) {
    TextRailItem(
        modifier = modifier,
        onClick = onClick,
        text = {
            AnimatedVisibility(visible = selected) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = text,
                        style = textStyle
                    )
                }
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        },
        selected = selected,
        selectedPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        unSelectedPadding = PaddingValues(all = 8.dp),
        unselectedContentColor = color
    )
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    launchGameViewModel: LaunchGameViewModel,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }

        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        launchGameViewModel = launchGameViewModel
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> {
                    AccountManageScreen(
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = {
                            screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
                        },
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        launchGameViewModel = launchGameViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    tasks: List<Task>,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val activeTasks = tasks.filter {
        it.taskState == TaskState.PREPARING || it.taskState == TaskState.RUNNING
    }
    val show = isExpanded && activeTasks.isNotEmpty()

    AnimatedVisibility(
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = true,
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.65f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column {
                CardTitleLayout(
                    alpha = 0.7f,
                    color = MaterialTheme.colorScheme.surfaceBright,
                    influencedByBackground = false
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowLeft,
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(
                        items = activeTasks,
                        key = { it.id }
                    ) { task ->
                        TaskItem(
                            taskProgress = task.currentProgress,
                            taskMessageRes = task.currentMessageRes,
                            taskMessageArgs = task.currentMessageArgs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            onCancelClick = {
                                TaskSystem.cancelTask(task.id)
                                TropiInstallManager.cancelInstallation()
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun TaskItem(
    taskProgress: Float,
    taskMessageRes: Int?,
    taskMessageArgs: Array<out Any>?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceBright,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 0.dp,
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color.copy(alpha = 0.7f),
        contentColor = contentColor,
        shadowElevation = shadowElevation,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .animateContentSize(animationSpec = getAnimateTween())
            ) {
                taskMessageRes?.let { messageRes ->
                    Text(
                        text = if (taskMessageArgs != null) {
                            stringResource(messageRes, *taskMessageArgs)
                        } else {
                            stringResource(messageRes)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { taskProgress },
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        )
                        Text(
                            text = "${(taskProgress * 100).toInt()}%",
                            modifier = Modifier.align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}