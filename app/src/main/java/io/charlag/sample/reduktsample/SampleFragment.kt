package io.charlag.sample.reduktsample

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject

/**
 * Created by charlag on 8/20/17.
 */

class SampleFragment : LifecycleFragment(), SampleView {

    private lateinit var field: EditText
    private lateinit var recyclerView: RecyclerView

    private val adapter = TodoAdapter()
    private val disposable = CompositeDisposable()
    private val addTodoSubject: PublishSubject<SampleViewEvent.TodoAddedViewEvent> =
            PublishSubject.create()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sample, container, false)
        field = view.findViewById(R.id.et_todo)
        recyclerView = view.findViewById(R.id.rv_todos)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        field.setImeActionLabel("Add", KeyEvent.KEYCODE_ENTER)
        field.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTodoSubject.onNext(SampleViewEvent.TodoAddedViewEvent(textView.text.toString()))
                textView.text = null
            }
            true
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        val viewModel = ViewModelProviders.of(this).get(SampleViewModel::class.java)
        viewModel.init(this)

        viewModel.viewState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { todos ->
                    adapter.update(todos)
                }.addTo(disposable)
    }

    override val events: Observable<SampleViewEvent>
        get() = Observable.merge(
                addTodoSubject,
                adapter.checkedChanges.map {
                    SampleViewEvent.TodoCheckedViewEvent(it.first, it.second)
                }
        )

    private class TodoAdapter : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

        val checkedChanges: PublishSubject<Pair<Long, Boolean>> = PublishSubject.create()
        private val todos = mutableListOf<TodoViewData>()

        init {
            setHasStableIds(true)
        }

        fun update(todos: List<TodoViewData>) {
            this.todos.clear()
            this.todos += todos
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = todos.size

        override fun getItemId(position: Int): Long = todos[position].id

        override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
            val item = todos[position]
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = item.completed
            holder.checkBox.setOnCheckedChangeListener(holder)
            holder.textView.paintFlags = if (item.completed)
                holder.textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else holder.textView.paintFlags and (Paint.STRIKE_THRU_TEXT_FLAG.inv())
            holder.textView.text = item.text
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_todo, parent, false)
            return TodoViewHolder(view)
        }

        private inner class TodoViewHolder(itemView: View) :
                RecyclerView.ViewHolder(itemView),
                CompoundButton.OnCheckedChangeListener {

            override fun onCheckedChanged(button: CompoundButton, isChecked: Boolean) {
                val item = todos[adapterPosition]
                checkedChanges.onNext(item.id to isChecked)
            }

            val checkBox: CheckBox = itemView.findViewById(R.id.cb_todo)
            val textView: TextView = itemView.findViewById(R.id.tv_todo)
        }
    }
}