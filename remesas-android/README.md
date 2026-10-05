# Remesas — APK nativa (WebView)

Empaqueta la web de [PeterServices](https://github.com/momosad26/PeterServices) en una APK
que corre en su propio WebView: sin Chrome, sin barra de direcciones y sin internet.

- **Compilar:** GitHub → Actions → *Build Remesas APK* → descargar el artifact `remesas-apk`.
  Cada compilación toma la última versión del repo de PeterServices.
- **Actualizar:** instalar la APK nueva encima de la anterior; los datos se conservan
  (misma firma `remesas.keystore`, versión creciente).
- **Pasar datos desde la PWA de Chrome:** en la PWA, *Respaldos → Exportar*; en la APK,
  *Respaldos → Importar* y elegir el `.json`.
- **Local:** `./sync-web.sh && gradle assembleRelease`.
