# Foco 0.9.11

versionName `0.9.11`, versionCode `26`. Same `signing/foco-debug.keystore`.

## Editar pantallas

A tap on the page cue opens Editar home. A tap on a dot still changes page. A long-press on a dot opens the editor too, including on Personal, where the cue does not repeat the page name.

Ajustes → Editar home still brings the launcher forward and opens the same editor. That return no longer leaves edit mode. `singleTask` runs `onRestart` before `onNewIntent`, so the old check looked at the previous intent, counted a home return, and the next frame cleared `editing`. The return is decided in `onResume`, after the edit extra is current. Opening Uso from Ajustes is unchanged.

The pager and the drag-to-group gesture are not on this control. Uso stays as in 0.9.10.
