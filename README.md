# Free Noise Generator

Aplicacion Android nativa para generar ruido blanco, rosa y marron.

## Estado actual

- Kotlin + Jetpack Compose.
- Reproduccion en segundo plano con `ForegroundService`.
- Generador procedural con `AudioTrack`, sin archivos de audio externos.
- Interfaz oscura con un unico boton para reproducir ruido marron.
- Notificacion persistente con acciones para pausar o parar.

## Como abrirlo

1. Abre la carpeta del proyecto en Android Studio.
2. Deja que Android Studio sincronice Gradle.
3. Ejecuta la app en un emulador o dispositivo Android.

La terminal de este entorno no tiene `java` ni `gradle` en PATH, asi que la compilacion local queda pendiente hasta tener JDK/Android Studio disponible desde consola.
