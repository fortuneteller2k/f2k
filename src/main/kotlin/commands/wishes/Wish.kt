package commands.wishes

import commands.api.Command
import commands.api.UserID
import commands.wishes.api.AuthKey
import commands.wishes.api.CachedWishJSON
import commands.wishes.api.WishTable
import commands.wishes.api.WishTable.authKey
import commands.wishes.api.WishTable.cachedWishJson
import commands.wishes.api.WishTable.userId
import dev.minn.jda.ktx.Embed
import dev.minn.jda.ktx.SLF4J
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.option
import dev.minn.jda.ktx.interactions.subcommand
import dev.minn.jda.ktx.interactions.upsertCommand
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.net.URL
import java.time.Instant

class Wish : Command {
    private val log by SLF4J

    companion object {
        internal val entries: MutableMap<UserID, AuthKey> = HashMap()
        internal val caches: MutableMap<UserID, CachedWishJSON> = HashMap()

        suspend fun describe(event: SlashCommandEvent) {
            event.replyEmbeds(
                Embed {
                    title = "/wish [get/set/history] [url/banner]"
                    description = "Retrieve wish history."
                    color = 0x000000
                    timestamp = Instant.now()
                    thumbnail = "https://cdn2.steamgriddb.com/file/sgdb-cdn/logo/2465517595f5ea9f225d52ed73a4d0db.png"

                    field {
                        name = "Example"
                        value = """
                            `/wish set url: https://hk4e-api-os.mihoyo.com/event/gacha_info/api/getGachaLog?authkey_ver=1&sign_type=2&auth_appid=webview_gacha&init_type=301&lang=en&authkey=X&gacha_type=301&page=1&size=6&end_id=0`
                            `/wish get`
                            `/wish history banner: character`
                        """.trimIndent()
                        inline = false
                    }

                    field {
                        name = "Valid values for `banner`"
                        value = """
                            `beginner` - Beginner Wish
                            `standard` - Standard Wish
                            `character` -  Character Event Wish
                            `character2` - Character Event Wish-2
                            `weapon` - Weapon Event Wish
                        """.trimIndent()
                        inline = false
                    }
                }
            ).await()
        }
    }

    override suspend fun initialize(event: ReadyEvent) {
        log.info("/wish loaded")

        event.jda.upsertCommand("wish", "Wish history related commands.") {
            subcommand("get", "Retrieve API key.")

            subcommand("set", "Save API key from URL.") {
                option<String>("url", "URL containing API key", true)
            }

            subcommand("history", "Retrieve wish history from API.") {
                option<String>("banner", "Banner to retrieve history from", true)
            }

            subcommand("invalidate", "Invalidates any cached wish history.")
        }.await()

        transaction {
            SchemaUtils.createMissingTablesAndColumns(WishTable)

            WishTable.selectAll().forEach {
                entries[it[userId]] = it[authKey]
                caches[it[userId]] = it[cachedWishJson]
            }
        }
    }

    override suspend fun execute(event: SlashCommandEvent) {
        event.deferReply(false).await()

        when (event.subcommandName) {
            "get" -> {
                val userKey = entries[event.user.id]

                if (userKey == null)
                    event.hook.editOriginal("No key found in database.").await()
                else
                    event.hook.editOriginal(userKey).await()
            }
            "set" -> {
                val key = HashMap<String, String>().apply {
                    event.getOption("url")?.let {
                        URL(it.asString).query.split("&").forEach { param ->
                            put(param.split("=")[0], param.split("=")[1])
                        }
                    }
                }["authkey"]!!

                if (entries[event.user.id] != null) {
                    entries[event.user.id] = key

                    transaction {
                        WishTable.update({ userId eq event.user.id }) {
                            it[authKey] = key
                        }
                    }

                    event.hook.editOriginal("Key updated successfully.").await()
                } else {
                    entries[event.user.id] = key

                    transaction {
                        WishTable.insert {
                            it[userId] = event.user.id
                            it[authKey] = key
                        }
                    }

                    event.hook.editOriginal("Key stored successfully.").await()
                }
            }
            "invalidate" -> {
                if (caches[event.user.id] != null) {
                    caches[event.user.id] = null

                    transaction {
                        WishTable.update({ userId eq event.user.id }) {
                            it[cachedWishJson] = null
                        }
                    }

                    event.hook.editOriginal("Cache invalidated.").await()
                } else {
                    event.hook.editOriginal("No cached history to invalidate.").await()
                }
            }
        }
    }
}