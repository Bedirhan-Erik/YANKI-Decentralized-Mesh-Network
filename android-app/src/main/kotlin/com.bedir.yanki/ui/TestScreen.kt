import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TestScreen(viewModel: TestViewModel = viewModel()) {
    val devices = viewModel.foundDevices.collectAsState().value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("YANKI Test Paneli", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { viewModel.createDummyUser() }, modifier = Modifier.fillMaxWidth()) {
            Text("Yerel DB'ye Sahte Kullanıcı Ekle")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { viewModel.startBleTest() }, modifier = Modifier.fillMaxWidth()) {
            Text("BLE Yayını ve Taramayı Başlat")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Bulunan Cihazlar:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.fillWeight(1f)) {
            items(devices) { device ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(device, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}