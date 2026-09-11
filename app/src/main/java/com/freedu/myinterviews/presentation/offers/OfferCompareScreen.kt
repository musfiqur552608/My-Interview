package com.freedu.myinterviews.presentation.offers

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedu.myinterviews.domain.model.Offer
import com.freedu.myinterviews.domain.repository.TrackerRepository
import com.freedu.myinterviews.presentation.components.EmptyState
import com.freedu.myinterviews.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OffersViewModel @Inject constructor(repo: TrackerRepository) : ViewModel() {
    val offers = repo.observeOffers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferCompareScreen(vm: OffersViewModel = hiltViewModel()) {
    val offers by vm.offers.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Offer comparison", fontWeight = FontWeight.Bold) }) }
    ) { pad ->
        if (offers.isEmpty()) {
            Column(Modifier.padding(pad).fillMaxSize()) {
                EmptyState(icon = Icons.Default.CompareArrows, title = "No offers yet",
                    subtitle = "Offers you add from any application will appear here side-by-side.")
            }
            return@Scaffold
        }
        Row(
            Modifier.padding(pad).fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.width(120.dp)) {
                Spacer(Modifier.height(48.dp))
                listOf("Total/yr", "Base", "Bonus", "Equity", "Benefits", "Deadline", "Decision").forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.height(44.dp).padding(top = 12.dp))
                }
            }
            offers.forEach { offer ->
                OfferColumn(offer)
            }
        }
    }
}

@Composable
private fun OfferColumn(offer: Offer) {
    Card(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(offer.companyName.ifBlank { "Offer" }, fontWeight = FontWeight.Bold)
            Text(offer.jobTitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val tc = com.freedu.myinterviews.util.CompCalc.annualized(
                offer.baseSalary, offer.bonus, offer.equity
            )
            Text(com.freedu.myinterviews.util.CompCalc.formatShort(tc),
                modifier = Modifier.height(44.dp), fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("${offer.baseSalary}", modifier = Modifier.height(44.dp))
            Text("${offer.bonus}", modifier = Modifier.height(44.dp))
            Text(offer.equity.ifBlank { "—" }, modifier = Modifier.height(44.dp))
            Text(offer.benefits.ifBlank { "—" }, modifier = Modifier.height(44.dp))
            Text(DateUtils.countdown(offer.deadline), modifier = Modifier.height(44.dp),
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(offer.decision, modifier = Modifier.height(44.dp))
        }
    }
}
