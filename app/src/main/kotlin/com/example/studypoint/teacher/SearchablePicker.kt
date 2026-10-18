package com.example.studypoint.teacher

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.Toast
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

    /**
     * Multi-select variant: checkboxes with instant search. At most [max]
     * options can be ticked; Done returns the selected raw values.
     */
    fun showMulti(
        context: Context,
        title: String,
        options: List<Pair<String, String>>,
        selected: List<String>,
        max: Int,
        onDone: (List<String>) -> Unit
    ) {
        val checked = selected.toMutableSet()
        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(TeacherUi.dp(context, 18), TeacherUi.dp(context, 16), TeacherUi.dp(context, 18), TeacherUi.dp(context, 4))

        val search = EditText(context)
        search.hint = "Search..."
        search.setSingleLine(true)
        search.textSize = 15f
        search.setTextColor(Color.parseColor("#1F2937"))
        search.setHintTextColor(Color.parseColor("#94A3B8"))
        search.setBackgroundResource(R.drawable.bg_input)
        root.addView(search)

        val list = LinearLayout(context)
        list.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(context)
        val scrollLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, TeacherUi.dp(context, 340)
        )
        scrollLp.topMargin = TeacherUi.dp(context, 10)
        scroll.layoutParams = scrollLp
        scroll.addView(list)
        root.addView(scroll)

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Done") { _, _ ->
                onDone(options.map { it.first }.filter { checked.contains(it) })
            }
            .create()

        fun refreshList() {
            val q = search.text.toString().trim()
            list.removeAllViews()
            val shown = options.filter {
                q.isEmpty() || it.first.contains(q, ignoreCase = true) ||
                    it.second.contains(q, ignoreCase = true)
            }
            if (shown.isEmpty()) {
                list.addView(TeacherUi.text(context, "No matches found", 14f, "#94A3B8"))
                return
            }
            for ((value, label) in shown) {
                val cb = android.widget.CheckBox(context)
                cb.text = label
                cb.textSize = 15.5f
                cb.setTextColor(Color.parseColor("#1F2937"))
                cb.isChecked = checked.contains(value)
                cb.setPadding(TeacherUi.dp(context, 4), TeacherUi.dp(context, 6), TeacherUi.dp(context, 4), TeacherUi.dp(context, 6))
                cb.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (checked.size >= max) {
                            cb.isChecked = false
                            Toast.makeText(context, "Maximum $max can be selected", Toast.LENGTH_SHORT).show()
                        } else {
                            checked.add(value)
                        }
                    } else {
                        checked.remove(value)
                    }
                }
                list.addView(cb)
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s2: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s2: CharSequence?, a: Int, b: Int, c: Int) = refreshList()
            override fun afterTextChanged(s2: Editable?) {}
        })
        refreshList()
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
