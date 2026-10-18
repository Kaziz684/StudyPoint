package com.example.studypoint.teacher

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import com.example.studypoint.R

/**
 * Searchable single-selection dialog used for Campus, School, Class,
 * Section, Gender, Subject, Session and Exam Type fields.
 *
 * Options are (value, label) pairs; search matches both, case-insensitively,
 * so typing "pre", "round", "schedule" or "t5" filters the exam types.
 * When allowCustom is true, the typed text itself can be used as a new value.
 */
object SearchablePicker {

    fun show(
        context: Context,
        title: String,
        options: List<Pair<String, String>>,
        allowCustom: Boolean,
        onPicked: (String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_searchable_picker, null)
        val search = view.findViewById<EditText>(R.id.editSearch)
        val list = view.findViewById<ListView>(R.id.listOptions)
        search.hint = "Search $title..."

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .create()

        var displayed: List<Pair<String, String>> = options
        val adapter = ArrayAdapter<String>(
            context,
            android.R.layout.simple_list_item_1,
            displayed.map { it.second }
        )
        list.adapter = adapter

        fun rebuild() {
            val q = search.text.toString().trim()
            val filtered = if (q.isEmpty()) {
                options
            } else {
                options.filter {
                    it.first.contains(q, ignoreCase = true) ||
                        it.second.contains(q, ignoreCase = true)
                }
            }
            displayed = if (allowCustom && q.isNotEmpty() &&
                options.none { it.first.equals(q, ignoreCase = true) }
            ) {
                listOf(q to "➕ Add \"$q\"") + filtered
            } else {
                filtered
            }
            adapter.clear()
            adapter.addAll(displayed.map { it.second })
            adapter.notifyDataSetChanged()
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = rebuild()
            override fun afterTextChanged(s: Editable?) {}
        })

        list.setOnItemClickListener { _, _, position, _ ->
            val picked = displayed.getOrNull(position) ?: return@setOnItemClickListener
            dialog.dismiss()
            onPicked(picked.first)
        }

        dialog.show()
    }

    /** Convenience for plain string options (value = label). */
    fun showSimple(
        context: Context,
        title: String,
        options: List<String>,
        allowCustom: Boolean,
        onPicked: (String) -> Unit
    ) = show(context, title, options.map { it to it }, allowCustom, onPicked)
}
