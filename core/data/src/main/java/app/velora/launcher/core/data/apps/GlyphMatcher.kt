package app.velora.launcher.core.data.apps

import java.text.Normalizer
import java.util.Locale

/**
 * Elige el ícono en línea de una app para el estilo Minimalista (ADR-021): primero por paquete
 * (las apps conocidas tienen su logo), luego por palabras del nombre o del paquete, luego por la
 * categoría que declara la app. null = se dibuja la inicial. Kotlin puro.
 */
object GlyphMatcher {

    fun match(packageName: String, label: String, category: Int): String? = byPackage[packageName]
        ?: byKeyword(normalize("$label $packageName"))
        ?: byCategory[category]

    private fun byKeyword(text: String): String? =
        keywords.firstOrNull { (words, _) -> words.any { it in text } }?.second

    private fun normalize(text: String) =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(marks, "")

    private val marks = Regex("\\p{Mn}+")

    private val byPackage = mapOf(
        "com.whatsapp" to "brand-whatsapp",
        "com.whatsapp.w4b" to "brand-whatsapp",
        "com.instagram.android" to "brand-instagram",
        "com.instagram.barcelona" to "brand-threads",
        "com.facebook.katana" to "brand-facebook",
        "com.facebook.lite" to "brand-facebook",
        "com.facebook.orca" to "brand-messenger",
        "org.telegram.messenger" to "brand-telegram",
        "com.zhiliaoapp.musically" to "brand-tiktok",
        "com.ss.android.ugc.trill" to "brand-tiktok",
        "com.google.android.youtube" to "brand-youtube",
        "com.google.android.apps.youtube.music" to "brand-youtube",
        "com.google.android.apps.youtube.kids" to "brand-youtube-kids",
        "com.spotify.music" to "brand-spotify",
        "com.netflix.mediaclient" to "brand-netflix",
        "com.twitter.android" to "brand-x",
        "com.discord" to "brand-discord",
        "com.reddit.frontpage" to "brand-reddit",
        "com.snapchat.android" to "brand-snapchat",
        "com.pinterest" to "brand-pinterest",
        "com.linkedin.android" to "brand-linkedin",
        "com.google.android.gm" to "brand-gmail",
        "com.android.chrome" to "brand-chrome",
        "com.google.android.apps.maps" to "brand-google-maps",
        "com.google.android.apps.photos" to "brand-google-photos",
        "com.google.android.apps.docs" to "brand-google-drive",
        "com.android.vending" to "brand-google-play",
        "com.google.android.googlequicksearchbox" to "brand-google",
        "com.google.android.apps.chromecast.app" to "brand-google-home",
        "com.niksoftware.snapseed" to "brand-snapseed",
        "us.zoom.videomeetings" to "brand-zoom",
        "com.amazon.mShop.android.shopping" to "brand-amazon",
        "com.paypal.android.p2pmobile" to "brand-paypal",
        "tv.twitch.android.app" to "brand-twitch",
        "com.valvesoftware.android.steam.community" to "brand-steam",
        "com.waze" to "brand-waze",
        "com.airbnb.android" to "brand-airbnb",
        "com.shazam.android" to "brand-shazam",
        "com.soundcloud.android" to "brand-soundcloud",
        "deezer.android.app" to "brand-deezer",
        "org.thoughtcrime.securesms" to "brand-signal",
        "com.skype.raider" to "brand-skype",
        "com.tinder" to "brand-tinder",
        "org.wikipedia" to "brand-wikipedia",
        "org.mozilla.firefox" to "brand-firefox",
        "com.opera.browser" to "brand-opera",
        "com.github.android" to "brand-github",
        "org.videolan.vlc" to "brand-vlc",
        "notion.id" to "brand-notion",
        "com.trello" to "brand-trello",
        "com.Slack" to "brand-slack",
        "com.dropbox.android" to "brand-dropbox",
        "com.microsoft.emmx" to "brand-edge",
        "com.microsoft.skydrive" to "brand-onedrive",
        "com.microsoft.teams" to "brand-teams",
        "com.microsoft.office.officehubrow" to "brand-office",
        "com.ubercab" to "brand-uber",
        "com.binance.dev" to "brand-binance",
        "com.strava" to "brand-strava",
        "com.tumblr" to "brand-tumblr",
        "com.vimeo.android.videoapp" to "brand-vimeo",
        "com.patreon.android" to "brand-patreon",
        "com.mojang.minecraftpe" to "brand-minecraft",
        "com.microsoft.xboxone.smartglass" to "brand-xbox",
        "com.openai.chatgpt" to "brand-openai",
        "com.supercell.clashroyale" to "crown",
        "com.supercell.brawlstars" to "skull",
        "com.supercell.clashofclans" to "sword",
        "com.chess" to "chess",
        "com.google.android.keep" to "notes",
        "com.google.android.apps.messaging" to "message",
        "com.google.android.dialer" to "phone",
        "com.google.android.contacts" to "address-book",
        "com.google.android.deskclock" to "clock",
        "com.google.android.calculator" to "calculator",
        "com.google.android.GoogleCamera" to "camera",
        "com.google.android.calendar" to "calendar",
        "com.android.settings" to "settings",
        "com.google.android.apps.translate" to "language",
        "com.google.android.apps.authenticator2" to "shield-lock",
        "com.google.android.apps.walletnfcrel" to "wallet",
        "com.google.android.apps.fitness" to "heart",
        "com.google.android.apps.nbu.files" to "folder",
        "com.google.android.apps.tachyon" to "video",
    )

    /** Palabras (sin tildes, en minúsculas) → ícono. El orden importa: lo más específico primero. */
    private val keywords: List<Pair<List<String>, String>> = listOf(
        listOf("authenticator", "autenticador") to "shield-lock",
        listOf("vault", "boveda", "applock") to "lock",
        listOf("camera", "camara") to "camera",
        listOf("deskclock", "clock", "reloj", "alarm", "alarma") to "clock",
        listOf("calculator", "calculadora") to "calculator",
        listOf("calendar", "calendario", "agenda") to "calendar",
        listOf("contacts", "contactos") to "address-book",
        listOf("dialer", "telefono", "phone", "llamadas") to "phone",
        listOf("messag", "mensaje", "sms", "mms") to "message",
        listOf("gallery", "galeria", "photos", "fotos") to "photo",
        listOf("filemanager", "archivos", "files", "documents", "documentos") to "folder",
        listOf("notes", "notas", "memo") to "notes",
        listOf("weather", "clima") to "cloud",
        listOf("email", "mail", "correo", "outlook") to "mail",
        listOf("browser", "navegador") to "world",
        listOf("store", "tienda", "market", "shop") to "shopping-bag",
        listOf("maps", "mapas", "navigation") to "map",
        listOf("chess", "ajedrez") to "chess",
        listOf("bible", "biblia") to "book",
        listOf("recorder", "grabadora", "voice") to "microphone",
        listOf("podcast") to "microphone-2",
        listOf("compass", "brujula") to "compass",
        listOf("flashlight", "linterna", "torch") to "bulb",
        listOf("health", "salud", "fitness") to "heart",
        listOf("wallet", "billetera", "cartera") to "wallet",
        listOf("bank", "banco", "nubank", "bancolombia", "nequi", "daviplata") to "building-bank",
        listOf("card", "tarjeta") to "credit-card",
        listOf("music", "musica", "radio") to "music",
        listOf("video", "movie", "pelicula", "player") to "movie",
        listOf("translate", "traductor") to "language",
        listOf("news", "noticias") to "news",
        listOf("scan", "escaner") to "scan",
        listOf("security", "seguridad") to "shield",
        listOf("cleaner", "limpiador") to "trash",
        listOf("vpn") to "network",
        listOf("download", "descarga") to "download",
        listOf("launcher") to "layout-grid",
        listOf("book", "libro", "kindle", "lectura") to "book-2",
        listOf("paint", "draw", "dibujo") to "brush",
        listOf("theme", "tema", "wallpaper", "fondo") to "palette",
        listOf("game", "juego") to "device-gamepad-2",
        listOf("settings", "ajustes", "configuracion") to "settings",
    )

    /**
     * `ApplicationInfo.CATEGORY_*`: juego, audio, video, imagen, social, noticias, mapas,
     * productividad, accesibilidad.
     */
    private val byCategory = mapOf(
        0 to "device-gamepad-2",
        1 to "headphones",
        2 to "movie",
        3 to "photo",
        4 to "users",
        5 to "news",
        6 to "map",
        7 to "checklist",
        8 to "accessible",
    )
}
