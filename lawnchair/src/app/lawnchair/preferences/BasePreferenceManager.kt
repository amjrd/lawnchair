/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.lawnchair.font.FontCache
import app.lawnchair.MSDLPlayer
import app.lawnchair.feedback.FeedbackLevel
import app.lawnchair.util.getApkVersionComparison
import app.lawnchair.util.isGestureNavContractCompatible
import app.lawnchair.util.isOnePlusStock
import com.android.launcher3.BuildConfig
import com.android.launcher3.InvariantDeviceProfile.INDEX_DEFAULT
import com.android.launcher3.InvariantDeviceProfile.INDEX_TWO_PANEL_PORTRAIT
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.model.DeviceGridState
import com.android.launcher3.util.ComponentKey
import com.android.launcher3.util.DaggerSingletonObject
import com.android.launcher3.util.SafeCloseable
import com.patrykkobylarz.lawnchair.dagger.LauncherAppComponent
import javax.inject.Inject

class PreferenceManager @Inject constructor(
    private val context: Context,
) : BasePreferenceManager(context),
    SafeCloseable,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val reloadIcons = { idp.onPreferencesChanged(context) }
    private val reloadGrid = { idp.onPreferencesChanged(context) }
    private val recreate = { context.recreateIfNotScheduled() }

    val iconPackPackage = StringPref("pref_iconPackPackage", "", reloadIcons)
    val hiddenAppSet = StringSetPref("pref_hidden_app_set", Collections.emptySet())
    val hiddenFolderAppSet = StringSetPref("pref_hidden_folder_app_set", Collections.emptySet())
    val isItemDragEnabled = BoolPref("pref_isItemDragEnabled", true)
    val iconSizeFactor = FloatPref("pref_iconSizeFactor", 1f, reloadIcons)
    val textSizeFactor = FloatPref("pref_textSizeFactor", 1f, reloadGrid)

    val workspaceColumns = IdpIntPref("pref_workspaceColumns", { numColumns }, reloadGrid)
    val workspaceRows = IdpIntPref("pref_workspaceRows", { numRows }, reloadGrid)
    val hotseatColumns = IdpIntPref("pref_hotseatColumns", { numDatabaseHotseatIcons }, reloadGrid)

    val workspaceColumnsUnfolded = IdpIntPref(
        "pref_workspaceColumns_unfolded",
        {
            if (context.resources.getBoolean(R.bool.allow_rotation)) {
                numColumns
            } else {
                numColumns * 2
            }
        },
        reloadGrid,
    )
    val workspaceRowsUnfolded = IdpIntPref(
        "pref_workspaceRows_unfolded",
        {
            if (context.resources.getBoolean(R.bool.allow_rotation)) {
                numRows
            } else {
                numRows / 2
            }
        },
        reloadGrid,
    )
    val hotseatColumnsUnfolded = IdpIntPref(
        "pref_hotseatColumns_unfolded",
        {
            if (context.resources.getBoolean(R.bool.allow_rotation)) {
                numDatabaseHotseatIcons
            } else {
                numDatabaseHotseatIcons * 2
            }
        },
        reloadGrid,
    )

    val folderColumns = IdpIntPref("pref_folderColumns", { numFolderColumns[INDEX_DEFAULT] }, reloadGrid)
    val folderColumnsTwoPanelPortrait = IdpIntPref("pref_folderColumns_two_panel_portrait", { numFolderColumns[INDEX_TWO_PANEL_PORTRAIT] }, reloadGrid)

    val workspaceIncreaseMaxGridSize = BoolPref("pref_workspace_increase_max_grid_size", false)
    val folderRows = IdpIntPref("pref_folderRows", { numFolderRows[INDEX_DEFAULT] }, reloadGrid)
    val drawerOpacity = FloatPref("pref_drawerOpacity", .5f, recreate)
    val coloredBackgroundLightness = FloatPref("pref_coloredBackgroundLightness", 1F)
    val feedProvider = StringPref("pref_feedProvider", "")
    val ignoreFeedWhitelist = BoolPref("pref_ignoreFeedWhitelist", false)
    val launcherTheme = StringPref("pref_launcherTheme", "system")
    val overrideWindowCornerRadius = BoolPref("pref_overrideWindowCornerRadius", false, recreate)
    val windowCornerRadius = IntPref("pref_windowCornerRadius", 80, recreate)
    val autoLaunchRoot = BoolPref("pref_autoLaunchRoot", false)
    val wallpaperScrolling = BoolPref("pref_wallpaperScrolling", true)
    val infiniteScrolling = BoolPref("pref_infiniteScrolling", false)
    val enableDebugMenu = BoolPref("pref_enableDebugMenu", false)

    val vibrationFeedbackLevel: IntPref = IntPref(
        "pref_vibrationFeedbackLevel",
        FeedbackLevel.DEFAULT.ordinal,
    ) {
        normalizeVibrationFeedbackLevel()
    }

    val customAppName = object : MutableMapPref<ComponentKey, String>("pref_appNameMap", reloadGrid) {
        override fun flattenKey(key: ComponentKey) = key.toString()
        override fun unflattenKey(key: String) = ComponentKey.fromString(key)!!
        override fun flattenValue(value: String) = value
        override fun unflattenValue(value: String) = value
    }

    val recentActionOrder = StringPref("pref_recentActionOrder", "0,1,2,3,4", recreate)

    private val fontCache = FontCache.INSTANCE.get(context)
    val fontWorkspace = FontPref("pref_workspaceFont", fontCache.uiText, recreate)
    val fontHeading = FontPref("pref_fontHeading", fontCache.uiRegular, recreate)
    val fontHeadingMedium = FontPref("pref_fontHeadingMedium", fontCache.uiMedium, recreate)
    val fontBody = FontPref("pref_fontBody", fontCache.uiText, recreate)
    val fontBodyMedium = FontPref("pref_fontBodyMedium", fontCache.uiTextMedium, recreate)

    // TODO REMOVE
    val deviceSearch = BoolPref("device_search", false, recreate)
    val searchResultShortcuts = BoolPref("pref_searchResultShortcuts", false)
    val searchResultPeople = BoolPref("pref_searchResultPeople", false, recreate)
    val searchResultPixelTips = BoolPref("pref_searchResultPixelTips", false)
    val searchResultSettings = BoolPref("pref_searchResultSettings", false)
    val searchResultCalculator = BoolPref("pref_searchResultCalculator", false)
    val searchResultApps = BoolPref("pref_searchResultApps", true, recreate)
    val searchResultFilesToggle = BoolPref("pref_searchResultFiles", false, recreate)
    val searchResultAllFiles = BoolPref("pref_searchResultAllFiles", false, recreate)
    val searchResultAudio = BoolPref("pref_searchResultAudio", false, recreate)
    val searchResultVisualMedia = BoolPref("pref_searchResultVisualMedia", false, recreate)
    val searchResultStartPageSuggestion = BoolPref("pref_searchResultStartPageSuggestion", true, recreate)
    val searchResultSettingsEntry = BoolPref("pref_searchResultSettingsEntry", false, recreate)
    val searchResulRecentSuggestion = BoolPref("pref_searchResultRecentSuggestion", false, recreate)

    val themedIcons = BoolPref("themed_icons", false, reloadIcons)
    val drawerThemedIcons = BoolPref("drawer_themed_icons", false, reloadIcons)

    val hotseatQsbCornerRadius = FloatPref("pref_hotseatQsbCornerRadius", 1F, recreate)
    val hotseatQsbAlpha = IntPref("pref_searchHotseatTranparency", 100, recreate)
    val hotseatQsbStrokeWidth = FloatPref("pref_searchStrokeWidth", 0F, recreate)
    val hotseatBG = BoolPref("pref_hotseatBG", true, recreate)
    val hotseatBGHorizontalInsetLeft = IntPref("pref_hotseatBGHRinsetLeft", 45, recreate)
    val hotseatBGVerticalInsetTop = IntPref("pref_hotseatBGVRinsetTop", 10, recreate)
    val hotseatBGHorizontalInsetRight = IntPref("pref_hotseatBGHRinsetRight", 45, recreate)
    val hotseatBGVerticalInsetBottom = IntPref("pref_hotseatBGVRinsetBottom", 25, recreate)

    val hotseatBGAlpha = IntPref("pref_hotseatBGTransparency", 45, recreate)

    val enableWallpaperBlur = BoolPref("pref_enableWallpaperBlur", false, recreate)
    val wallpaperBlur = IntPref("pref_wallpaperBlur", 25, recreate)
    val wallpaperBlurFactorThreshold = FloatPref("pref_wallpaperBlurFactor", 3.0F, recreate)

    val drawerList = BoolPref("pref_drawerList", true, recreate)
    val folderApps = BoolPref("pref_hideFolderApps", true, reloadGrid)

    val recentsActionScreenshot = BoolPref("pref_recentsActionScreenshot", !isOnePlusStock)
    val recentsActionShare = BoolPref("pref_recentsActionShare", isOnePlusStock)
    val recentsActionLens = BoolPref("pref_recentsActionLens", true)
    val recentsActionClearAll = BoolPref("pref_clearAllAsAction", false)
    val recentsActionLocked = BoolPref("pref_lockedAsAction", false)

    val recentsTranslucentBackground = BoolPref("pref_recentsTranslucentBackground", false, recreate)
    val recentsTranslucentBackgroundAlpha = FloatPref("pref_recentTranslucentBackgroundAlpha", .8f, recreate)

    val hideVersionInfo = BoolPref("pref_hideVersionInfo", false)
    val pseudonymVersion = StringPref("pref_pseudonymVersion", "Bubble Tea")

    val enableGnc = BoolPref("pref_enableGnc", isGestureNavContractCompatible, recreate)
    val hasOpenedSettings = BoolPref("pref_hasOpenedSettings", false)

    val lawnchairMajorVersion = IntPref(
        "pref_lawnchairMajorVersion",
        context.getApkVersionComparison().first[0],
    )

    val forceIconMonochrome = BoolPref("pref_forceIconMonochrome", false)

    override fun close() {
        TODO("Not yet implemented")
    }

    private fun normalizeVibrationFeedbackLevel() {
        val storedLevel = vibrationFeedbackLevel.get()
        val normalizedLevel = storedLevel.toFeedbackLevel().ordinal
        if (storedLevel != normalizedLevel) {
            vibrationFeedbackLevel.set(normalizedLevel)
        }
        MSDLPlayer.SYSTEM_FEEDBACK_LEVEL = FeedbackLevel.entries[normalizedLevel]
    }

    init {
        sp.registerOnSharedPreferenceChangeListener(this)
        migratePrefs(CURRENT_VERSION) { oldVersion ->
            if (oldVersion < 2) {
                val gridState = DeviceGridState(context).toProtoMessage()
                if (gridState.hotseatCount != -1) {
                    val colsAndRows = gridState.gridSize.split(",")
                    workspaceColumns.set(colsAndRows[0].toInt())
                    workspaceRows.set(colsAndRows[1].toInt())
                    hotseatColumns.set(gridState.hotseatCount)
                }
            }
        }
        normalizeVibrationFeedbackLevel()
    }

    companion object {
        private const val CURRENT_VERSION = 2

        @JvmField
        val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getPreferenceManager)

        @JvmStatic
        fun getInstance(context: Context) = INSTANCE.get(context)!!
    }
}

private fun Int.toFeedbackLevel() = FeedbackLevel.entries.getOrNull(this) ?: FeedbackLevel.DEFAULT

@Composable
fun preferenceManager() = PreferenceManager.getInstance(LocalContext.current)

/**
 * Grid layout configuration for a device's workspace.
 *
 * @param hotseatColumns The amount of column the dock can contain
 * @param workspaceColumns The amount of column the home screen can contain
 * @param workspaceRows The amount of row the home screen can contain
 * @param hotseatColumnsUnfolded The amount of column the dock can contain when unfolded (foldables only)
 */
data class LayoutConfig(
    /**
     * Hotseat columns refer to the amount of column the dock can contain.
     * For foldables, this is the folded (closed) state.
     */
    val hotseatColumns: Int,
    /**
     * Workspace columns refer to the amount of column the home screen can contain.
     */
    val workspaceColumns: Int,
    /**
     * Workspace rows refer to the amount of row the home screen can contain.
     */
    val workspaceRows: Int,
    /**
     * Hotseat columns when the foldable is in unfolded (opened) state.
     * For non-foldable devices, this must be equals to [hotseatColumns].
     */
    val hotseatColumnsUnfolded: Int = hotseatColumns,
)
