/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 */

package com.hazender.tropimonlauncher.ui.screens.content

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hazender.tropimonlauncher.R
import com.hazender.tropimonlauncher.context.copyLocalFile
import com.hazender.tropimonlauncher.context.getFileName
import com.hazender.tropimonlauncher.coroutine.Task
import com.hazender.tropimonlauncher.coroutine.TaskSystem
import com.hazender.tropimonlauncher.game.account.Account
import com.hazender.tropimonlauncher.game.account.AccountsManager
import com.hazender.tropimonlauncher.game.account.isMicrosoftAccount
import com.hazender.tropimonlauncher.game.account.isMicrosoftLogging
import com.hazender.tropimonlauncher.game.account.microsoft.MINECRAFT_SERVICES_URL
import com.hazender.tropimonlauncher.game.account.microsoft.MinecraftProfileException
import com.hazender.tropimonlauncher.game.account.microsoft.NotPurchasedMinecraftException
import com.hazender.tropimonlauncher.game.account.microsoft.XboxLoginException
import com.hazender.tropimonlauncher.game.account.microsoft.toLocal
import com.hazender.tropimonlauncher.game.account.microsoftLogin
import com.hazender.tropimonlauncher.game.account.refreshMicrosoft
import com.hazender.tropimonlauncher.game.account.wardrobe.EmptyCape
import com.hazender.tropimonlauncher.game.account.wardrobe.capeTranslatedName
import com.hazender.tropimonlauncher.game.account.wardrobe.validateSkinFile
import com.hazender.tropimonlauncher.game.account.yggdrasil.changeCape
import com.hazender.tropimonlauncher.game.account.yggdrasil.executeWithAuthorization
import com.hazender.tropimonlauncher.game.account.yggdrasil.getPlayerProfile
import com.hazender.tropimonlauncher.game.account.yggdrasil.isUsing
import com.hazender.tropimonlauncher.game.account.yggdrasil.uploadSkin
import com.hazender.tropimonlauncher.path.PathManager
import com.hazender.tropimonlauncher.ui.base.BaseScreen
import com.hazender.tropimonlauncher.ui.components.BackgroundCard
import com.hazender.tropimonlauncher.ui.components.ScalingLabel
import com.hazender.tropimonlauncher.ui.components.SimpleAlertDialog
import com.hazender.tropimonlauncher.ui.components.SimpleListDialog
import com.hazender.tropimonlauncher.ui.screens.NormalNavKey
import com.hazender.tropimonlauncher.ui.screens.content.elements.AccountItem
import com.hazender.tropimonlauncher.ui.screens.content.elements.AccountOperation
import com.hazender.tropimonlauncher.ui.screens.content.elements.LoginItem
import com.hazender.tropimonlauncher.ui.screens.content.elements.MicrosoftChangeCapeOperation
import com.hazender.tropimonlauncher.ui.screens.content.elements.MicrosoftChangeSkinOperation
import com.hazender.tropimonlauncher.ui.screens.content.elements.MicrosoftLoginOperation
import com.hazender.tropimonlauncher.ui.screens.content.elements.MicrosoftLoginTipDialog
import com.hazender.tropimonlauncher.ui.screens.content.elements.SelectSkinModelDialog
import com.hazender.tropimonlauncher.utils.animation.swapAnimateDpAsState
import com.hazender.tropimonlauncher.utils.logging.Logger.lError
import com.hazender.tropimonlauncher.utils.network.safeBodyAsJson
import com.hazender.tropimonlauncher.utils.string.getMessageOrToString
import com.hazender.tropimonlauncher.viewmodel.ErrorViewModel
import com.hazender.tropimonlauncher.viewmodel.ScreenBackStackViewModel
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.UUID

@Composable
fun AccountManageScreen(
    backStackViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    openLink: (url: String) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    var microsoftLoginOperation by remember { mutableStateOf<MicrosoftLoginOperation>(MicrosoftLoginOperation.None) }
    var microsoftChangeSkinOperation by remember { mutableStateOf<MicrosoftChangeSkinOperation>(MicrosoftChangeSkinOperation.None) }
    var microsoftChangeCapeOperation by remember { mutableStateOf<MicrosoftChangeCapeOperation>(MicrosoftChangeCapeOperation.None) }

    BaseScreen(
        screenKey = NormalNavKey.AccountManager,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            ServerTypeMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(all = 12.dp)
                    .weight(3f),
                updateMicrosoftOperation = { microsoftLoginOperation = it }
            )
            AccountsLayout(
                isVisible = isVisible,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                    .weight(7f),
                submitError = submitError,
                onMicrosoftChangeSkin = { account, result ->
                    microsoftChangeSkinOperation = MicrosoftChangeSkinOperation.ImportFile(account, result)
                },
                onMicrosoftChangeCape = { account ->
                    microsoftChangeCapeOperation = MicrosoftChangeCapeOperation.FetchProfiles(account)
                }
            )
        }
    }

    // Gestion de la connexion Microsoft
    MicrosoftLoginOperation(
        checkIfInWebScreen = {
            backStackViewModel.mainScreen.currentKey is NormalNavKey.WebScreen
        },
        navigateToWeb = { url ->
            backStackViewModel.mainScreen.backStack.navigateToWeb(url)
        },
        backToMainScreen = backToMainScreen,
        microsoftLoginOperation = microsoftLoginOperation,
        updateOperation = { microsoftLoginOperation = it },
        openLink = openLink,
        submitError = submitError
    )

    // Gestion du changement de skin Microsoft
    MicrosoftChangeSkinOperation(
        operation = microsoftChangeSkinOperation,
        updateOperation = { microsoftChangeSkinOperation = it },
        submitError = submitError
    )

    // Gestion du changement de cape Microsoft
    MicrosoftChangeCapeOperation(
        operation = microsoftChangeCapeOperation,
        updateOperation = { microsoftChangeCapeOperation = it },
        submitError = submitError
    )
}

@Composable
private fun ServerTypeMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    updateMicrosoftOperation: (MicrosoftLoginOperation) -> Unit
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    BackgroundCard(
        modifier = modifier
            .offset { IntOffset(x = xOffset.roundToPx(), y = 0) }
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp)
        ) {
            LoginItem(
                modifier = Modifier.fillMaxWidth(),
                serverName = stringResource(R.string.account_type_microsoft),
            ) {
                if (!isMicrosoftLogging()) {
                    updateMicrosoftOperation(MicrosoftLoginOperation.Tip)
                }
            }
        }
    }
}

@Composable
private fun MicrosoftLoginOperation(
    checkIfInWebScreen: () -> Boolean,
    navigateToWeb: (url: String) -> Unit,
    backToMainScreen: () -> Unit,
    microsoftLoginOperation: MicrosoftLoginOperation,
    updateOperation: (MicrosoftLoginOperation) -> Unit,
    openLink: (url: String) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current

    when (microsoftLoginOperation) {
        is MicrosoftLoginOperation.None -> {}
        is MicrosoftLoginOperation.Tip -> {
            MicrosoftLoginTipDialog(
                onDismissRequest = { updateOperation(MicrosoftLoginOperation.None) },
                onConfirm = { updateOperation(MicrosoftLoginOperation.RunTask) },
                openLink = openLink
            )
        }
        is MicrosoftLoginOperation.RunTask -> {
            microsoftLogin(
                context = context,
                toWeb = navigateToWeb,
                backToMain = backToMainScreen,
                checkIfInWebScreen = checkIfInWebScreen,
                updateOperation = { updateOperation(it) },
                submitError = submitError
            )
            updateOperation(MicrosoftLoginOperation.None)
        }
    }
}

@Composable
private fun MicrosoftChangeSkinOperation(
    operation: MicrosoftChangeSkinOperation,
    updateOperation: (MicrosoftChangeSkinOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current
    when (operation) {
        is MicrosoftChangeSkinOperation.None -> {}
        is MicrosoftChangeSkinOperation.ImportFile -> {
            val account = operation.account
            val uri = operation.uri

            val fileName = context.getFileName(uri) ?: UUID.randomUUID().toString().replace("-", "")
            val cacheFile = File(PathManager.DIR_IMAGE_CACHE, fileName)

            val importCacheSkin = Task.runTask(
                id = account.uniqueUUID,
                dispatcher = Dispatchers.IO,
                task = {
                    context.copyLocalFile(uri, cacheFile)
                    if (validateSkinFile(cacheFile)) {
                        updateOperation(MicrosoftChangeSkinOperation.SelectSkinModel(account, cacheFile))
                    } else {
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = context.getString(R.string.generic_warning),
                                message = context.getString(R.string.account_change_skin_invalid)
                            )
                        )
                        updateOperation(MicrosoftChangeSkinOperation.None)
                    }
                },
                onError = { th ->
                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = context.getString(R.string.generic_error),
                            message = context.getString(R.string.account_change_skin_failed_to_import) + "\r\n" + th.getMessageOrToString()
                        )
                    )
                    updateOperation(MicrosoftChangeSkinOperation.None)
                },
                onCancel = {
                    updateOperation(MicrosoftChangeSkinOperation.None)
                }
            )

            TaskSystem.submitTask(importCacheSkin)
        }
        is MicrosoftChangeSkinOperation.SelectSkinModel -> {
            val account = operation.account
            val skinFile = operation.file
            SelectSkinModelDialog(
                onDismissRequest = {
                    updateOperation(MicrosoftChangeSkinOperation.None)
                },
                onSelected = { modelType ->
                    updateOperation(
                        MicrosoftChangeSkinOperation.RunTask(
                            account = account,
                            file = skinFile,
                            skinModel = modelType
                        )
                    )
                }
            )
        }
        is MicrosoftChangeSkinOperation.RunTask -> {
            val account = operation.account
            val skinFile = operation.file
            val skinModel = operation.skinModel

            val task = Task.runTask(
                dispatcher = Dispatchers.IO,
                task = { task ->
                    executeWithAuthorization(
                        block = {
                            task.updateProgress(-1f, R.string.account_change_skin_uploading)
                            uploadSkin(
                                apiUrl = MINECRAFT_SERVICES_URL,
                                accessToken = account.accessToken,
                                file = skinFile,
                                modelType = skinModel
                            )
                        },
                        onRefreshRequest = {
                            account.refreshMicrosoft(task = task, coroutineContext = coroutineContext)
                            AccountsManager.suspendSaveAccount(account)
                        }
                    )
                    task.updateMessage(R.string.account_change_skin_update_local)
                    runCatching {
                        account.downloadSkin()
                    }.onFailure { th ->
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = context.getString(R.string.account_logging_in_failed),
                                message = context.formatAccountError(th)
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.account_change_skin_update_toast),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    updateOperation(MicrosoftChangeSkinOperation.None)
                },
                onError = { th ->
                    val (title, message) = when {
                        th is io.ktor.client.plugins.ResponseException -> {
                            val response = th.response
                            val code = response.status.value
                            val body = response.safeBodyAsJson<JsonObject>()
                            val message = body["errorMessage"]?.jsonPrimitive?.contentOrNull
                            context.getString(R.string.account_change_skin_failed_to_upload, code) to (message ?: th.getMessageOrToString())
                        }
                        else -> context.getString(R.string.generic_error) to context.formatAccountError(th)
                    }

                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = title,
                            message = message
                        )
                    )
                    updateOperation(MicrosoftChangeSkinOperation.None)
                },
                onCancel = {
                    updateOperation(MicrosoftChangeSkinOperation.None)
                }
            )

            TaskSystem.submitTask(task)
        }
    }
}

@Composable
private fun MicrosoftChangeCapeOperation(
    operation: MicrosoftChangeCapeOperation,
    updateOperation: (MicrosoftChangeCapeOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current
    when (operation) {
        is MicrosoftChangeCapeOperation.None -> {}
        is MicrosoftChangeCapeOperation.FetchProfiles -> {
            val account = operation.account
            val task = Task.runTask(
                id = account.uniqueUUID,
                dispatcher = Dispatchers.IO,
                task = { task ->
                    executeWithAuthorization(
                        block = {
                            task.updateProgress(-1f, R.string.account_change_cape_fetch_all)
                            val profile = getPlayerProfile(
                                apiUrl = MINECRAFT_SERVICES_URL,
                                accessToken = account.accessToken
                            )
                            updateOperation(MicrosoftChangeCapeOperation.SelectCape(account, profile))
                        },
                        onRefreshRequest = {
                            account.refreshMicrosoft(task = task, coroutineContext = coroutineContext)
                            AccountsManager.suspendSaveAccount(account)
                        }
                    )
                },
                onError = { th ->
                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = context.getString(R.string.generic_error),
                            message = context.getString(R.string.account_change_cape_fetch_all_failed) + "\r\n" + th.getMessageOrToString()
                        )
                    )
                    updateOperation(MicrosoftChangeCapeOperation.None)
                },
                onCancel = {
                    updateOperation(MicrosoftChangeCapeOperation.None)
                }
            )
            TaskSystem.submitTask(task)
        }
        is MicrosoftChangeCapeOperation.SelectCape -> {
            val account = operation.account
            val profile = operation.profile

            val capes = remember(profile.capes) {
                listOf(EmptyCape) + profile.capes
            }

            SimpleListDialog(
                title = stringResource(R.string.account_change_cape_select_cape),
                items = capes,
                itemTextProvider = { cape ->
                    cape.capeTranslatedName()
                },
                onItemSelected = { cape ->
                    updateOperation(MicrosoftChangeCapeOperation.RunTask(account, cape))
                },
                isCurrent = { cape ->
                    cape.isUsing()
                },
                onDismissRequest = { selected ->
                    if (!selected) {
                        updateOperation(MicrosoftChangeCapeOperation.None)
                    }
                }
            )
        }
        is MicrosoftChangeCapeOperation.RunTask -> {
            val account = operation.account
            val cape = operation.cape
            val capeName = cape.capeTranslatedName()
            val capeId: String? = cape.takeIf { it != EmptyCape }?.id

            val task = Task.runTask(
                dispatcher = Dispatchers.IO,
                task = { task ->
                    executeWithAuthorization(
                        block = {
                            task.updateMessage(R.string.account_change_cape_apply)
                            changeCape(
                                apiUrl = MINECRAFT_SERVICES_URL,
                                accessToken = account.accessToken,
                                capeId = capeId
                            )
                        },
                        onRefreshRequest = {
                            account.refreshMicrosoft(task = task, coroutineContext = coroutineContext)
                            AccountsManager.suspendSaveAccount(account)
                        }
                    )
                    withContext(Dispatchers.Main) {
                        val text = if (cape == EmptyCape) {
                            context.getString(R.string.account_change_cape_apply_reset)
                        } else {
                            context.getString(R.string.account_change_cape_apply_success, capeName)
                        }

                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                    updateOperation(MicrosoftChangeCapeOperation.None)
                },
                onError = { th ->
                    val (title, message) = when {
                        th is io.ktor.client.plugins.ResponseException -> {
                            val response = th.response
                            val code = response.status.value
                            val body = response.safeBodyAsJson<JsonObject>()
                            val message = body["errorMessage"]?.jsonPrimitive?.contentOrNull
                            context.getString(R.string.account_change_cape_apply_failed, code) to (message ?: th.getMessageOrToString())
                        }
                        else -> context.getString(R.string.generic_error) to context.formatAccountError(th)
                    }

                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = title,
                            message = message
                        )
                    )
                    updateOperation(MicrosoftChangeCapeOperation.None)
                },
                onCancel = {
                    updateOperation(MicrosoftChangeCapeOperation.None)
                }
            )
            TaskSystem.submitTask(task)
        }
    }
}

@Composable
private fun AccountsLayout(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    onMicrosoftChangeSkin: (Account, Uri) -> Unit,
    onMicrosoftChangeCape: (Account) -> Unit
) {
    val yOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    val context = LocalContext.current

    val accounts by AccountsManager.accountsFlow.collectAsState()
    val currentAccount by AccountsManager.currentAccountFlow.collectAsState()

    var accountOperation by remember { mutableStateOf<AccountOperation>(AccountOperation.None) }
    AccountOperation(
        accountOperation = accountOperation,
        updateAccountOperation = { accountOperation = it },
        submitError = submitError
    )

    BackgroundCard(
        modifier = modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        if (accounts.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = MaterialTheme.shapes.extraLarge),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(accounts) { account ->
                    var refreshAvatar by remember { mutableStateOf(false) }

                    val skinPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        uri?.let { result ->
                            if (account.isMicrosoftAccount()) {
                                onMicrosoftChangeSkin(account, result)
                            }
                        }
                    }

                    AccountItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        currentAccount = currentAccount,
                        account = account,
                        refreshKey = refreshAvatar,
                        onSelected = { acc ->
                            AccountsManager.setCurrentAccount(acc)
                        },
                        onChangeSkin = {
                            skinPicker.launch(arrayOf("image/*"))
                        },
                        onChangeCape = {
                            onMicrosoftChangeCape(account)
                        },
                        onResetSkin = {},
                        onRefreshClick = {
                            AccountsManager.refreshAccount(
                                context = context,
                                account = account,
                                onFailed = { th ->
                                    accountOperation = AccountOperation.OnFailed(th)
                                }
                            )
                        },
                        onDeleteClick = { accountOperation = AccountOperation.Delete(account) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                ScalingLabel(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.account_no_account)
                )
            }
        }
    }
}

@Composable
private fun AccountOperation(
    accountOperation: AccountOperation,
    updateAccountOperation: (AccountOperation) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current
    when (accountOperation) {
        is AccountOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_delete_title),
                text = stringResource(R.string.account_delete_message,
                    accountOperation.account.username),
                onConfirm = {
                    AccountsManager.deleteAccount(accountOperation.account)
                    updateAccountOperation(AccountOperation.None)
                },
                onDismiss = { updateAccountOperation(AccountOperation.None) }
            )
        }
        is AccountOperation.OnFailed -> {
            val message: String = context.formatAccountError(accountOperation.th)

            submitError(
                ErrorViewModel.ThrowableMessage(
                    title = stringResource(R.string.account_logging_in_failed),
                    message = message
                )
            )
            updateAccountOperation(AccountOperation.None)
        }
        is AccountOperation.None -> {}
    }
}

private fun Context.formatAccountError(th: Throwable) = when (th) {
    is NotPurchasedMinecraftException -> toLocal(this)
    is MinecraftProfileException -> th.toLocal(this)
    is XboxLoginException -> th.toLocal(this)
    is HttpRequestTimeoutException -> getString(R.string.error_timeout)
    is UnknownHostException, is UnresolvedAddressException -> getString(R.string.error_network_unreachable)
    is ConnectException -> getString(R.string.error_connection_failed)
    is io.ktor.client.plugins.ResponseException -> {
        val statusCode = th.response.status
        val res = when (statusCode) {
            HttpStatusCode.Unauthorized -> R.string.error_unauthorized
            HttpStatusCode.NotFound -> R.string.error_notfound
            else -> R.string.error_client_error
        }
        getString(res, statusCode)
    }
    else -> {
        lError("An unknown exception was caught!", th)
        val errorMessage = th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
        getString(R.string.error_unknown, errorMessage)
    }
}