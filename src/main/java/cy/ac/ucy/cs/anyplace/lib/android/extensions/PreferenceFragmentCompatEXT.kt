package cy.ac.ucy.cs.anyplace.lib.android.extensions

/**
// val value = (val as String).toInt()
*/
import android.text.InputType
import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

fun PreferenceFragmentCompat.setNumericInput(@StringRes prefRes: Int,
                                             @StringRes summaryRes: Int,
                                             initialValue: String, ) {
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

fun PreferenceFragmentCompat.setPercentageInput(@StringRes prefRes: Int,
                                             @StringRes summaryRes: Int,
                                             initialValue: String) {
  val preference = findPreference(getString(prefRes)) as EditTextPreference?
  preference?.setOnBindEditTextListener { editText ->
    editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

    if (editText.text.isEmpty()) editText.setText(initialValue)

    editText.setSelection(editText.text.length) // cursor at the end
  }

  // set initial value and update on new values
  preference?.summary = getString(summaryRes, "${initialValue}%")
  preference?.setOnPreferenceChangeListener { it, newValue ->
    if (outOfBounds(newValue)) return@setOnPreferenceChangeListener false

    it.summary = getString(summaryRes, "${newValue}%")
    true
  }
}

fun outOfBounds(newValue: Any?): Boolean {
  val value : Int = newValue as Int
  return value < 0|| value > 100
}

fun PreferenceFragmentCompat.setBooleanInput(@StringRes prefRes: Int, value: Boolean) {
  val preference = findPreference(getString(prefRes)) as SwitchPreferenceCompat?
  preference?.isChecked = value
}
