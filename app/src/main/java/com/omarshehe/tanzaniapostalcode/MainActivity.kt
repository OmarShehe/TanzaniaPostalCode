package com.omarshehe.tanzaniapostalcode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.data.AddressStore
import com.omarshehe.tzaddress.data.createAddressRepository
import com.omarshehe.tzaddress.ui.AddressPicker
import com.omarshehe.tzaddress.ui.AddressSearchField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Sample: the two ready-made widgets over the bundled Tanzanian address database. */
class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var store by mutableStateOf<AddressStore?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scope.launch { store = createAddressRepository(applicationContext) }
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    val repository = store
                    Column(
                        Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (repository == null) {
                            Text("Loading addresses…")
                        } else {
                            var searched by rememberSaveable { mutableStateOf<String?>(null) }
                            var picked by remember { mutableStateOf<AddressPath?>(null) }
                            Text("Search", style = MaterialTheme.typography.titleMedium)
                            AddressSearchField(repository, onSelected = { searched = it.describe() })
                            searched?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            HorizontalDivider()
                            Text("Pick", style = MaterialTheme.typography.titleMedium)
                            AddressPicker(repository, value = picked, onValueChange = { picked = it })
                            Text(picked?.describe() ?: "Nothing selected yet", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        store?.close()
        super.onDestroy()
    }
}

private fun AddressPath.describe(): String =
    listOfNotNull(kitongoji?.name, mtaa?.name, ward?.let { "${it.name} (${it.postcode})" }, district?.name, region.name).joinToString(", ")
