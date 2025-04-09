package com.example.ogz_pgz

import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import model.Coordinate
import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.Color
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.overlay.Polygon
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import java.io.File
import java.io.FileOutputStream
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import kotlin.math.max
import kotlin.math.min


class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var tileCache: TileCache
    private lateinit var tileRendererLayer: TileRendererLayer
    private val viewModel: MapViewModel by activityViewModels()

    //Маркеры
    private var marker1: Marker? = null
    private var marker2: Marker? = null
    private var polyline: Polyline? = null
    private var rectangle: Polygon? = null
    private var areaLabel: Marker? = null

    private var isRectangleVisible = true

    private val gestureDetector by lazy {
        GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                toggleRectangleVisibility()
                return
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        AndroidGraphicFactory.createInstance(requireActivity().application)

        mapView = MapView(requireContext())
        mapView.isClickable = true
        mapView.mapScaleBar.isVisible = true
        mapView.setBuiltInZoomControls(true)

        mapView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        val nwCorner = LatLong(56.17, 23.16) // Северо-западный угол Беларуси
        val seCorner = LatLong(51.26, 32.78) // Юго-восточный угол Беларуси
        val boundingBox = BoundingBox(seCorner.latitude, nwCorner.longitude,
            nwCorner.latitude, seCorner.longitude)
        mapView.model.mapViewPosition.mapLimit = boundingBox

        setupMapFile()

        allObserve()


        return mapView
    }

    private fun allObserve(){
        viewModel.point1.observe(viewLifecycleOwner, {
                coordinate ->
            updateMarker1(coordinate)
            updatePolyline()
            updateRectangle()
        })

        viewModel.point2.observe(viewLifecycleOwner, {
                coordinate ->
            updateMarker2(coordinate)
            updatePolyline()
            updateRectangle()
        })
    }

    private fun updateMarker1(coordinate: Coordinate?){
        marker1?.let {
            mapView.layerManager.layers.remove(it)
            marker1 = null
        }

        if (coordinate != null){
            marker1 = createMarker(coordinate, R.drawable.marker_point_a)
            mapView.layerManager.layers.add(marker1)
        }

        mapView.invalidate()
    }

    private fun updateMarker2(coordinate: Coordinate?){
        marker2?.let {
            mapView.layerManager.layers.remove(it)
            marker2 = null
        }

        if (coordinate != null){
            marker2 = createMarker(coordinate, R.drawable.marker_point_b)
            mapView.layerManager.layers.add(marker2)
        }

        mapView.invalidate()
    }

    private fun createMarker(coordinate: Coordinate, drawableResId: Int): Marker{
        val latLong = LatLong(coordinate.latitude, coordinate.longitude)

        val bitmap = AndroidGraphicFactory.convertToBitmap(ResourcesCompat.getDrawable(resources, drawableResId, null))

        return Marker(latLong, bitmap, 0, -bitmap.height/2)
    }

    private fun updatePolyline() {
        // Удаляем старую линию
        polyline?.let {
            mapView.layerManager.layers.remove(it)
            polyline = null
        }

        // Если обе точки установлены, рисуем новую линию
        val point1 = viewModel.point1.value
        val point2 = viewModel.point2.value

        if (point1 != null && point2 != null) {
            val latLongs = ArrayList<LatLong>()
            latLongs.add(LatLong(point1.latitude, point1.longitude))
            latLongs.add(LatLong(point2.latitude, point2.longitude))

            // Создаем стиль
            val paintStroke = AndroidGraphicFactory.INSTANCE.createPaint()
            paintStroke.color = AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK)
            paintStroke.strokeWidth = 2 * resources.displayMetrics.density
            paintStroke.setStyle(Style.STROKE)

            // Создаем полилинию и устанавливаем точки
            polyline = Polyline(paintStroke, AndroidGraphicFactory.INSTANCE)
            polyline!!.setPoints(latLongs)  // Используем правильный метод setPoints

            // Добавляем линию в слои карты
            mapView.layerManager.layers.add(polyline)
        }

        mapView.invalidate()
    }

    private fun updateRectangle() {
        // Удаляем старый прямоугольник
        rectangle?.let {
            mapView.layerManager.layers.remove(it)
            rectangle = null
        }

        // Удаляем старую текстовую метку, если она есть
        areaLabel?.let {
            mapView.layerManager.layers.remove(it)
            areaLabel = null
        }

        // Если обе точки установлены и прямоугольник должен быть видимым, рисуем новый прямоугольник
        val point1 = viewModel.point1.value
        val point2 = viewModel.point2.value

        if (point1 != null && point2 != null && isRectangleVisible) {
            // Определяем углы прямоугольника
            val minLat = min(point1.latitude, point2.latitude)
            val maxLat = max(point1.latitude, point2.latitude)
            val minLon = min(point1.longitude, point2.longitude)
            val maxLon = max(point1.longitude, point2.longitude)

            // Создаем координаты для полигона (прямоугольника)
            val latLongs = ArrayList<LatLong>()
            latLongs.add(LatLong(minLat, minLon)) // Нижний левый угол
            latLongs.add(LatLong(minLat, maxLon)) // Нижний правый угол
            latLongs.add(LatLong(maxLat, maxLon)) // Верхний правый угол
            latLongs.add(LatLong(maxLat, minLon)) // Верхний левый угол
            latLongs.add(LatLong(minLat, minLon)) // Замыкаем полигон

            // Создаем стиль заливки с прозрачностью
            val paintFill = AndroidGraphicFactory.INSTANCE.createPaint()
            paintFill.color = AndroidGraphicFactory.INSTANCE.createColor(80, 255, 0, 0) // Полупрозрачный красный
            paintFill.setStyle(Style.FILL)

            // Создаем стиль контура
            val paintStroke = AndroidGraphicFactory.INSTANCE.createPaint()
            paintStroke.color = AndroidGraphicFactory.INSTANCE.createColor(Color.RED)
            paintStroke.strokeWidth = 2 * resources.displayMetrics.density
            paintStroke.setStyle(Style.STROKE)

            // Создаем полигон
            rectangle = Polygon(paintFill, paintStroke, AndroidGraphicFactory.INSTANCE)
            rectangle!!.setPoints(latLongs)

            // Добавляем его под другие слои, чтобы он не закрывал маркеры и линию
            mapView.layerManager.layers.add(1, rectangle)

            // Вычисляем площадь в квадратных километрах
            val area = calculateArea(minLat, maxLat, minLon, maxLon)
            Log.d("MapFragment", "Rectangle area: $area sq km")

            // Добавляем метку с площадью в центре прямоугольника
            addAreaLabel(LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2), area)
        }

        mapView.invalidate()
    }

    // Метод для добавления текстовой метки с площадью
    private fun addAreaLabel(center: LatLong, area: Double) {
        // Округляем площадь до 2 знаков после запятой
        val areaText = String.format("%.2f кв.км", area)

        // Создаем битмап с текстом
        val bitmap = createTextBitmap(areaText)

        // Создаем маркер в центре прямоугольника
        areaLabel = Marker(center, bitmap, -bitmap.width / 2, -bitmap.height / 2)

        // Добавляем метку на карту
        mapView.layerManager.layers.add(areaLabel)
    }

    // Метод для создания битмапа с текстом
    private fun createTextBitmap(text: String): Bitmap {
        // Настройка для отрисовки текста
        val paint = AndroidGraphicFactory.INSTANCE.createPaint()
        paint.color = AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK)
        paint.strokeWidth = 0f
        // На устройствах с высокой плотностью экрана увеличиваем размер текста
        val textSize = 20 * resources.displayMetrics.density
        // В Mapsforge нужно использовать setTextSize
        paint.setTextSize(textSize)
        paint.setStyle(Style.FILL)

        // Создаем фоновую краску
        val bgPaint = AndroidGraphicFactory.INSTANCE.createPaint()
        bgPaint.color = AndroidGraphicFactory.INSTANCE.createColor(220, 255, 255, 255) // Полупрозрачный белый
        bgPaint.setStyle(Style.FILL)

        // Примерно оцениваем размер текста
        // Mapsforge не имеет прямого API для измерения текста, поэтому примерно оцениваем
        val textWidth = text.length * textSize * 0.6f
        val textHeight = textSize * 1.2f

        // Создаем битмап достаточного размера с учетом отступов
        val padding = (10 * resources.displayMetrics.density).toInt()
        val width = textWidth.toInt() + padding * 2
        val height = textHeight.toInt() + padding * 2

        val bitmap = AndroidGraphicFactory.INSTANCE.createBitmap(width, height)
        val canvas = AndroidGraphicFactory.INSTANCE.createCanvas()
        canvas.setBitmap(bitmap)

        // Заполняем фон (без скругленных углов, т.к. Mapsforge не поддерживает drawRoundRect)
        canvas.fillColor(bgPaint.color)

        // Рисуем текст в центре
        canvas.drawText(text, padding, (height / 2 + textHeight / 3).toInt(), paint)

        return bitmap
    }

    private fun calculateArea(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Double {
        // Радиус Земли в километрах
        val earthRadius = 6371.0

        // Перевод координат в радианы
        val minLatRad = Math.toRadians(minLat)
        val maxLatRad = Math.toRadians(maxLat)
        val minLonRad = Math.toRadians(minLon)
        val maxLonRad = Math.toRadians(maxLon)

        // Вычисление ширины (расстояние по долготе)
        val width = earthRadius * Math.cos((minLatRad + maxLatRad) / 2) * Math.abs(maxLonRad - minLonRad)

        // Вычисление высоты (расстояние по широте)
        val height = earthRadius * Math.abs(maxLatRad - minLatRad)

        return width * height
    }

    // Переключение видимости прямоугольника
    private fun toggleRectangleVisibility() {
        isRectangleVisible = !isRectangleVisible
        updateRectangle()

        // Показываем всплывающее сообщение об изменении состояния
        val message = if (isRectangleVisible) "Прямоугольник отображается" else "Прямоугольник скрыт"
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setupMapFile() {
        val mapFile = File(requireContext().filesDir, "belarus.map")

        if (!mapFile.exists()) {
            copyMapFileFromAssets(mapFile)
        }

        val mapDataStore = MapFile(mapFile)

        tileCache = AndroidUtil.createTileCache(
            requireContext(),
            "mapcache",
            mapView.model.displayModel.tileSize,
            1f,
            mapView.model.frameBufferModel.overdrawFactor
        )

        tileRendererLayer = TileRendererLayer(
            tileCache,
            mapDataStore,
            mapView.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE
        )

        tileRendererLayer.setXmlRenderTheme(MapsforgeThemes.OSMARENDER)

        mapView.layerManager.layers.add(tileRendererLayer)

        mapView.model.mapViewPosition.setZoomLevel(10.toByte())
        mapView.model.mapViewPosition.setCenter(LatLong(53.90, 27.54))
    }

    private fun copyMapFileFromAssets(destinationFile: File) {
        requireContext().assets.open("belarus.map").use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        marker1?.let { mapView.layerManager.layers.remove(it) }
        marker2?.let { mapView.layerManager.layers.remove(it) }
        polyline?.let { mapView.layerManager.layers.remove(it) }
        rectangle?.let { mapView.layerManager.layers.remove(it) }

        tileCache.destroy()
        mapView.destroyAll()
        AndroidGraphicFactory.clearResourceMemoryCache()
    }
}