# PC & Body-mind para Android

App Android de [PC & Body-mind](https://gcalabro12.github.io/PC-Body-mind/) (by Práctica Integral). Cuenta pasos **siempre**: con la pantalla apagada, con la app cerrada y tras reiniciar el teléfono.

- Usa el podómetro del propio teléfono (gasta muy poca batería) y muestra los pasos de hoy en una notificación fija.
- Dice en voz alta «Llevas 500 pasos», los bloques de 2000 y la meta del día, también con el móvil bloqueado.
- La pantalla es la misma web (descansos de la vista, bloques de tiempo, pasos, música y pantalla del ordenador), que recibe los pasos del podómetro.

## Descargar

👉 **[PC-Body-mind.apk (última versión)](https://github.com/GCalabro12/PC-Body-mind-Android/releases/latest/download/PC-Body-mind.apk)**

Ábrelo en el móvil y acepta «instalar apps de origen desconocido» la primera vez. Para actualizar, descarga e instala encima: se conservan los datos.

## Cómo se compila

Cada cambio en `main` lo compila GitHub Actions (`.github/workflows/compilar.yml`) y publica el APK firmado en *Releases*. El número de versión está en `VERSION` (la misma numeración que la web). La clave de firma está en los secretos del repositorio (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`).
