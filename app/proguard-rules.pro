# Reglas de R8 para Velora.
# Las librerías AndroidX y Compose traen sus propias reglas de consumidor.

# Sin logs de depuración en release (REGLAS §8).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
