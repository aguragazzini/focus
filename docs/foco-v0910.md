# Foco 0.9.10

versionName `0.9.10`, versionCode `25`. Same `signing/foco-debug.keystore`.

## Uso

Own home page, after Personal: Reloj, Agenda, Personal, Uso, Comida, Trabajo. Home still lands on Personal. It is not a block inside Personal.

The page reads this profile only. Copy: «Uso del perfil personal». Work-profile time is not queried and not filled in.

From API 28 the windows use `queryAndAggregateUsageStats`. Older releases use daily `queryUsageStats` buckets. Events are not read. Each window is local midnight: today, and today plus the six days before it. The list is foreground time for every named app above zero, capped at 12, not the home whitelist.

Hoy / 7 días switches the total, the order, and the time on the right. The stats are read when the page settles and when the user taps Actualizar. They are not polled and they are not stored. A missing grant shows «Ver tu uso» and opens usage-access settings. That state is not shown as zero minutes. If the grant is still missing after the return, the page mentions restricted settings for a sideload. A row opens the app when it has a launcher intent.
