package com.kunzisoft.keepass.education

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.core.view.allViews
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSpecialMode
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.settings.PreferencesUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeatureOverviewTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName).apply {
            firstInstallTime = 1000L
            lastUpdateTime = firstInstallTime
        }
    }

    private fun launcherIntent() = Intent(Intent.ACTION_MAIN)
        .setClass(context, FileDatabaseSelectActivity::class.java)
        .addCategory(Intent.CATEGORY_LAUNCHER)

    private fun launch(intent: Intent = launcherIntent()) =
        Robolectric.buildActivity(FileDatabaseSelectActivity::class.java, intent).setup()

    private fun FileDatabaseSelectActivity.overviews(): List<FeatureOverviewDialogFragment> {
        supportFragmentManager.executePendingTransactions()
        return supportFragmentManager.fragments.filterIsInstance<FeatureOverviewDialogFragment>()
    }

    @Test
    fun freshLaunchRestoresOneDialogAndRemembersCompletion() {
        val preferences = Education.getEducationSharedPreferences(context)
        val key = context.getString(R.string.education_feature_overview_key)
        launch().use { controller ->
            assertTrue(controller.get().overviews().single().requireDialog().isShowing)
            controller.recreate()
            val overview = controller.get().overviews().single()
            assertEquals(FeatureOverviewDialogFragment.TAG, overview.tag)
            assertFalse(preferences.getBoolean(key, false))
            shadowOf(Looper.getMainLooper()).idle()
            fun hintCount() = controller.get().window.decorView.allViews.count { it is TapTargetView }
            assertEquals("Hints must wait for the overview to close", 0, hintCount())

            val dialog = overview.requireDialog() as AlertDialog
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(controller.get().overviews().isEmpty())
            assertTrue(preferences.getBoolean(key, false))
            assertEquals("Show the first database hint after dismissal", 1, hintCount())
        }
        launch().use { assertTrue(it.get().overviews().isEmpty()) }
    }

    @Test
    fun selectionRequestLeavesOverviewForNextLauncherVisit() {
        val selection = Intent(context, FileDatabaseSelectActivity::class.java)
            .addSpecialMode(SpecialMode.SELECTION)
        launch(selection).use { assertTrue(it.get().overviews().isEmpty()) }
        launch().use { assertTrue(it.get().overviews().single().requireDialog().isShowing) }
    }

    @Test
    fun configuredDefaultDatabaseBypassesOverview() {
        PreferencesUtil.saveDefaultDatabasePath(context, Uri.parse("content://documents/vault.kdbx"))
        launch().use { assertTrue(it.get().overviews().isEmpty()) }
    }

    @Test
    fun disabledEducationalHintsSuppressOverview() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(context.getString(R.string.enable_education_screens_key), false).apply()
        launch().use { assertTrue(it.get().overviews().isEmpty()) }
    }

    @Test
    fun upgradeSkipsOverviewUntilHintsAreReset() {
        shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName)
            .lastUpdateTime += 1000L
        launch().use { assertTrue(it.get().overviews().isEmpty()) }
        val editor = Education.getEducationSharedPreferences(context).edit()
        Education.educationResourcesKeys.forEach { editor.putBoolean(context.getString(it), false) }
        editor.apply()
        launch().use { assertTrue(it.get().overviews().single().requireDialog().isShowing) }
    }
}
