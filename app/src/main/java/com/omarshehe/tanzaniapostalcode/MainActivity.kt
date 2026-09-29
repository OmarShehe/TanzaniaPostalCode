package com.omarshehe.tanzaniapostalcode

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.omarshehe.tzaddress.AddressMatch
import com.omarshehe.tzaddress.data.AddressStore
import com.omarshehe.tzaddress.data.createAddressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Sample: type-ahead search over the bundled Tanzanian address database. */
class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var repository: AddressStore? = null
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val input = findViewById<EditText>(R.id.search)
        val status = findViewById<TextView>(R.id.status)
        val list = findViewById<ListView>(R.id.results)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        list.adapter = adapter

        status.setText(R.string.loading)
        scope.launch {
            try {
                repository = createAddressRepository(applicationContext)
                status.text = ""
                runSearch(input.text.toString(), status, adapter)
            } catch (e: Exception) {
                status.setText(R.string.load_failed)
            }
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = runSearch(s?.toString().orEmpty(), status, adapter)
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    private fun runSearch(query: String, status: TextView, adapter: ArrayAdapter<String>) {
        val repo = repository ?: return
        searchJob?.cancel()
        searchJob = scope.launch {
            val matches = repo.search(query)
            adapter.clear()
            adapter.addAll(matches.map(::describe))
            status.text = if (query.isNotBlank() && matches.isEmpty()) getString(R.string.no_results) else ""
        }
    }

    private fun describe(match: AddressMatch): String {
        val path = match.path
        val trail = listOfNotNull(path.region.name, path.district?.name, path.ward?.name, path.mtaa?.name)
            .dropLast(1) // the node itself is already the label
            .joinToString(" › ")
        val postcode = match.postcode?.let { "  ($it)" }.orEmpty()
        return "${match.label}$postcode\n$trail"
    }

    override fun onDestroy() {
        scope.cancel()
        repository?.close()
        super.onDestroy()
    }
}
