package commands.wishes

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import commands.api.ClientCommand
import commands.api.CommandException
import commands.api.UserID
import commands.wishes.api.*
import dev.minn.jda.ktx.EmbedBuilder
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes

class WishHistory : ClientCommand {
    private val log by SLF4J
    private val requests = HashMap<UserID, HistoryRequest>()
    private val requestResults = HashMap<UserID, Wishes>()
    private lateinit var timeout: Job

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val authKey = Wish.entries[event.user.id]

        if (authKey == null) {
            event.hook.editOriginal("No API key is set.")
            return
        }

        event.getOption("banner")?.let {
            val bannerType = BannerType.fromValue(it.asLong)
            requests[event.user.id] = HistoryRequest(authKey, bannerType)
        }

        try {
            val hr = requests[event.user.id]!!

            // Retrieve stored wish history from the cache
            if (Wish.caches[event.user.id] != null) {
                val mapper = JsonMapper.builder()
                    .addModule(KotlinModule.Builder().configure(KotlinFeature.StrictNullChecks, true).build())
                    .build()

                val cached = mapper.readValue<CachedWish>(Wish.caches[event.user.id]!!)

                requestResults[event.user.id] = cached.wishes
            } else {
                requestResults[event.user.id] = requestWishData(hr, event.user.id, event.jda.httpClient)
            }
        } catch (e: CommandException) {
            log.error(e.localizedMessage)
            event.hook.editOriginal(e.responseMessage()).await()
            return
        }

        val data = requestResults[event.user.id]?.get(requests[event.user.id]?.bannerType)!!

        event.hook.editOriginalEmbeds(
            formEmbed(data[requests[event.user.id]!!.pageNumber], requests[event.user.id]!!.pageNumber)
        ).await()

        // We are guaranteed to be on the first page, so disable the "prev" button.
        event.hook.editOriginalComponents().setActionRow(
            Button.primary("prev", "\u25c0\ufe0f").asDisabled(),
            Button.primary("next", "\u25b6\ufe0f")
        ).await()

        timeout = executor.launch {
            delay(2.minutes)
            event.hook.editOriginalComponents().await()
        }
    }

    override suspend fun handleButtons(event: ButtonInteractionEvent) {
        timeout.cancel()

        if (requestResults[event.user.id]?.get(requests[event.user.id]?.bannerType)!!.isEmpty())
            return

        when (event.componentId) {
            "prev" -> handlePrev(event)
            "next" -> handleNext(event)
        }
    }

    private suspend fun handlePrev(event: ButtonInteractionEvent) {
        requests[event.user.id]!!.pageNumber -= 1

        val data = requestResults[event.user.id]?.get(requests[event.user.id]?.bannerType)!!

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

        timeout = executor.launch {
            delay(1.minutes)
            event.hook.editOriginalComponents().await()
        }
    }

    private suspend fun handleNext(event: ButtonInteractionEvent) {
        requests[event.user.id]!!.pageNumber += 1

        val data = requestResults[event.user.id]?.get(requests[event.user.id]?.bannerType)!!

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

        event.message.editMessageEmbeds(
            formEmbed(data[requests[event.user.id]!!.pageNumber], requests[event.user.id]!!.pageNumber)
        ).await()

        timeout = executor.launch {
            delay(1.minutes)
            event.hook.editOriginalComponents().await()
        }
    }

    private fun formEmbed(wishData: WishData, page: Int): MessageEmbed {
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val targetDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")

        return EmbedBuilder {
            title = "History for UID ${wishData.list.last().uid}"

            description = StringBuilder().apply {
                wishData.list.forEach { wish ->
                    val pullDate = LocalDate.parse(wish.time, dateFormat)
                    val pullTime = pullDate.format(targetDateFormat)

                    appendLine("`[$pullTime]` ${wish.rankType}★ - ${wish.name}")
                }
            }.toString()

            footer {
                name = "Page $page"
            }
        }.build()
    }


    @Throws(CommandException::class)
    private suspend fun requestWishData(hr: HistoryRequest, userId: UserID, http: OkHttpClient): Wishes {
        val wishes: MutableMap<BannerType, List<WishData>> = mutableMapOf()

        val mapper = JsonMapper.builder()
            .addModule(KotlinModule.Builder().configure(KotlinFeature.StrictNullChecks, true).build())
            .build()

        enumValues<BannerType>().forEach { banner ->
            val pages: MutableList<WishData> = mutableListOf()
            var lastWishId = "0"

            // Make a request at least every 500ms to avoid getting rate-limited.
            delay(500L)

            val initialRequestUrl = StringBuilder()
                .append("https://hk4e-api-os.mihoyo.com/event/gacha_info/api/getGachaLog?")
                .append("authkey_ver=1")
                .append("&sign_type=2")
                .append("&auth_appid=webview_gacha")
                .append("&init_type=${banner.value}")
                .append("&lang=en")
                .append("&authkey=${hr.authKey}")
                .append("&gacha_type=${banner.value}")
                .append("&page=1")
                .append("&size=12")
                .append("&end_id=${lastWishId}")
                .toString()

            val initialRequest = Request.Builder().url(initialRequestUrl).build()

            http.newCall(initialRequest).execute().use { initialResponse ->
                if (!initialResponse.isSuccessful)
                    throw CommandException("Failed to request data from API. Got code ${initialResponse.code}", 1)

                val initialHistory = mapper.readValue<History>(initialResponse.body!!.string())

                if (initialHistory.data == null) {
                    throw CommandException(
                        "Wish data is null, API returned code ${initialHistory.retcode}, for reason: ${initialHistory.message}",
                        initialHistory.retcode
                    )
                } else {
                    pages.add(0, initialHistory.data)

                    var pageIndex = 1

                    while (true) {
                        // Make a request at least every 500ms to avoid getting rate-limited.
                        delay(500L)

                        val apiUrl = StringBuilder()
                            .append("https://hk4e-api-os.mihoyo.com/event/gacha_info/api/getGachaLog?")
                            .append("authkey_ver=1")
                            .append("&sign_type=2")
                            .append("&auth_appid=webview_gacha")
                            .append("&init_type=${banner.value}")
                            .append("&lang=en")
                            .append("&authkey=${hr.authKey}")
                            .append("&gacha_type=${banner.value}")
                            .append("&page=${pageIndex + 1}")
                            .append("&size=12")
                            .append("&end_id=${lastWishId}")
                            .toString()

                        val request = Request.Builder().url(apiUrl).build()
                        val response = http.newCall(request).execute()

                        if (!response.isSuccessful)
                            throw CommandException("Failed to request data from API. Got code ${response.code}", 1)

                        val history = mapper.readValue<History>(response.body!!.string())

                        if (history.data == null) {
                            throw CommandException(
                                "Wish data is null, API returned code ${history.retcode}, for reason: ${history.message}",
                                history.retcode
                            )
                        } else {
                            pages.add(pageIndex, history.data)

                            if (pages[pageIndex].list.isEmpty())
                                break

                            lastWishId = pages[pageIndex].list.last().id

                            pageIndex++
                        }
                    }
                }

                wishes[banner] = pages
            }
        }

        // Cache the result, for future use
        val cachedWish = mapper.writeValueAsString(CachedWish(wishes))

        Wish.caches[userId] = cachedWish

        transaction {
            WishTable.update({ WishTable.userId eq userId }) {
                it[cachedWishJson] = cachedWish
            }
        }

        return wishes
    }
}