# Mis Tutorías

App Android (WebView, sin internet) para llevar el control de alumnos, sus cursos y las actividades de cada curso.

- Alumnos, cursos (abiertos, por hacer, completados) y actividades: todo se puede crear, renombrar, reordenar y borrar.
- Estado al lado de cada actividad y sumatoria por alumno y por curso.
- Pestaña **Pendientes**: todo lo que falta por hacer, revisar, humanizar o entregar, de todos los alumnos, filtrable por estado.
- Nota y **fecha de entrega** opcionales en cada actividad; fecha límite opcional por curso.
- Pestaña **Plan** (sin conexión): reparte lo pendiente por días según tu capacidad diaria, dice si vas a buen ritmo, justo o atrasado, y cuánto necesitas hacer por día.
- **Aviso diario** a la hora que elijas con lo vencido, lo que vence hoy y lo que toca según el plan (Ajustes ⚙).
- Aviso cuando un alumno llega a 10 actividades (el número se cambia en Ajustes ⚙).
- **Reporte Word**: genera un `.docx` con cada curso y sus actividades enumeradas.
- Respaldo: Ajustes ⚙ → Guardar / Restaurar respaldo. Los datos viven solo en el teléfono.

## Compilar

GitHub → Actions → **Build Mis Tutorías APK** → descargar el artefacto `mis-tutorias-apk`.
Toda la pantalla está en `app/src/main/assets/www/index.html`.
