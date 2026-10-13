# Pasos para Android

App Android del [Contador de Pasos](https://gcalabro12.github.io/Contador-Pasos/) que **cuenta siempre**: con la pantalla apagada, con la app cerrada y tras reiniciar el teléfono.

- Usa el podómetro del propio teléfono (gasta muy poca batería).
- Muestra los pasos de hoy en una notificación fija.
- Dice en voz alta «Llevas 500 pasos», los bloques de 2000 y la meta del día, también con el móvil bloqueado.
- La pantalla es la misma web (diseño, música, avisos y pantalla del ordenador), que recibe los pasos del podómetro.

## Descargar

👉 **[Pasos.apk (última versión)](https://github.com/GCalabro12/Contador-Pasos-Android/releases/latest/download/Pasos.apk)**

Ábrelo en el móvil y acepta «instalar apps de origen desconocido» la primera vez. Para actualizar, descarga e instala encima: se conservan los pasos.

## Cómo se compila

Cada cambio en `main` lo compila GitHub Actions (`.github/workflows/compilar.yml`) y publica el APK firmado en *Releases*. La clave de firma está en los secretos del repositorio (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`).
