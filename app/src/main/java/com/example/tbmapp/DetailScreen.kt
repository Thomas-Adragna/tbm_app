package com.example.tbmapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: VeloViewModel,
    stationIndex: Int,
    onBack: () -> Unit
) {
    val stations by viewModel.stations.collectAsStateWithLifecycle()

    if (stationIndex < 0 || stationIndex >= stations.size) {
        onBack()
        return
    }

    val station = stations[stationIndex].fields
    val estConnectee = station.etat == "CONNECTEE"
    val couleurEtat = if (estConnectee) Color(0xFF1E7A3E) else Color(0xFFC0392B)
    val couleurVelos = if (station.nbvelos > 0) Color(0xFF1E7A3E) else Color(0xFFC0392B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(station.nom, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Statut", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = station.etat,
                        color = couleurEtat,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Vélos disponibles", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "${station.nbvelos}",
                        style = MaterialTheme.typography.displayMedium,
                        color = couleurVelos
                    )
                    if (station.nbvelos == 0) {
                        Text("Aucun vélo disponible", color = Color(0xFFC0392B))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Places libres", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "${station.nbplaces}",
                        style = MaterialTheme.typography.displayMedium
                    )
                    if (station.nbplaces == 0) {
                        Text("Station complète", color = Color(0xFFC0581A))
                    }
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retour à la liste")
            }
        }
    }
}