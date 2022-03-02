package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.text.InputType
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cy.ac.ucy.cs.anyplace.lib.android.LOG

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

/**
 * Setting an input field as a percentage:
 * - valid range: 0 to 100
 *
 * @msg100: message when the percentage is 100
 * @msg0: message when the percentage is 0
 */
fun PreferenceFragmentCompat.setPercentageInput(@StringRes prefRes: Int,
                                                @StringRes summaryRes: Int,
                                                initialValue: String,
                                                msg100: String? = null,
                                                msg0: String? = null,
) {
  val preference = findPreference(getString(prefRes)) as EditTextPreference?
  preference?.setOnBindEditTextListener { editText ->
    editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

    if (editText.text.isEmpty()) editText.setText(initialValue)

    editText.setSelection(editText.text.length) // cursor at the end
  }

  // set initial value and update on new values
  preference?.summary = getString(summaryRes, "${initialValue}%")
  preference?.setOnPreferenceChangeListener { it, strValue ->
    val value = (strValue as String).toInt()
    if (outOfBounds(value)) {
      Toast.makeText(context, "Valid is range: 0-100", Toast.LENGTH_SHORT).show()
      return@setOnPreferenceChangeListener false
    }

    var summary = when (value) {
      100 -> msg100
      0 -> msg0
      else -> null
    }

    if (summary==null)  summary = getString(summaryRes, "${value}%")
    it.summary = summary

    true
  }
}

fun outOfBounds(value: Int) : Boolean = (value < 0 || value > 100)

fun PreferenceFragmentCompat.setBooleanInput(@StringRes prefRes: Int, value: Boolean) {
  val preference = findPreference(getString(prefRes)) as SwitchPreferenceCompat?
  preference?.isChecked = value
}
