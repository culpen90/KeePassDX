package com.kunzisoft.keepass.education

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R

class FeatureOverviewDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.feature_overview_title)
            .setMessage(R.string.feature_overview_message)
            .setPositiveButton(R.string.feature_overview_get_started) { _, _ -> completeOverview() }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        completeOverview()
    }

    private fun completeOverview() {
        parentFragmentManager.setFragmentResult(TAG, Bundle.EMPTY)
    }

    companion object {
        const val TAG = "FeatureOverviewDialog"
    }
}
