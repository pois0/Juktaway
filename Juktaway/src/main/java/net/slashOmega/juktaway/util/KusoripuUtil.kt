package net.slashOmega.juktaway.util

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import jp.nephy.jsonkt.JsonObject
import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.jsonkt.delegation.int
import jp.nephy.jsonkt.delegation.model
import jp.nephy.jsonkt.delegation.string
import jp.nephy.jsonkt.parse

/**
 * Created on 2018/11/18.
 */
object KusoripuUtil {
    suspend fun getKusoripu(screenName: String) =
        HttpClient().use { client ->
            val response = client.get<String>(scheme = "https", host = "api.nephy.jp", path = "/v1/kusoripu/random?screen_name=$screenName")
            response.parse<RepooplyModel>().text.raw
        }

    class RepooplyModel(override val json: JsonObject): JsonModel {
        val count by int
        class UserModel(override val json: JsonObject): JsonModel {
            val count by int
            val screen_name by string
        }
        val target by model<UserModel>()
        class TextModel(override val json: JsonObject): JsonModel {
            val raw by string
            val html by string
            val url by string
        }
        val text by model<TextModel>()
    }
}