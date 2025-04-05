package com.example.ogz_pgz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.ogz_pgz.R
import model.Coordinate
import org.mapsforge.core.graphics.Color
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import java.io.File
import java.io.FileOutputStream
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var tileCache: TileCache
    private lateinit var tileRendererLayer: TileRendererLayer
    private val viewModel: MapViewModel by activityViewModels()

    //Маркеры
    private var marker1: Marker? = null
    private var marker2: Marker? = null
    private var polyline: Polyline? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        AndroidGraphicFactory.createInstance(requireActivity().application)

        mapView = MapView(requireContext())
        mapView.isClickable = true
        mapView.mapScaleBar.isVisible = true
        mapView.setBuiltInZoomControls(true)

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
        })

        viewModel.point2.observe(viewLifecycleOwner, {
                coordinate ->
            updateMarker2(coordinate)
            updatePolyline()
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

        tileCache.destroy()
        mapView.destroyAll()
        AndroidGraphicFactory.clearResourceMemoryCache()
    }


}