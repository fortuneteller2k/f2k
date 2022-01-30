package commands.wishes

import commands.api.ClientCommand
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
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.net.URL
import java.time.Instant

class Wish : ClientCommand {
    private val log by SLF4J

    companion object {
        internal val entries: MutableMap<UserID, AuthKey> = HashMap()
        internal val caches: MutableMap<UserID, CachedWishJSON> = HashMap()

        suspend fun describe(event: SlashCommandInteractionEvent) {
            event.replyEmbeds(
                Embed {
                    title = "/wish"
                    description = "Retrieve wish history."
                    color = 0x6873be
                    timestamp = Instant.now()
                    thumbnail = "https://cdn2.steamgriddb.com/file/sgdb-cdn/logo/2465517595f5ea9f225d52ed73a4d0db.png"

                    field {
                        name = "Example"
                        value = """
                            `/wish set url: https://hk4e-api-os.mihoyo.com/event/gacha_info/api/...`
                            `/wish get`
                            `/wish history banner: Character Event Wish`
                        """.trimIndent()
                        inline = false
                    }
                }
            ).await()
        }
    }

    override suspend fun initialize(event: ReadyEvent): CommandData {
        log.info("/wish loaded")

        transaction {
            SchemaUtils.createMissingTablesAndColumns(WishTable)

            WishTable.selectAll().forEach {
                entries[it[userId]] = it[authKey]
                caches[it[userId]] = it[cachedWishJson]
            }
        }

        return Commands.slash("wish", "Wish history related commands.")
            .addSubcommands(
                SubcommandData("get", "Retrieve API key."),
                SubcommandData("set", "Save API key from URL.")
                    .addOption(OptionType.STRING, "url", "URL containing API key", true),
                SubcommandData("history", "Retrieve wish history from API.")
                    .addOptions(
                        OptionData(OptionType.INTEGER, "banner", "Banner to retrieve history from", true)
                            .addChoices(
                                Command.Choice("Standard Wish", 200),
                                Command.Choice("Character Event Wish", 301),
                                Command.Choice("Character Event Wish-2", 400),
                                Command.Choice("Weapon Event Wish", 302)
                            )
                    ),
                SubcommandData("invalidate", "Invalidates any cached wish history.")
            )
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
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
                            it[cachedWishJson] = null
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