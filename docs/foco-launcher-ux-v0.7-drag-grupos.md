# Foco Launcher — UX v0.7 (grupos drag + pausas)

**De:** Camila · UX  
**Para:** FORGE · Nico · Andrés · Agus  
**Fecha:** 2026-09-25  
**Prioridad:** grupos plug-and-play por **drag** (Agus rechazó long-press+menú como camino principal).  
**SoT:** este doc para v0.7. Hereda: whitelist Personal, Trabajo = todas las launchables (A1), local-first, ES-AR, sin wellness/gamificación.  
**Arch:** pausa Trabajo = filtro Foco (ocultar sección + silenciar notifs Trabajo en NLS). **No** DPM / quiet del perfil sistema — Andrés valida. Si arch no puede filtrar notifs Trabajo, UX igual oculta la sección; copy de notifs se ajusta a “solo se ocultan las apps”.

---

## 0. Qué NO toca este SoT

- Santo del día / glances extra → charla aparte. Máx 1–2 glances fijos ya definidos (reloj+fecha; línea glance local). Cero AppWidgetHost de terceros.
- Bio, drawer de todas las personales, clima red.

---

## 1. Grupos por drag (P0 ship)

### 1.1 Modelo mental
- **Personal:** tiles de whitelist; se pueden agrupar entre sí.  
- **Trabajo:** tiles work; se pueden agrupar entre sí.  
- **Prohibido:** mezclar Personal ↔ Trabajo en el mismo grupo. Al soltar cross-sección → snap back + hint corto.

### 1.2 Gestos

| Gesto | Resultado |
|-------|-----------|
| Drag tile A **sobre** tile B (≥60% overlap, dwell 280–350 ms) | Crear grupo `{A,B}` o meter A en grupo de B |
| Drag tile **sobre carpeta** abierta/cerrada | Meter en ese grupo |
| Drag tile **fuera** del grupo (al grid vacío de su sección) | Sacar del grupo; si queda 1 app → disolver grupo |
| Drag grupo completo | Mover el stack en el grid (opcional P0; si duele, P1) |
| Long-press tile | Respaldo: sheet `Agrupar con…` / `Sacar del grupo` / `Quitar de la lista` (solo Personal) |

**No** menú “Crear grupo” como camino feliz.

### 1.3 Feedback (affordances)

| Momento | UI |
|---------|-----|
| Lift | Tile escala ~1.06, elevación sutil; hueco en grid |
| Hover válido sobre otra app/grupo | Target ring Paper 2dp + preview “apilar” (segunda silueta detrás) |
| Hover inválido (otra sección) | Ring muted + shake 1 frame; no preview apilar |
| Drop crear grupo | Animación apilar 200 ms → carpeta con **2** glifos/iniciales |
| Carpeta cerrada | Cuadrante 2×2 de iniciales **o** tipografía del nombre del grupo (ver §4) |
| Abrir carpeta | Overlay/sheet de la sección: fondo ink scrim; grid interno; tap fuera cierra |
| Empty tras sacar última | Grupo desaparece sin diálogo |

### 1.4 Nombre de grupo
- Default: `Grupo` / `Grupo 2`… o concatenar 2 primeras apps truncadas.  
- Rename: long-press carpeta → `Renombrar` (1 campo, Enter guarda).  
- Copy vacío rename: `Nombre del grupo`.

### 1.5 Vacíos / bordes
- 1 sola app en sección: drag sobre sí mismo no crea grupo.  
- Durante drag, banners/pausas no roban el drop target.  
- Trabajo en pausa (§2): no se draguean tiles Trabajo (sección ausente).

### 1.6 Criterios aceptación grupos
- [ ] G1: Drag A→B en Personal crea grupo; long-press no es obligatorio.  
- [ ] G2: Drag fuera saca; 1 restante disuelve.  
- [ ] G3: Cross Personal/Trabajo no mezcla.  
- [ ] G4: Dwell + overlap evitan grupos accidentales al scrollear.  
- [ ] G5: Long-press sheet cubre el mismo poder como respaldo.

---

## 2. Pausar área Trabajo (P0)

### 2.1 Qué hace
| ON (pausado) | OFF (activo) |
|--------------|--------------|
| Sección Trabajo **no se muestra** en home | Sección Trabajo visible (A1) |
| Notifs Trabajo **no pasan** el filtro Foco (si Andrés OK) | Notifs Trabajo PASS como hoy |
| Personal + reloj intactos | Igual |

**No es** pausar el perfil Android del sistema. Copy nunca diga “pausar perfil de trabajo de Android”.

### 2.2 Control
- Lugar: **Ajustes → Trabajo** (fila) + atajo en ⋮: `Pausar Trabajo` / `Reanudar Trabajo`.  
- Home (opcional P0): chip discreto bajo el reloj **solo si** hay work profile: `Trabajo · En pausa` tap → reanudar. Si activo, **no** chip (cero ruido).  
- Confirmación al pausar: **ninguna** (reversible en 1 tap). Al reanudar: ninguna.

### 2.3 Copy

| Clave | Texto |
|-------|--------|
| `work_pause` | Pausar Trabajo |
| `work_resume` | Reanudar Trabajo |
| `work_pause_sub` | Oculta las apps de Trabajo en el inicio. Los avisos de Trabajo no suenan en Foco. |
| `work_paused_chip` | Trabajo · En pausa |
| `work_pause_arch_fallback` | Oculta las apps de Trabajo en el inicio. *(usar si notifs no se pueden filtrar)* |

Estado en Ajustes: toggle o dos botones claros; label de estado `En pausa` / `Visible en el inicio` muted.

---

## 3. Pausa general de notificaciones (filtro NLS) (P0)

### 3.1 Qué es
Mute del **filtro NLS de Foco** (dejan de silenciarse / o se silencia todo lo que Foco controla — alinear con Nico: interpretación UX = **“Foco no filtra: llegan todas las del personal que el sistema mande”** vs **“Foco silencia todo lo no-allowlist”**).

**Producto Agus (usar esta):** un interruptor `Pausar filtro de avisos` =
- **Pausa ON:** el filtro NLS de Foco **no actúa** (equivale a filtro Off): avisos personales según sistema; no es DND.  
- **Pausa OFF:** filtro NLS activo (comportamiento Foco normal).

Diferenciar de DND del sistema en subtítulo.

### 3.2 Diferencia visual vs pausa Trabajo

| | Trabajo | Filtro avisos |
|--|---------|---------------|
| Icono | maletín / “Trabajo” texto | campana tachada / “Avisos” |
| Ubicación Ajustes | bloque **Trabajo** | bloque **Avisos** |
| Chip home | `Trabajo · En pausa` | solo si filtro pausado: `Avisos · Sin filtro` (muted) — **no** mismo estilo que Trabajo |
| Color | mismo Paper; **no** semáforo | igual — distinguir por **label**, no por rojo/verde |

Nunca un solo botón “Pausa” ambiguo.

### 3.3 Copy

| Clave | Texto |
|-------|--------|
| `nls_pause` | Pausar filtro de avisos |
| `nls_resume` | Reanudar filtro de avisos |
| `nls_pause_sub` | Foco deja de filtrar avisos del perfil personal. No es el No molestar del teléfono. |
| `nls_paused_chip` | Avisos · Sin filtro |

---

## 4. Sin logos de apps (apuesta)

### 4.1 Recomendación
**Default OFF (opt-in).** Modo `Nombres sin ícono` en Ajustes → Apariencia.

**Por qué no default-on:** en launcher real el costo de reconocimiento día 1 es alto (sobre todo Trabajo con muchas apps parecidas). Opt-in deja la apuesta para quien ya internalizó la lista; default-on pelea con “usar el teléfono” los primeros días y Agus va a medir rechazo, no menos estímulo.

**Default-on solo si:** Agus acepta fricción consciente los primeros 2–3 días y hay fallback rápido (toggle en ⋮). Si ship único sin dial: **opt-in**.

### 4.2 Spec del modo
- Tile = label 2 líneas max, tipografía Paper, sin bitmap del launcher icon.  
- Opcional: inicial en círculo 28dp PaperDim (glifo neutro), **igual para todas** — no meta-ícono de la app.  
- Carpetas: 2×2 iniciales o nombre del grupo.  
- Trabajo y Personal mismos reglas.  
- Accesibilidad: `contentDescription` sigue siendo el label de la app.

### 4.3 Copy
| Clave | Texto |
|-------|--------|
| `look_no_icons` | Mostrar solo nombres |
| `look_no_icons_sub` | Oculta los íconos de las apps. Cuesta reconocerlas al principio; baja el estímulo visual. |

---

## 5. Wire home v0.7 (con pausas)

```
[ ⋮ ]
  21:47
  vie 25 sep
  [chip Trabajo · En pausa]     ← solo si pausa Trabajo
  [chip Avisos · Sin filtro]  ← solo si pausa NLS; estilo distinto/label
Personal
○ ○ 📁 ○        ← tiles / grupos drag
─ ─ ─
Trabajo         ← ausente si pausa Trabajo
○ ○ ○ …
```

---

## 6. Opinión corta (fuera de impl)

Glances: quedarse en reloj+fecha (+ batería/alarma si ya está). “Santo del día” y widgets de terceros diluyen el minimalismo; charlarlo aparte, no meter en v0.7.

---

## 7. Orden de build sugerido
1. Drag crear/sacar grupo (Personal, luego Trabajo) + anti-mix  
2. Long-press respaldo  
3. Pausa Trabajo (UI + hide sección; notifs según Andrés)  
4. Pausa filtro avisos (bloque Ajustes distinto)  
5. Opt-in solo nombres  

**Éxito:** Agus agrupa en un gesto sin menú; pausa Trabajo y deja el inicio en Personal quieto; entiende que “Sin filtro” ≠ No molestar; sin íconos es una elección, no un castigo de primer uso.
