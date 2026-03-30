package com.example.tbmapp

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val BORDEAUX_CENTER = GeoPoint(44.8378, -0.5792)
private const val ZOOM_DEFAUT = 13.5

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: VeloViewModel,
    onMarkerClick: (V3Record) -> Unit = {}
) {
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val locationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    var mapViewRef: MapView? by remember { mutableStateOf(null) }

    LaunchedEffect(userLocation) {
        val loc = userLocation ?: return@LaunchedEffect

        mapViewRef?.controller?.animateTo(
            GeoPoint(loc.latitude, loc.longitude),
            15.0,
            1200L
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(ZOOM_DEFAUT)
                    controller.setCenter(BORDEAUX_CENTER)
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapViewRef = mapView
                rafraichirMarqueurs(mapView, stations, userLocation, onMarkerClick)
            },
            onRelease = { mapView ->
                mapView.onDetach()
            }
        )

        FloatingActionButton(
            onClick = {
                if (locationPermission.status.isGranted) {
                    viewModel.fetchUserLocation(context)
                } else {
                    locationPermission.launchPermissionRequest()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Me localiser"
            )
        }
    }
}

private fun creerIconeRonde(
    couleurFond: Int,
    couleurBord: Int = Color.WHITE,
    taillePx: Int = 48
): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(taillePx, taillePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val rayon = taillePx / 2f

    val paintBord = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleurBord
        style = Paint.Style.FILL
    }
    canvas.drawCircle(rayon, rayon, rayon, paintBord)

    val paintFond = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleurFond
        style = Paint.Style.FILL
    }
    canvas.drawCircle(rayon, rayon, rayon * 0.80f, paintFond)

    return BitmapDrawable(Resources.getSystem(), bitmap)
}

private fun rafraichirMarqueurs(
    mapView: MapView,
    stations: List<V3Record>,
    userLocation: Location?,
    onMarkerClick: (V3Record) -> Unit
) {
    mapView.overlays.clear()

    stations.forEach { record ->
        val geo = record.fields.geo_point_2d ?: return@forEach
        val position = GeoPoint(geo[0], geo[1])

        val couleur = when {
            record.fields.etat != "CONNECTEE" -> Color.GRAY
            record.fields.nbvelos == 0 -> Color.RED
            else -> Color.parseColor("#2E7D32")
        }

        val marker = Marker(mapView).apply {
            this.position = position
            title = record.fields.nom
            snippet = buildSnippet(record.fields)
            icon = creerIconeRonde(couleur)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

            setOnMarkerClickListener { _, _ ->
                onMarkerClick(record)
                true
            }
        }

        mapView.overlays.add(marker)
    }

    userLocation?.let { loc ->
        val moi = Marker(mapView).apply {
            position = GeoPoint(loc.latitude, loc.longitude)
            title = "Ma position"
            icon = creerIconeRonde(
                Color.parseColor("#1565C0"),
                Color.WHITE,
                40
            )
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        mapView.overlays.add(moi)
    }

    mapView.invalidate()
}

private fun buildSnippet(fields: V3Fields): String {
    val etatStr = if (fields.etat == "CONNECTEE") "Connectée" else "Hors service"
    return "${fields.nbvelos} vélos | ${fields.nbplaces} places | $etatStr"
}