# Foco 0.9.10

versionName `0.9.10`, versionCode `25`. Same `signing/foco-debug.keystore`.

## Uso

Own home page, after Personal: Reloj, Agenda, Personal, Uso, Comida, Trabajo. Home still lands on Personal. It is not a block inside Personal.

The page reads this profile only. Copy: «Uso del perfil personal». Work-profile time is not queried and not filled in.

From API 28 the windows use `queryAndAggregateUsageStats`. Older releases use daily `queryUsageStats` buckets. Events are not read. Each window is local midnight: today, and today plus the six days before it. The list is foreground time for every named app above zero, capped at 12, not the home whitelist.

Hoy is the large total, with the subtitle Hoy. When the device reports it, Pantalla is today's screen-on time from `queryEventStats` and is omitted when that read is empty. Últimos 7 días sits under it, with seven monochrome bars and a short weekday. A line counts foreground time outside the Personal whitelist. Each row can say fuera and when it was last used. The chips Hoy and 7 días switch that total and the ranking. The list is Más usadas: up to 12 apps, time on the right in Paper dim, a thin bar against the top row. An app icon is used when one loads; otherwise a letter on ink elevated. A missing grant shows «Ver tu uso», the usage-access body, and «Activar uso». An empty granted day says «Todavía no hay datos de hoy»; an empty week says «Sin uso en estos 7 días». A failed read shows «No se pudo leer el uso» for two seconds and does not pretend the total is zero. The stats are read when the page settles and when the user taps Actualizar. They are not polled and they are not stored. Settings has a Uso row that opens this page. Home still lands on Personal. A row opens the app when it has a launcher intent. There is no long-press on the row.
