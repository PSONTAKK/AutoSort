package com.autosort.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autosort.data.config.AppConfig
import com.autosort.data.model.DestinationType
import com.autosort.data.model.Rule
import com.autosort.data.model.RuleType
import com.autosort.data.repository.RuleRepository
import com.autosort.service.ai.SuggestionEngine
import com.autosort.service.ai.SuggestionEngine.RuleSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RuleViewModel @Inject constructor(
    private val repository: RuleRepository,
    private val suggestionEngine: SuggestionEngine,
    private val appConfig: AppConfig
) : ViewModel() {

    val rules: StateFlow<List<Rule>> = repository.allRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _suggestions = MutableStateFlow<List<RuleSuggestion>>(emptyList())
    val suggestions: StateFlow<List<RuleSuggestion>> = _suggestions.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    init {
        // Run suggestion scan on launch if we have few rules (indicating new user)
        viewModelScope.launch {
            repository.allRules.collect { currentRules ->
                if (currentRules.size < 2 && _suggestions.value.isEmpty()) {
                    _isLoadingSuggestions.value = true
                    try {
                        val generated = withContext(Dispatchers.IO) {
                            suggestionEngine.generateSuggestions(appConfig.sourceFolder)
                        }
                        _suggestions.value = generated
                    } finally {
                        _isLoadingSuggestions.value = false
                    }
                }
            }
        }
    }

    fun addRule(
        name: String,
        type: RuleType,
        value: String,
        target: String,
        destinationType: DestinationType = DestinationType.LOCAL,
        sourceFolder: String? = null,
        keepLocalAfterUpload: Boolean = false,
        targetAccount: String? = null
    ) {
        viewModelScope.launch {
            repository.insert(
                Rule(
                    name                 = name.trim(),
                    type                 = type,
                    value                = value.trim(),
                    target               = target.trim(),
                    destinationType      = destinationType,
                    active               = true,
                    sourceFolder         = sourceFolder?.trim()?.takeIf { it.isNotBlank() },
                    keepLocalAfterUpload = keepLocalAfterUpload,
                    targetAccount        = targetAccount
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
    
    fun dismissSuggestion(suggestion: RuleSuggestion) {
        _suggestions.value = _suggestions.value.filter { it != suggestion }
    }
}
