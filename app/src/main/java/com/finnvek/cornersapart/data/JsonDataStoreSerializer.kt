package com.finnvek.cornersapart.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class JsonDataStoreSerializer<T>(
    override val defaultValue: T,
    private val serializer: KSerializer<T>,
    private val json: Json = CornersApartJson,
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T =
        try {
            json.decodeFromString(serializer, input.readBytes().decodeToString())
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot read JSON DataStore.", exception)
        } catch (exception: IllegalArgumentException) {
            throw CorruptionException("Cannot read JSON DataStore.", exception)
        }

    override suspend fun writeTo(
        t: T,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(serializer, t).encodeToByteArray())
    }
}

val CornersApartJson: Json =
    Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
