# Foco 0.9.8

versionName `0.9.8`, versionCode `23`. Same `signing/foco-debug.keystore`.

## Two states

The pill toggles. **Avisos** is off: outline bell, inkElevated fill, 1dp line, PaperDim. The listener does not cancel. **Foco** is on: struck bell, Paper at 11%, 1.5dp Paper border, Paper label. The listener cancels every notification it can see. A press scales to 0.98 and ticks a light haptic.

TalkBack names the control `Avisos`, says `desactivado` or `activado`, and hints `tocá para activar Foco` or `tocá para dejar pasar avisos`. Without a listener the pill says **Activar avisos**. The next time it connects, Foco turns on. If turning Foco on fails, a muted snackbar says **No se pudo activar Foco** for 2 seconds and the pill stays on Avisos.

The pill sits on Personal, under the glances and above the grid. A multi-page home does not also put it on Reloj. A stored in-scope pause becomes cancel-all. A chosen off stays off. Pausar Trabajo stays its own control.

## One page title

Personal, Trabajo, and Apps already print their name, so the pager cue keeps the dots and does not repeat the word. Reloj, Comida, and a default Agenda still use the cue as their only title.
