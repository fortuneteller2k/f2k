package commands.wishs

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import commands.Command
import commands.wishs.api.BannerType
import commands.wishs.api.History
import commands.wishs.api.HistoryRequest
import commands.wishs.api.WishData
import dev.minn.jda.ktx.EmbedBuilder
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.components.Button
import okhttp3.OkHttpClient
import okhttp3.Request

class WishHistory: Command {
    private val requests: MutableMap<String, HistoryRequest> = HashMap()
    private val requestResults: MutableMap<String, List<WishData>> = HashMap()
    private val log by SLF4J

    override suspend fun execute(event: SlashCommandEvent) {
        event.deferReply(false).await()

        val authKey = Wish.entries[event.user.id]

        if (authKey == null) {
            event.hook.editOriginal("No API key is set.")
            return
        }

        requests[event.user.id] = HistoryRequest(authKey)

        event.getOption("banner")?.let {
            val bannerType = when (it.asString) {
                "beginner" -> BannerType.BEGINNER
                "standard" -> BannerType.PERMANENT
                "character" -> BannerType.CHARACTER
                "character2" -> BannerType.CHARACTER2
                "weapon" -> BannerType.WEAPON
                else -> BannerType.CHARACTER
            }

            requests[event.user.id]?.bannerType = bannerType
        }

        requestResults[event.user.id] = requestWishData(requests[event.user.id]!!, event.jda.httpClient)

        val data = requestResults[event.user.id]!!

        log.info(data.toString())


        event.hook.editOriginalEmbeds(
            formEmbed(data[requests[event.user.id]!!.pageNumber], requests[event.user.id]!!.pageNumber)
        ).await()

        // We are guaranteed to be on the first page, so disable the "prev" button.
        event.hook.editOriginalComponents().setActionRow(
            Button.primary("prev", "\u25c0\ufe0f").asDisabled(),
            Button.primary("next", "\u25b6\ufe0f")
        ).await()
    }

    override suspend fun handleButtons(event: ButtonClickEvent) {
        event.deferEdit().await()

        when (event.componentId) {
            "prev" -> handlePrev(event)
            "next" -> handleNext(event)
        }
    }

    private suspend fun handlePrev(event: ButtonClickEvent) {
        requests[event.user.id]!!.pageNumber -= 1

        val data = requestResults[event.user.id]!!

        if (requests[event.user.id]!!.pageNumber <= 1) {
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

        event.message.editMessageEmbeds(
            formEmbed(data[requests[event.user.id]!!.pageNumber], requests[event.user.id]!!.pageNumber)
        ).await()
    }

    private suspend fun handleNext(event: ButtonClickEvent) {
        requests[event.user.id]!!.pageNumber += 1

        val data = requestResults[event.user.id]!!

        if (data.size <= requests[event.user.id]!!.pageNumber + 1) {
            event.hook.editOriginalComponents().setActionRow(
                Button.primary("prev", "\u25c0\ufe0f"),
                Button.primary("next", "\u25b6\ufe0f").asDisabled()
            ).await()
        } else {
            event.hook.editOriginalComponents().setActionRow(
                Button.primary("prev", "\u25c0\ufe0f"),
                Button.primary("next", "\u25b6\ufe0f")
            ).await()
        }

        log.info(data[requests[event.user.id]!!.pageNumber].toString())

        event.message.editMessageEmbeds(
            formEmbed(data[requests[event.user.id]!!.pageNumber], requests[event.user.id]!!.pageNumber)
        ).await()
    }

    private fun formEmbed(wishData: WishData, page: Int): MessageEmbed = EmbedBuilder {
        title = "History for UID ${wishData.list.last().uid}"

        description = StringBuilder().apply {
            wishData.list.forEach { wish -> appendLine("${wish.rankType}★ - ${wish.name}") }
        }.toString()

        footer {
            name = "Page $page"
        }
    }.build()

    private suspend fun requestWishData(historyRequest: HistoryRequest, httpClient: OkHttpClient): List<WishData> {
        val pages: MutableList<WishData> = mutableListOf()
        var lastWishId = "0"

        val initialRequestUrl = StringBuilder()
            .append("https://hk4e-api-os.mihoyo.com/event/gacha_info/api/getGachaLog?")
            .append("authkey_ver=1")
            .append("&sign_type=2")
            .append("&auth_appid=webview_gacha")
            .append("&init_type=${historyRequest.bannerType}")
            .append("&lang=en")
            .append("&authkey=${historyRequest.authKey}")
            .append("&gacha_type=${historyRequest.bannerType.value}")
            .append("&page=1")
            .append("&size=12")
            .append("&end_id=${lastWishId}")
            .toString()

        val initialRequest = Request.Builder().url(initialRequestUrl).build()

        val mapper = JsonMapper.builder()
            .addModule(KotlinModule.Builder().configure(KotlinFeature.StrictNullChecks, true).build())
            .build()

        val response = httpClient.newCall(initialRequest).execute().body

        pages.add(0, mapper.readValue(response?.string(), History::class.java).data)

        var page = 1

        while (true) {
            delay(1000L)

            val sb = StringBuilder()
                .append("https://hk4e-api-os.mihoyo.com/event/gacha_info/api/getGachaLog?")
                .append("authkey_ver=1")
                .append("&sign_type=2")
                .append("&auth_appid=webview_gacha")
                .append("&init_type=${historyRequest.bannerType}")
                .append("&lang=en")
                .append("&authkey=${historyRequest.authKey}")
                .append("&gacha_type=${historyRequest.bannerType.value}")
                .append("&page=${page + 1}")
                .append("&size=12")
                .append("&end_id=${lastWishId}")
                .toString()

            val req = Request.Builder().url(sb).build()
            val resp = httpClient.newCall(req).execute().body

            pages.add(page, mapper.readValue(resp?.string(), History::class.java).data)

            if (pages[page].list.isEmpty()) {
                break
            }

            lastWishId = pages[page].list.last().id

            page++
        }

        return pages
    }
}