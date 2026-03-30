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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tbmapp.ui.theme.TbmAppTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class V3Response(
    val records: List<V3Record>
)

data class V3Record(
    val fields: V3Fields
)

data class V3Fields(
    val nom: String,
    val nbvelos: Int,
    val nbplaces: Int,
    val etat: String
)

interface VeloApi {
    @GET("api/records/1.0/search/?dataset=ci_vcub_p&rows=100")
    suspend fun getStations(): V3Response
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TbmAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    var listeStations by remember { mutableStateOf(listOf<V3Record>()) }
    var messageStatus by remember { mutableStateOf("Chargement...") }

    LaunchedEffect(Unit) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://opendata.bordeaux-metropole.fr/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(VeloApi::class.java)
            val response = api.getStations()

            listeStations = response.records
            messageStatus = "V3 Bordeaux : ${listeStations.size} stations"
        } catch (e: Exception) {
            messageStatus = "Erreur : ${e.localizedMessage}"
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = messageStatus,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(listeStations) { record ->
                StationCard(
                    nom = record.fields.nom,
                    velos = record.fields.nbvelos,
                    places = record.fields.nbplaces,
                    etat = record.fields.etat
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
    etat: String
) {
    val estDeconnectee = etat != "CONNECTEE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = nom,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (estDeconnectee) Color.Gray else Color.Unspecified
                )

                if (estDeconnectee) {
                    Text(
                        text = "HORS SERVICE",
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall
                    )
                } else if (places == 0) {
                    Text(
                        text = "Complet",
                        color = Color(0xFFC0581A),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$velos vélos",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (velos > 0) Color(0xFF2E7D32) else Color.Red
                )
                Text(
                    text = "$places places libres",
                    style = MaterialTheme.typography.bodyMedium
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