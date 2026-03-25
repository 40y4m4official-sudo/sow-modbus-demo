package com.example.meterdemo.persistence

import android.content.Context
import com.example.meterdemo.meter.model.DataType
import com.example.meterdemo.meter.model.MeterPoint
import com.example.meterdemo.meter.model.MeterProfile
import com.example.meterdemo.meter.model.WordByteOrder
import org.json.JSONArray
import org.json.JSONObject

class MeterPersistence(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(): PersistedMeterState? {
        val jsonText = preferences.getString(KEY_STATE, null) ?: return null
        return runCatching {
            parseState(JSONObject(jsonText))
        }.getOrNull()
    }

    fun saveState(state: PersistedMeterState) {
        preferences.edit()
            .putString(KEY_STATE, buildStateJson(state).toString())
            .apply()
    }

    private fun buildStateJson(state: PersistedMeterState): JSONObject {
        return JSONObject()
            .put("selectedProfileModelId", state.selectedProfileModelId)
            .put("currentSlaveId", state.currentSlaveId)
            .put("currentRawValues", JSONObject().apply {
                state.currentRawValues.forEach { (address, value) ->
                    put(address.toString(), value)
                }
            })
            .put("userProfiles", JSONArray().apply {
                state.userProfiles.forEach { profile ->
                    put(profileToJson(profile))
                }
            })
    }

    private fun parseState(json: JSONObject): PersistedMeterState {
        val rawValuesJson = json.optJSONObject("currentRawValues") ?: JSONObject()
        val rawValues = buildMap {
            rawValuesJson.keys().forEach { key ->
                put(key.toInt(), rawValuesJson.getInt(key))
            }
        }

        val userProfilesJson = json.optJSONArray("userProfiles") ?: JSONArray()
        val userProfiles = buildList {
            for (index in 0 until userProfilesJson.length()) {
                add(jsonToProfile(userProfilesJson.getJSONObject(index)))
            }
        }

        return PersistedMeterState(
            userProfiles = userProfiles,
            selectedProfileModelId = json.optString("selectedProfileModelId").ifBlank { null },
            currentSlaveId = json.optInt("currentSlaveId").takeIf { it in 1..247 },
            currentRawValues = rawValues
        )
    }

    private fun profileToJson(profile: MeterProfile): JSONObject {
        return JSONObject()
            .put("modelId", profile.modelId)
            .put("displayName", profile.displayName)
            .put("slaveId", profile.slaveId)
            .put("baudRate", profile.baudRate)
            .put("dataBits", profile.dataBits)
            .put("parity", profile.parity)
            .put("stopBits", profile.stopBits)
            .put("functionCode", profile.functionCode)
            .put("points", JSONArray().apply {
                profile.points.forEach { point ->
                    put(
                        JSONObject()
                            .put("name", point.name)
                            .put("address", point.address)
                            .put("registerCount", point.registerCount)
                            .put("gain", point.gain)
                            .put("dataType", point.dataType.name)
                            .put("wordByteOrder", point.wordByteOrder.name)
                            .put("unit", point.unit)
                            .put("initialRawValue", point.initialRawValue)
                    )
                }
            })
    }

    private fun jsonToProfile(json: JSONObject): MeterProfile {
        val pointsJson = json.getJSONArray("points")
        val points = buildList {
            for (index in 0 until pointsJson.length()) {
                val pointJson = pointsJson.getJSONObject(index)
                add(
                    MeterPoint(
                        name = pointJson.getString("name"),
                        address = pointJson.getInt("address"),
                        registerCount = pointJson.optInt("registerCount", 1),
                        gain = pointJson.optDouble("gain", 1.0),
                        dataType = DataType.valueOf(pointJson.optString("dataType", DataType.INT16.name)),
                        wordByteOrder = WordByteOrder.valueOf(
                            pointJson.optString("wordByteOrder", WordByteOrder.MSB_MSB.name)
                        ),
                        unit = pointJson.optString("unit"),
                        initialRawValue = pointJson.optInt("initialRawValue")
                    )
                )
            }
        }

        return MeterProfile(
            modelId = json.getString("modelId"),
            displayName = json.getString("displayName"),
            slaveId = json.optInt("slaveId", 2),
            baudRate = json.optInt("baudRate", 19200),
            dataBits = json.optInt("dataBits", 8),
            parity = json.optInt("parity", 2),
            stopBits = json.optInt("stopBits", 1),
            functionCode = json.optInt("functionCode", 0x03),
            points = points
        )
    }

    private companion object {
        private const val PREFS_NAME = "meter_demo_prefs"
        private const val KEY_STATE = "persisted_meter_state"
    }
}

data class PersistedMeterState(
    val userProfiles: List<MeterProfile>,
    val selectedProfileModelId: String?,
    val currentSlaveId: Int?,
    val currentRawValues: Map<Int, Int>
)
