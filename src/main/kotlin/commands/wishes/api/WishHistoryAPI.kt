package commands.wishes.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.sql.Table
import kotlin.properties.Delegates

typealias AuthKey = String
typealias CachedWishJSON = String?
typealias Wishes = Map<BannerType, List<WishData>>

class History {
    var retcode by Delegates.notNull<Int>()
    lateinit var message: String
    val data: WishData? = null
}

@JsonIgnoreProperties("page", "size", "total", "region")
class WishData {
    lateinit var list: List<WishInstance>
}

@JsonIgnoreProperties("count", "lang", "gacha_type", "item_id", "item_type")
class WishInstance {
    lateinit var uid: String
    @Suppress("unused") lateinit var time: String
    lateinit var name: String
    @JsonProperty("rank_type") lateinit var rankType: String
    lateinit var id: String
}

class CachedWish(val wishes: Wishes)

object WishTable: Table() {
    val userId = varchar("user_id", 18).check { it.isNotNull() }
    val authKey = varchar("authkey", 1100).check { it.isNotNull() }
    // Long JSON string...
    val cachedWishJson = varchar("cached_wish", 1_000_000_000).nullable()
}

enum class BannerType(val value: Int) {
    BEGINNER(100),
    PERMANENT(200),
    CHARACTER(301),
    WEAPON(302),
    CHARACTER2(400)
}

class HistoryRequest(var authKey: String, var bannerType: BannerType = BannerType.CHARACTER, var pageNumber: Int = 1)