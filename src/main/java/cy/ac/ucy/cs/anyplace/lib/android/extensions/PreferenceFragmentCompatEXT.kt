package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.text.InputType
import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

fun PreferenceFragmentCompat.setNumericInput(@StringRes prefRes: Int,
                                             @StringRes summaryRes: Int,
                                             initialValue: String) {
  val preference = findPreference(getString(prefRes)) as EditTextPreference?
  preference?.setOnBindEditTextListener { editText ->
    editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

    if (editText.text.isEmpty()) editText.setText(initialValue)

    editText.setSelection(editText.text.length) // cursor at the end
  }

  // set initial value and update on new values
  preference?.summary = getString(summaryRes, initialValue)
  preference?.setOnPreferenceChangeListener { it, newValue ->
    it.summary = getString(summaryRes, newValue)
    true
  }
}