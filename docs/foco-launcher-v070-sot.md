# Foco 0.7 — source of truth

Attached notes:

- Camila, UX: `docs/foco-launcher-ux-v0.7-drag-grupos.md`
- Andrés, arch pauses: `docs/foco-launcher-arch-pause-v0.6.md`

Product override on Camila §3.1: **Pausar avisos is silence.** While `notificationsPaused` is on, the listener cancels in-scope notifications (Andrés §2). It does not turn the filter off. Shipped copy is `Pausar avisos`, `Reanudar avisos`, and the chip `Avisos · En pausa`. Do not ship `Pausar filtro de avisos`, `Avisos · Sin filtro`, or “deja de filtrar”.

Trabajo folders only organize. Every work launchable stays on the list when the section is visible (Andrés A1). Pausar Trabajo hides the section and cancels work notifications in Foco. It does not write system quiet mode.
