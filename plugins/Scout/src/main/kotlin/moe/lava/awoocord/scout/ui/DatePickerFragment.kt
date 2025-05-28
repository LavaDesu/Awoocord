package moe.lava.awoocord.scout.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import java.util.Calendar

class DatePickerFragment(
    private val callback: (String) -> Unit
) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    companion object {
        fun open(fragmentManager: FragmentManager, callback: (date: String) -> Unit) {
            DatePickerFragment(callback).show(fragmentManager, "datePicker")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()

        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK
        return DatePickerDialog(requireContext(), this, year, month, day)
    }

    override fun onDateSet(picker: DatePicker, year: Int, month: Int, day: Int) {
        callback("%04d-%02d-%02d".format(year, month, day))
    }
}
