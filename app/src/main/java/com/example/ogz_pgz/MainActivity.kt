package com.example.ogz_pgz

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.ogz_pgz.R
import com.google.android.material.tabs.TabLayout
import org.mapsforge.map.android.view.MapView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        toolbar = findViewById(R.id.mainToolbar)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId) {
                R.id.action_history -> {
                    val historyFragment = HistoryFragment()
                    historyFragment.show(supportFragmentManager, HistoryFragment.TAG)
                    true
                }
                R.id.action_info -> {
                    InfoFragment.newInstance().show(supportFragmentManager, "InfoFragment")
                    true
                }
                R.id.action_compass -> {
                    val compassDialog = CompassFragment.newInstance()
                    compassDialog.show(supportFragmentManager, CompassFragment.TAG)
                    true
                }
                else -> false
            }
        }

        tabLayout = findViewById(R.id.tabs)
        tabLayout.addTab(tabLayout.newTab().setText("OGZ"))
        tabLayout.addTab(tabLayout.newTab().setText("PGZ"))

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position){
                    0-> {
                        val navOption = NavOptions.Builder().setPopUpTo(R.id.pgzFragment, true).build()
                        navController.navigate(R.id.action_pgzFragment_to_ogzFragment2, null, navOption)
                    }
                    1->{
                        val navOption = NavOptions.Builder().setPopUpTo(R.id.ogzFragment, true).build()
                        navController.navigate(R.id.action_ogzFragment_to_pgzFragment2, null, navOption)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                Log.d("action", "tab unselected")
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d("action", "tab reselected")
            }
        })



        handleIntent(intent)

        enableEdgeToEdge()


    }


    private fun handleIntent(intent: Intent?){
        val value = intent?.getIntExtra(EXTRA_TAB_INDEX, -1)

        when(value){
            0 -> tabLayout.getTabAt(0)?.select()
            1 -> tabLayout.getTabAt(1)?.select()
        }
    }


    companion object{
        const val EXTRA_TAB_INDEX = "EXTRA_TAB_INDEX"

        fun setupMask(editText: EditText) {
            val template = "__°__'__.__''N __°__'__.__''E____.__"
            // Устанавливаем шаблон и курсор на первый символ '_'
            editText.setText(template)
            editText.setSelection(template.indexOf('_'))

            // Храним предыдущее состояние текста
            var previousText = template

            editText.addTextChangedListener(object : TextWatcher {
                var isUpdating = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    if (!isUpdating) {
                        previousText = s.toString()
                    }
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (isUpdating) return
                    if (s == null) return

                    isUpdating = true

                    // Вставка (count > 0)
                    if (count > 0) {
                        // Получаем вставленную подстроку
                        val inserted = s.substring(start, start + count)
                        // Если вставлен пробел, то отменяем вставку пробела и только перемещаем курсор
                        if (inserted == " ") {
                            // Возвращаем предыдущий текст, чтобы пробела не оказалось в строке
                            editText.setText(previousText)
                            // Находим следующую свободную позицию ('_') начиная с текущей позиции
                            val searchFrom = if (start + 1 < previousText.length) start + 1 else previousText.length
                            val nextUnderscore = previousText.indexOf('_', searchFrom)
                            if (nextUnderscore != -1) {
                                editText.setSelection(nextUnderscore)
                            } else {
                                editText.setSelection(previousText.length)
                            }
                        }
                        // Если вставлены цифры
                        else if (inserted.matches(Regex("\\d+"))) {
                            val sb = StringBuilder(previousText)
                            var pos = start
                            // Заменяем первые найденные символы '_' на введённые цифры
                            for (digit in inserted) {
                                val index = sb.indexOf("_", pos)
                                if (index != -1) {
                                    sb.setCharAt(index, digit)
                                    pos = index + 1
                                }
                            }
                            val newText = sb.toString()
                            editText.setText(newText)
                            // Ставим курсор в первую позицию с '_' после последней внесённой цифры
                            val nextUnderscore = newText.indexOf('_', pos)
                            if (nextUnderscore != -1) {
                                editText.setSelection(nextUnderscore)
                            } else {
                                editText.setSelection(newText.length)
                            }
                            previousText = newText
                        }
                    }
                    // Обработка удаления (когда before > count)
                    else if (before > count) {
                        val sb = StringBuilder(previousText)
                        for (i in start until (start + before)) {
                            if (i < previousText.length && previousText[i].isDigit()) {
                                sb.setCharAt(i, '_')
                            }
                        }
                        val newText = sb.toString()
                        editText.setText(newText)
                        editText.setSelection(start)
                        previousText = newText
                    }

                    isUpdating = false
                    // Вывод для отладки
                    Log.d("Text changed", editText.text.toString())
                }

                override fun afterTextChanged(s: Editable?) { }
            })
        }

        fun setupAngleMask(editText: EditText) {
            val template = "___°__'__.__''"
            editText.setText(template)
            // Устанавливаем курсор на первую свободную позицию (символ '_')
            editText.setSelection(template.indexOf('_'))

            // Храним предыдущее состояние шаблона
            var previousText = template

            editText.addTextChangedListener(object : TextWatcher {
                var isUpdating = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    if (!isUpdating) {
                        previousText = s.toString()
                    }
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (isUpdating) return
                    if (s == null) return

                    isUpdating = true

                    // Обработка вставки символов (count > 0)
                    if (count > 0) {
                        val inserted = s.substring(start, start + count)

                        // Если вставлен пробел — блокируем его и перемещаем курсор к следующей позиции '_'
                        if (inserted == " ") {
                            // Восстанавливаем предыдущий текст, чтобы пробела не было добавлено
                            editText.setText(previousText)
                            // Ищем следующий '_' начиная с позиции start + 1
                            val searchFrom = if (start + 1 < previousText.length) start + 1 else previousText.length
                            val nextUnderscore = previousText.indexOf('_', searchFrom)
                            if (nextUnderscore != -1) {
                                editText.setSelection(nextUnderscore)
                            } else {
                                editText.setSelection(previousText.length)
                            }
                        }
                        // Обработка вставки цифр
                        else if (inserted.matches(Regex("\\d+"))) {
                            val sb = StringBuilder(previousText)
                            var pos = start
                            for (digit in inserted) {
                                val index = sb.indexOf("_", pos)
                                if (index != -1) {
                                    sb.setCharAt(index, digit)
                                    pos = index + 1
                                }
                            }
                            val newText = sb.toString()
                            editText.setText(newText)
                            // Перемещаем курсор к следующему '_' после вставленных цифр
                            val nextUnderscore = newText.indexOf('_', pos)
                            if (nextUnderscore != -1) {
                                editText.setSelection(nextUnderscore)
                            } else {
                                editText.setSelection(newText.length)
                            }
                            previousText = newText
                        }
                    }
                    // Обработка удаления (когда before > count)
                    else if (before > count) {
                        val sb = StringBuilder(previousText)
                        // В диапазоне [start, start + before) заменяем цифры на '_'
                        for (i in start until (start + before)) {
                            if (i < previousText.length && previousText[i].isDigit()) {
                                sb.setCharAt(i, '_')
                            }
                        }
                        val newText = sb.toString()
                        editText.setText(newText)
                        editText.setSelection(start)
                        previousText = newText
                    }

                    isUpdating = false
                }

                override fun afterTextChanged(s: Editable?) { }
            })
        }
    }

    // Добавьте этот метод в вашу Activity
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN ||
            ev.action == MotionEvent.ACTION_MOVE ||
            ev.action == MotionEvent.ACTION_UP) {

            // Ищем все MapView в иерархии представлений
            val decorView = window.decorView as ViewGroup
            val mapViews = findMapViewsInViewGroup(decorView)

            for (mapView in mapViews) {
                // Определяем позицию MapView на экране
                val pos = IntArray(2)
                mapView.getLocationOnScreen(pos)

                // Создаем прямоугольник для проверки попадания
                val hitRect = Rect(pos[0], pos[1], pos[0] + mapView.width, pos[1] + mapView.height)

                // Проверяем, находится ли касание в пределах MapView
                val isHitTest = hitRect.contains(ev.x.toInt(), ev.y.toInt())

                // Если касание происходит внутри MapView
                if (isHitTest) {
                    // Запрещаем родительскому элементу перехватывать события касания
                    mapView.parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    // Вспомогательный метод для поиска всех MapView в иерархии представлений
    private fun findMapViewsInViewGroup(viewGroup: ViewGroup): List<MapView> {
        val mapViews = mutableListOf<MapView>()

        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            if (child is MapView) {
                mapViews.add(child)
            } else if (child is ViewGroup) {
                mapViews.addAll(findMapViewsInViewGroup(child))
            }
        }

        return mapViews
    }
}