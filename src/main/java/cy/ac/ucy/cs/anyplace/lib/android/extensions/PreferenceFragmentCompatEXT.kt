package cy.ac.ucy.cs.anyplace.lib.android.extensions

import android.text.InputType
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG

fun PreferenceFragmentCompat.setNumericInput(@StringRes prefRes: Int,
                                             @StringRes summaryRes: Int,
                                             initialValue: String, minLimit: Int) {
  val preference = findPreference(getString(prefRes)) as EditTextPreference?
  preference?.setOnBindEditTextListener { editText ->
    editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

    if (editText.text.isEmpty()) editText.setText(initialValue)

    editText.setSelection(editText.text.length) // cursor at the end
  }

  // set initial value and update on new values
  preference?.summary = getString(summaryRes, initialValue)

  // impose limit
  preference?.setOnPreferenceChangeListener { it, strValue ->
    val value = (strValue as String).toInt()
    LOG.E(TAG, "NEW VAL: $strValue")
    LOG.E(TAG, "min: $minLimit")
    if (value < minLimit) {
      Toast.makeText(context, "Min value is range: $minLimit", Toast.LENGTH_SHORT).show()
      return@setOnPreferenceChangeListener false
    }

    it.summary = getString(summaryRes, value)
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

fun PreferenceFragmentCompat.setListPreferenceInput(@StringRes prefRes: Int, value: String) {
  val pref= findPreference(getString(prefRes)) as ListPreference?
  pref?.value = value
}
