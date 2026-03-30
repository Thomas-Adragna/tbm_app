package com.example.tbmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tbmapp.ui.theme.TbmAppTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilterChip
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.osmdroid.config.Configuration
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import retrofit2.http.GET


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        Configuration.getInstance().userAgentValue = packageName

        setContent {
            TbmAppTheme {
                val navController = rememberNavController()
                val viewModel: VeloViewModel = viewModel()

                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                val bottomItems = listOf(BottomNavItem.Liste, BottomNavItem.Carte)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute in bottomItems.map { it.route }) {
                            NavigationBar {
                                bottomItems.forEach { item ->
                                    NavigationBarItem(
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(Routes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                viewModel = viewModel,
                                onStationClick = { record ->
                                    val index = viewModel.stations.value.indexOf(record)
                                    navController.navigate(Routes.detail(index))
                                }
                            )
                        }

                        composable(Routes.MAP) {
                            MapScreen(
                                viewModel = viewModel,
                                onMarkerClick = { record ->
                                    val index = viewModel.stations.value.indexOf(record)
                                    navController.navigate(Routes.detail(index))
                                }
                            )
                        }

                        composable(Routes.DETAIL) { backStackEntry ->
                            val id = backStackEntry.arguments
                                ?.getString("stationId")
                                ?.toInt() ?: 0

                            DetailScreen(
                                viewModel = viewModel,
                                stationIndex = id,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: VeloViewModel = viewModel(),
    onStationClick: (V3Record) -> Unit = {}
) {
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val messageStatus by viewModel.messageStatus.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortByDistance by viewModel.sortByDistance.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val locationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    val stationsFiltrees = stations
        .filter { it.fields.nom.contains(searchQuery, ignoreCase = true) }
        .let { liste ->
            val loc = userLocation
            if (sortByDistance && loc != null) {
                liste.sortedBy { record ->
                    viewModel.distanceKm(record.fields, loc.latitude, loc.longitude)
                }
            } else {
                liste
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                placeholder = { Text("Rechercher une station...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            IconButton(
                onClick = { viewModel.loadData() },
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rafraîchir"
                    )
                }
            }
        }

        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            FilterChip(
                selected = sortByDistance,
                onClick = {
                    if (locationPermission.status.isGranted) {
                        if (sortByDistance) {
                            viewModel.toggleSortByDistance()
                        } else {
                            viewModel.fetchUserLocation(context)
                        }
                    } else {
                        locationPermission.launchPermissionRequest()
                    }
                },
                label = { Text("Près de moi") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        Text(
            text = if (searchQuery.isEmpty()) {
                messageStatus
            } else {
                "${stationsFiltrees.size} résultat(s)"
            },
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(stationsFiltrees) { record ->
                val loc = userLocation
                val dist = if (loc != null) {
                    viewModel.distanceKm(record.fields, loc.latitude, loc.longitude)
                } else {
                    null
                }

                StationCard(
                    nom = record.fields.nom,
                    velos = record.fields.nbvelos,
                    places = record.fields.nbplaces,
                    etat = record.fields.etat,
                    distanceKm = dist,
                    onClick = { onStationClick(record) }
                )
            }
        }
    }
}

@Composable
fun StationCard(
    nom: String,
    velos: Int,
    places: Int,
    etat: String,
    distanceKm: Float? = null,
    onClick: () -> Unit = {}
) {
    val estDeconnectee = etat != "CONNECTEE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (estDeconnectee) Color(0xFFF1F1F1) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nom,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (estDeconnectee) Color.Gray else Color.Unspecified
                )

                if (distanceKm != null) {
                    Text(
                        text = if (distanceKm < 1f) {
                            "${(distanceKm * 1000).toInt()} m"
                        } else {
                            "${String.format("%.1f", distanceKm)} km"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (estDeconnectee) {
                    Text(
                        text = "HORS SERVICE",
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (places == 0 && !estDeconnectee) {
                    Text(
                        text = "Complet",
                        color = Color(0xFFC0581A),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$velos vélos",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (velos > 0) Color(0xFF2E7D32) else Color.Red
                )
                Text(
                    text = "$places places libres",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TbmAppTheme {
        HomeScreen()
    }
}