package com.example.android.utils

import android.content.Context
import com.example.android.data.model.ResultLog
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.android.utils.ResultStorage

data class ResultLog(val animal: String, val score: Float, val date: String)
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

        val json = prefs.getString(KEY_RESULTS, "[]")
        return gson.fromJson(json, Array<ResultLog>::class.java).toList()
            .filter {
                LocalDate.parse(it.date, formatter).isAfter(today.minusDays(30))
            }
    }

    fun deleteResult(context: Context, target: ResultLog) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val current = prefs.getString(KEY_RESULTS, "[]")
        val list = gson.fromJson(current, Array<ResultLog>::class.java).toMutableList()

        val updated = list.filterNot {
            it.date == target.date && it.animal == target.animal && it.score == target.score
        }

        prefs.edit().putString(KEY_RESULTS, gson.toJson(updated)).apply()
    }
    
}
