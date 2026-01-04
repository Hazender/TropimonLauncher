/*
 * Zalith Launcher 2
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

package com.hazender.tropimonlauncher.ui.screens.content

import android.content.Context
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.hazender.tropimonlauncher.BuildConfig
import com.hazender.tropimonlauncher.R
import com.hazender.tropimonlauncher.coroutine.TaskState
import com.hazender.tropimonlauncher.coroutine.TaskSystem
import com.hazender.tropimonlauncher.coroutine.TropiTaskSystem
import com.hazender.tropimonlauncher.game.account.AccountsManager
import com.hazender.tropimonlauncher.ui.components.ServerStatusIndicator
import com.hazender.tropimonlauncher.game.download.game.TropiInstallManager
import com.hazender.tropimonlauncher.game.launch.LaunchGame
import com.hazender.tropimonlauncher.game.version.installed.Version
import com.hazender.tropimonlauncher.game.version.installed.VersionsManager
import com.hazender.tropimonlauncher.info.InfoDistributor
import com.hazender.tropimonlauncher.ui.base.BaseScreen
import com.hazender.tropimonlauncher.ui.components.BackgroundCard
import com.hazender.tropimonlauncher.ui.components.MarqueeText
import com.hazender.tropimonlauncher.ui.components.ScalingActionButton
import com.hazender.tropimonlauncher.ui.screens.NestedNavKey
import com.hazender.tropimonlauncher.ui.screens.NormalNavKey
import com.hazender.tropimonlauncher.ui.screens.content.elements.AccountAvatar
import com.hazender.tropimonlauncher.utils.animation.swapAnimateDpAsState
import com.hazender.tropimonlauncher.viewmodel.LaunchGameViewModel
import com.hazender.tropimonlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    launchGameViewModel: LaunchGameViewModel
) {
    val context = LocalContext.current
    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            ContentMenu(
                isVisible = isVisible,
                modifier = Modifier.weight(7f)
            )

            RightMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                launchGameViewModel = launchGameViewModel,
                context = context,
                toAccountManageScreen = {
                    backStackViewModel.mainScreen.navigateTo(NormalNavKey.AccountManager)
                },
                toVersionManageScreen = {
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        remove = NestedNavKey.VersionSettings::class,
                        screenKey = NormalNavKey.VersionsManager
                    )
                },
                toVersionSettingsScreen = {
                    VersionsManager.currentVersion?.let { version ->
                        navigateToVersions(version)
                    }
                }
            )
        }
    }
}

@Composable
private fun ContentMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val yOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
    ) {
        if (BuildConfig.DEBUG) {
            BackgroundCard(
                modifier = Modifier.padding(all = 12.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.generic_warning),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.launcher_version_debug_warning, InfoDistributor.LAUNCHER_NAME),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        modifier = Modifier
                            .alpha(0.8f)
                            .align(Alignment.End),
                        text = stringResource(R.string.launcher_version_debug_warning_cant_close),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
@Composable
private fun RightMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    launchGameViewModel: LaunchGameViewModel,
    context: Context,
    toAccountManageScreen: () -> Unit = {},
    toVersionManageScreen: () -> Unit = {},
    toVersionSettingsScreen: () -> Unit = {}
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = 40.dp,
        swapIn = isVisible,
        isHorizontal = true
    )
    val isInstalling by TropiInstallManager.isInstalling.collectAsState()
    val isUpdating by TropiInstallManager.isUpdating.collectAsState()
    val isChecking by TropiInstallManager.isChecking.collectAsState()

    var isLaunchingGame by remember { mutableStateOf(false) }

    val tasks by combine(
        TaskSystem.tasksFlow,
        TropiTaskSystem.tasksFlow
    ) { normalTasks, tropiTasks ->
        when {
            tropiTasks.any { it.taskState == TaskState.PREPARING || it.taskState == TaskState.RUNNING } -> tropiTasks
            normalTasks.any { it.taskState == TaskState.PREPARING || it.taskState == TaskState.RUNNING } -> normalTasks
            else -> emptyList()
        }
    }.collectAsState(initial = emptyList())

    val hasActiveTask = tasks.any { it.taskState == TaskState.PREPARING || it.taskState == TaskState.RUNNING }
    val isRealGameLaunch = LaunchGame.isLaunchInProgress()

    LaunchedEffect(hasActiveTask, isRealGameLaunch, isChecking, isUpdating, isLaunchingGame) {
        if (isLaunchingGame && !hasActiveTask && !isChecking && !isUpdating && !isRealGameLaunch) {
            delay(500)
            isLaunchingGame = false
        }
    }

    BackgroundCard(
        modifier = modifier.offset { IntOffset(x = xOffset.roundToPx(), y = 0) },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        val account by AccountsManager.currentAccountFlow.collectAsState()
        val version = VersionsManager.currentVersion

        key(version) {
            ConstraintLayout(
                modifier = Modifier.fillMaxSize()
            ) {
                val (serverStatus, accountAvatar, versionManagerLayout, launchButton) = createRefs()

                // Indicateur de statut du serveur
                ServerStatusIndicator(
                    modifier = Modifier
                        .constrainAs(serverStatus) {
                            top.linkTo(parent.top, margin = 8.dp)
                            centerHorizontallyTo(parent)
                        },
                )

                AccountAvatar(
                    modifier = Modifier.constrainAs(accountAvatar) {
                        top.linkTo(parent.top, margin = 8.dp)
                        bottom.linkTo(launchButton.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                    account = account,
                    onClick = toAccountManageScreen
                )

                VersionManagerLayout(
                    version = version,
                    modifier = Modifier
                        .constrainAs(versionManagerLayout) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(launchButton.top)
                        }
                    )
                ScalingActionButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .constrainAs(launchButton) {
                            bottom.linkTo(parent.bottom, margin = 8.dp)
                        }
                        .padding(PaddingValues(horizontal = 12.dp)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    enabled = !isInstalling && !isUpdating && !isChecking && !hasActiveTask && !isLaunchingGame && !isRealGameLaunch,
                    onClick = {
                        isLaunchingGame = true
                        TropiInstallManager.checkAndUpdateAutomatic(
                            context = context,
                            version = version,
                            onComplete = {
                                val currentVer = VersionsManager.currentVersion
                                if (currentVer != null) {
                                    launchGameViewModel.tryLaunch(currentVer)
                                } else {
                                    isLaunchingGame = false
                                }
                            },
                            onError = {
                                it.printStackTrace()
                                isLaunchingGame = false
                            }
                        )
                    }
                ) {
                    @Composable
                    fun LoadingRow(text: String) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            MarqueeText(text = text)
                        }
                    }
                    when {
                        isInstalling -> LoadingRow(stringResource(R.string.download_tropimon_installation_in_progress))
                        isChecking -> LoadingRow(stringResource(R.string.download_tropimon_checking_updates))
                        isUpdating -> LoadingRow(stringResource(R.string.download_tropimon_updating_progress))
                        isLaunchingGame -> LoadingRow(stringResource(R.string.main_launch_game_preparing))
                        else -> MarqueeText(
                            text = if (version == null)
                                stringResource(R.string.download_tropimon_install_tropimon)
                            else
                                stringResource(R.string.main_launch_game)
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun VersionManagerLayout(
    version: Version?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
    ) {
        if (VersionsManager.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(all = 2.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (version == null) {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = stringResource(R.string.versions_manage_no_versions),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                } else {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = version.getVersionName(),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}