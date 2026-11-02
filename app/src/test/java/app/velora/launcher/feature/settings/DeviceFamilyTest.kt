package app.velora.launcher.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceFamilyTest {

    @Test
    fun `cada marca cae en su familia sin importar mayúsculas`() {
        assertEquals(DeviceFamily.Xiaomi, DeviceFamily.of("Xiaomi"))
        assertEquals(DeviceFamily.Xiaomi, DeviceFamily.of("POCO"))
        assertEquals(DeviceFamily.Samsung, DeviceFamily.of("samsung"))
        assertEquals(DeviceFamily.Huawei, DeviceFamily.of("HONOR"))
        assertEquals(DeviceFamily.Oppo, DeviceFamily.of("OnePlus"))
        assertEquals(DeviceFamily.Vivo, DeviceFamily.of("vivo"))
    }

    @Test
    fun `una marca desconocida usa los pasos generales`() {
        assertEquals(DeviceFamily.Other, DeviceFamily.of("Nothing"))
        assertEquals(DeviceFamily.Other, DeviceFamily.of(""))
    }
}
