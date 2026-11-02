package app.velora.launcher.core.data.backup

import app.velora.launcher.core.data.favorites.FolderSnapshot
import app.velora.launcher.core.model.AppEntry
import app.velora.launcher.core.model.AppKey
import app.velora.launcher.core.model.ClockStyle
import app.velora.launcher.core.model.Customization
import app.velora.launcher.core.model.FontChoice
import app.velora.launcher.core.model.IconOptions
import app.velora.launcher.core.model.IconPack
import app.velora.launcher.core.model.IconStyle
import app.velora.launcher.core.model.TextSize
import app.velora.launcher.core.model.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileTest {

    private val camera = AppKey("com.example.camera", "com.example.camera.Main", 0)
    private val bank = AppKey("com.bank.app", ".ui.Start\$Inner", 10)

    private val full = VeloraBackup(
        favorites = listOf(camera, bank),
        folders = listOf(FolderSnapshot("Trabajo: día=1 | noche", listOf(bank)), FolderSnapshot("Vacía", emptyList())),
        hidden = listOf(bank),
        widgets = listOf(BackupWidget("com.weather/.Widget", 160)),
        prefs = BackupPrefs(
            preset = ThemePreset.Galaxy,
            font = FontChoice.Cinzel,
            clock = ClockStyle.Day,
            icons = IconStyle.Minimal,
            textSize = TextSize.Large,
            leftHanded = true,
            fullList = false,
            custom = Customization(
                icons = IconOptions(pack = IconPack.Stellar),
                accentColor = 0xFF7FF0E0,
                dismissedTips = setOf("swipe"),
                usageLimits = mapOf("com.social.app" to 30),
            ),
        ),
        photo = byteArrayOf(1, 2, 3),
    )

    @Test
    fun `un respaldo se exporta y se vuelve a leer igual`() {
        assertEquals(full, BackupFile.decode(BackupFile.encode(full)))
    }

    @Test
    fun `un respaldo vacio tambien es valido`() {
        assertEquals(VeloraBackup(), BackupFile.decode(BackupFile.encode(VeloraBackup())))
    }

    @Test
    fun `un archivo ajeno se rechaza`() {
        assertNull(BackupFile.decode(""))
        assertNull(BackupFile.decode("velora-theme 1\npreset=Neon"))
        assertNull(BackupFile.decode("velora-backup 2\nfavorite=com.a.b/Main#0"))
        assertNull(BackupFile.decode("PK\u0003\u0004 zip binario"))
    }

    @Test
    fun `un archivo demasiado grande se rechaza`() {
        val huge = BackupFile.HEADER + "\n" + "x".repeat(BackupFile.MAX_BYTES * 2)

        assertNull(BackupFile.decode(huge))
    }

    @Test
    fun `los campos desconocidos y los invalidos se ignoran sin romper`() {
        val text = """
            ${BackupFile.HEADER}
            script=rm -rf /
            favorite=com.ok.app/com.ok.app.Main#0
            favorite=../../etc/passwd/x#0
            favorite=com.ok.app/Main;drop#0
            favorite=com.ok.app/Main#-3
            hidden=sin-barra
            folder=0:%%%
            folder=x:QQ
            folder.app=7:com.ok.app/Main#0
            widget=com.w/.W|nope
            widget=com.w/.W|99999
            pref.font=Inexistente
            pref.leftHanded=quizas
            icons.thirdParty=no es paquete
            limits=com.ok.app:15,mal paquete:5
            photo=%%%
        """.trimIndent()

        val backup = BackupFile.decode(text)

        assertEquals(listOf(AppKey("com.ok.app", "com.ok.app.Main", 0)), backup?.favorites)
        assertTrue(backup?.hidden.orEmpty().isEmpty())
        assertTrue(backup?.folders.orEmpty().isEmpty())
        assertEquals(listOf(BackupWidget("com.w/.W", 360)), backup?.widgets)
        assertNull(backup?.prefs?.font)
        assertNull(backup?.prefs?.leftHanded)
        assertNull(backup?.prefs?.custom?.icons?.thirdPartyPack)
        assertEquals(mapOf("com.ok.app" to 15), backup?.prefs?.custom?.usageLimits)
        assertNull(backup?.photo)
    }

    @Test
    fun `las apps de una carpeta van con su carpeta aunque vengan desordenadas`() {
        val text = """
            ${BackupFile.HEADER}
            folder.app=1:com.b.app/Main#0
            folder=1:QmFuY29z
            folder=0:VHJhYmFqbw
            folder.app=0:com.a.app/Main#0
        """.trimIndent()

        val folders = BackupFile.decode(text)?.folders

        assertEquals(
            listOf(
                FolderSnapshot("Trabajo", listOf(AppKey("com.a.app", "Main", 0))),
                FolderSnapshot("Bancos", listOf(AppKey("com.b.app", "Main", 0))),
            ),
            folders,
        )
    }

    @Test
    fun `en otro telefono la app se busca por componente prefiriendo el perfil personal`() {
        val personal = entry(AppKey("com.example.camera", "com.example.camera.Main", 0), work = false)
        val work = entry(AppKey("com.example.camera", "com.example.camera.Main", 11), work = true)
        val saved = AppKey("com.example.camera", "com.example.camera.Main", 42)
        val notInstalled = AppKey("com.not.here", "Main", 0)

        val resolved = BackupMatcher.resolve(listOf(saved, notInstalled), listOf(work, personal))

        assertEquals(listOf(personal.key, notInstalled), resolved)
    }

    private fun entry(key: AppKey, work: Boolean) = AppEntry(
        key = key,
        label = key.packageName,
        section = "C",
        searchKey = key.packageName,
        isWorkProfile = work,
        canUninstall = true,
    )
}
