package com.autosort.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autosort.data.db.AppDatabase
import com.autosort.data.model.DestinationType
import com.autosort.data.model.Rule
import com.autosort.data.model.RuleType
import com.autosort.data.repository.RuleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RuleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RuleRepository = RuleRepository(
        AppDatabase.getInstance(application).ruleDao()
    )

    val rules: StateFlow<List<Rule>> = repository.allRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addRule(
        name: String,
        type: RuleType,
        value: String,
        target: String,
        destinationType: DestinationType = DestinationType.LOCAL,
        sourceFolder: String? = null
    ) {
        viewModelScope.launch {
            repository.insert(
                Rule(
                    name            = name.trim(),
                    type            = type,
                    value           = value.trim(),
                    target          = target.trim(),
                    destinationType = destinationType,
                    active          = true,
                    sourceFolder    = sourceFolder?.trim()?.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    fun toggleActive(rule: Rule) {
        viewModelScope.launch {
            repository.update(rule.copy(active = !rule.active))
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    suspend fun getRuleById(id: String): Rule? = repository.getById(id)

    fun updateRule(rule: Rule) {
        viewModelScope.launch {
            repository.update(rule)
        }
    }
}
