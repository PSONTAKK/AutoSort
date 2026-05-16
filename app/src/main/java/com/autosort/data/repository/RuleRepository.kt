package com.autosort.data.repository

import com.autosort.data.db.RuleDao
import com.autosort.data.model.Rule
import kotlinx.coroutines.flow.Flow

class RuleRepository(private val ruleDao: RuleDao) {

    val allRules: Flow<List<Rule>> = ruleDao.getAllRules()

    suspend fun insert(rule: Rule) = ruleDao.insert(rule)

    suspend fun update(rule: Rule) = ruleDao.update(rule)

    suspend fun deleteById(id: String) = ruleDao.deleteById(id)

    suspend fun getActiveRules(): List<Rule> = ruleDao.getActiveRules()

    suspend fun getById(id: String): Rule? = ruleDao.getById(id)
}
