# Foco 0.7 — source of truth

Attached notes:

- Camila, UX: `docs/foco-launcher-ux-v0.7-drag-grupos.md`
- Andrés, arch pauses: `docs/foco-launcher-arch-pause-v0.6.md`

Product override on Camila §3.1: **Pausar avisos is silence.** While `notificationsPaused` is on, the listener cancels in-scope notifications (Andrés §2). It does not turn the filter off. Shipped copy is `Pausar avisos`, `Reanudar avisos`, and the chip `Avisos · En pausa`. Do not ship `Pausar filtro de avisos`, `Avisos · Sin filtro`, or “deja de filtrar”.

Trabajo folders only organize. Every work launchable stays on the list when the section is visible (Andrés A1). Pausar Trabajo hides the section and cancels work notifications in Foco. It does not write system quiet mode.

The 1962 Missal, Vetus Ordo, rubrics of 1960, stays bundled in `app/src/main/assets/santoral_1962.json`. From 0.8.0 the home is four swipe pages; that layout, the Novus line, and where the pauses live are in `docs/foco-home-pages-v080.md`.

App icons stay on by default. `Mostrar solo nombres` stays opt-in and off until the user turns it on. AppWidgetHost stays out.
