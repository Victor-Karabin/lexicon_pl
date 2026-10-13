package com.lexicon.application.course

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class LessonAsset(number: Int) {
    private val screens = Json.parseToJsonElement(File("../data/src/androidMain/assets/lesson_krok-a1-%02d.json".format(number)).readText())
        .jsonObject.getValue("screens").jsonArray.map { it.jsonObject }

    fun screen(id: String): JsonObject = screens.first { it.getValue("id").jsonPrimitive.content == id }

    fun items(id: String): List<JsonObject> = screen(id).getValue("items").jsonArray.map { it.jsonObject }

    fun gap(
        id: String,
        number: Int,
    ): List<String> =
        screen(id).getValue("gaps").jsonArray.map { it.jsonObject }
            .first { it.getValue("number").jsonPrimitive.content == number.toString() }.list("answers")
}

fun JsonObject.list(key: String): List<String> = getValue(key).jsonArray.map { it.jsonPrimitive.content }
