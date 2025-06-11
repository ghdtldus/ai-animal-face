package com.example.android.utils

import android.content.Context
import com.example.android.data.model.ResultLog
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.android.utils.ResultStorage
import java.util.UUID
import android.util.Log

object ResultStorage {
    private const val PREF_NAME = "result_history"
    private const val KEY_RESULTS = "results"

    fun saveResult(context: Context, resultLog: ResultLog) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val current = prefs.getString(KEY_RESULTS, "[]")
        val list = gson.fromJson(current, Array<ResultLog>::class.java).toMutableList()

        list.add(resultLog)
        prefs.edit().putString(KEY_RESULTS, gson.toJson(list)).apply()
    }

    fun loadRecentResults(context: Context): List<ResultLog> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        val json = prefs.getString(KEY_RESULTS, "[]") ?: return emptyList()
        val list = try {
            gson.fromJson(json, Array<ResultLog>::class.java).toMutableList()
        } catch (e: Exception) {
            Log.e("ResultStorage", "불러오기 실패: ${e.message}")
            return emptyList()
        }

        var updated = false
        val updatedList = list.map {
            if (it.id.isBlank()) {
                updated = true
                it.copy(id = UUID.randomUUID().toString())
            } else it
        }

        if (updated) {
            prefs.edit().putString(KEY_RESULTS, gson.toJson(updatedList)).apply()
        }

        return updatedList.filter {
            try {
                LocalDate.parse(it.date, formatter).isAfter(today.minusDays(30))
            } catch (e: Exception) {
                false
            }
        }
    }

    fun deleteResult(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RESULTS, "[]") ?: return
        val list = Gson().fromJson(json, Array<ResultLog>::class.java).toMutableList()

        val updatedList = list.filterNot { it.id == id }

        val newJson = Gson().toJson(updatedList)
        prefs.edit().putString(KEY_RESULTS, newJson).apply()
    }
    
}
