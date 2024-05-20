package org.odk.collect.android.entities

import android.os.StrictMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.odk.collect.entities.EntitiesRepository
import org.odk.collect.entities.Entity
import java.io.File

class JsonFileEntitiesRepository(directory: File) : EntitiesRepository {

    private val entitiesFile = File(directory, "entities.json")

    override fun getLists(): Set<String> {
        return readJson().keys
    }

    override fun getEntities(list: String): List<Entity> {
        return readEntities().filter { it.list == list }
    }

    override fun save(vararg entities: Entity) {
        val storedEntities = readEntities()

        entities.forEach { entity ->
            val existing = storedEntities.find { it.id == entity.id && it.list == entity.list }

            if (existing != null) {
                val offline = if (existing.offline) {
                    entity.offline
                } else {
                    false
                }

                storedEntities.remove(existing)
                storedEntities.add(
                    Entity(
                        entity.list,
                        entity.id,
                        entity.label ?: existing.label,
                        version = entity.version,
                        properties = mergeProperties(existing, entity),
                        offline = offline
                    )
                )
            } else {
                storedEntities.add(entity)
            }
        }

        writeEntities(storedEntities)
    }

    override fun clear() {
        StrictMode.noteSlowCall("Writing to JSON file")
        entitiesFile.delete()
    }

    override fun addList(list: String) {
        val existing = readJson()
        if (!existing.containsKey(list)) {
            existing[list] = mutableListOf()
        }

        writeJson(existing)
    }

    override fun delete(id: String) {
        val existing = readEntities()
        existing.removeIf { it.id == id }
        writeEntities(existing)
    }

    private fun writeEntities(entities: MutableList<Entity>) {
        val map = mutableMapOf<String, MutableList<JsonEntity>>()
        entities.forEach {
            map.getOrPut(it.list) { mutableListOf() }.add(it.toJson())
        }

        writeJson(map)
    }

    private fun readEntities(): MutableList<Entity> {
        return readJson().entries.flatMap { (list, entities) ->
            entities.map { it.toEntity(list) }
        }.toMutableList()
    }

    private fun readJson(): MutableMap<String, MutableList<JsonEntity>> {
        StrictMode.noteSlowCall("Reading from JSON file")

        if (!entitiesFile.exists()) {
            createFile()
        }

        try {
            val typeToken = TypeToken.getParameterized(
                MutableMap::class.java,
                String::class.java,
                TypeToken.getParameterized(MutableList::class.java, JsonEntity::class.java).type
            )

            val json = entitiesFile.readText()
            return if (json.isNotBlank()) {
                val parsedJson =
                    Gson().fromJson<MutableMap<String, MutableList<JsonEntity>>>(json, typeToken.type)

                parsedJson
            } else {
                mutableMapOf()
            }
        } catch (e: Exception) {
            entitiesFile.delete()
            createFile()
            return mutableMapOf()
        }
    }

    private fun createFile() {
        entitiesFile.parentFile.mkdirs()
        entitiesFile.createNewFile()
    }

    private fun writeJson(map: MutableMap<String, MutableList<JsonEntity>>) {
        StrictMode.noteSlowCall("Writing to JSON file")

        val json = Gson().toJson(map)
        entitiesFile.writeText(json)
    }

    private fun mergeProperties(
        existing: Entity,
        new: Entity
    ): List<Pair<String, String>> {
        val existingProperties = mutableMapOf(*existing.properties.toTypedArray())
        new.properties.forEach {
            existingProperties[it.first] = it.second
        }

        return existingProperties.map { Pair(it.key, it.value) }
    }

    private data class JsonEntity(
        val id: String,
        val label: String?,
        val version: Int,
        val properties: Map<String, String>,
        val offline: Boolean
    )

    private fun JsonEntity.toEntity(list: String): Entity {
        return Entity(
            list,
            this.id,
            this.label,
            this.version,
            this.properties.entries.map { Pair(it.key, it.value) },
            this.offline
        )
    }

    private fun Entity.toJson(): JsonEntity {
        return JsonEntity(
            this.id,
            this.label,
            this.version,
            this.properties.toMap(),
            this.offline
        )
    }
}
