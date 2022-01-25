package commands.fates

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import commands.Command
import commands.fates.api.History
import commands.fates.api.HistoryRequest
import commands.fates.api.WishData
import dev.minn.jda.ktx.EmbedBuilder
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.components.Button
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL

class WishHistory: Command {
    private val log by SLF4J
    private lateinit var hr: HistoryRequest

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/wishhistory loaded")

        event.jda.upsertCommand("wishhistory", "Retrieve wish history.") {
            option<String>("banner", "Banner to retrieve history from", true)
            option<String>("url", "URL containing API key", true)
        }.await()
    }

    override suspend fun execute(event: SlashCommandEvent) {
        event.deferReply(false).await()
        event.hook.expirationTimestamp

        hr = HistoryRequest()

        hr.bannerType = when (event.getOption("banner")?.asString) {
            "beginner" -> 100
            "standard" -> 200
            "character" -> 301
            "character2" -> 400
            "weapon" -> 302
            else -> 301
        }

        val queries = HashMap<String, String>().apply {
            event.getOption("url")?.let {
                URL(it.asString).query.split("&").forEach { param ->
                    put(param.split("=")[0], param.split("=")[1])
                }
            }
        }

        hr.authKey = queries["authkey"]!!
        hr.currentPage = queries["page"]!!.toInt()

        try {
            val wishData = getWishData(hr, event.jda.httpClient)
            hr.lastWishId = wishData.list.last().id

            event.hook.editOriginalEmbeds(formEmbed(wishData)).await()

            event.hook.editOriginalComponents().setActionRow(
                Button.primary("prev", "\u25c0\ufe0f").asDisabled(),
                Button.primary("next", "\u25b6\ufe0f")
            ).await()
        } catch (e: Exception) {
            event.hook.editOriginal("Something went wrong...").await()
            e.printStackTrace()
        }
    }

    override suspend fun handleButtons(event: ButtonClickEvent) {
        event.deferEdit().await()

        var wishData = WishData()

        when (event.componentId) {
            "prev" -> {
                hr.currentPage -= 1
                wishData = getWishData(hr, event.jda.httpClient)
                hr.lastWishId = wishData.list.last().id

                if (hr.currentPage == 1) {
                    event.hook.editOriginalComponents().setActionRow(
                        Button.primary("prev", "\u25c0\ufe0f").asDisabled(),
                        Button.primary("next", "\u25b6\ufe0f")
                    ).await()
                } else {
                    event.hook.editOriginalComponents().setActionRow(
                        Button.primary("prev", "\u25c0\ufe0f"),
                        Button.primary("next", "\u25b6\ufe0f")
                    ).await()
                }
            }
            "next" -> {
                hr.currentPage += 1
                wishData = getWishData(hr, event.jda.httpClient)
                hr.lastWishId = wishData.list.last().id

                event.hook.editOriginalComponents().setActionRow(
                    Button.primary("prev", "\u25c0\ufe0f"),
                    Button.primary("next", "\u25b6\ufe0f")
                ).await()
            }
        }

        event.message.editMessageEmbeds(formEmbed(wishData)).await()
    }

    private fun formEmbed(wishData: WishData): MessageEmbed = EmbedBuilder {
        title = "History for UID ${wishData.list.last().uid}"

        description = StringBuilder().apply {
            wishData.list.forEach { wish -> appendLine("${wish.rankType}★ - ${wish.name}") }
        }.toString()

        footer {
            name = "Page ${hr.currentPage}"
        }
    }.build()

    private fun getWishData(historyRequest: HistoryRequest, httpClient: OkHttpClient): WishData {
        val requestUrl = StringBuilder()
            .append("https://hk4e-api-os.mihoyo.com/event/gacha_info/api/getGachaLog?")
            .append("authkey_ver=1")
            .append("&sign_type=2")
            .append("&auth_appid=webview_gacha")
            .append("&init_type=${historyRequest.bannerType}")
            .append("&lang=en")
            .append("&authkey=${historyRequest.authKey}")
            .append("&gacha_type=${historyRequest.bannerType}")
            .append("&page=${historyRequest.currentPage}")
            .append("&size=12")
            .append("&end_id=${historyRequest.lastWishId}")
            .toString()

        val request = Request.Builder().url(requestUrl).build()

        val mapper = JsonMapper.builder()
            .addModule(KotlinModule.Builder().configure(KotlinFeature.StrictNullChecks, true).build())
            .build()

        val response = httpClient.newCall(request).execute().body

        return mapper.readValue(response?.string(), History::class.java).data
    }
}