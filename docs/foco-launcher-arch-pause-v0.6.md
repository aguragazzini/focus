# Foco — arch check pausas (Agus) v0.6

**De:** Andrés · Arquitectura  
**Pedido:** FORGE — 2026-09-25  
**Hard constraint:** NO DevicePolicyManager · NO admin · NO pausar work profile del sistema.

---

## 1) Pausar área Trabajo — **GO-with-limits**

| Pieza | Veredicto |
|-------|-----------|
| Flag local `workSectionPaused` (DataStore) | **GO** |
| UI: ocultar sección Trabajo | **GO** |
| NLS: mientras paused, **cancel** notifs con UserHandle work (deja de “pasar”) | **GO** (override explícito del default work-PASS) |
| Al reanudar: flag off → UI vuelve + NLS work-PASS otra vez | **GO** |
| `UserManager.requestQuietModeEnabled` / toggle quiet del sistema | **NO-GO** |
| DPM / Profile Owner / disable work apps | **NO-GO** |
| Detectar quiet del sistema (`isQuietModeEnabled`) solo lectura para S3 UI | **GO** (ya v0.5) — **no escribir** |

**Default sin pausa:** work sigue **PASS** (arch NLS §0 intacto).  
**Excepciones al cancelar work en pausa:** CALL / ALARM / NAVIGATION / MediaStyle / packages sistema — mismos spare que personal.  
**OEM Moto:** no tocar quiet; no “sincronizar” pausa Foco con quiet del perfil (doble semántica + frágil).  
**Edge:** A1 del grid Trabajo no aplica mientras sección oculta; al reanudar, refresh LauncherApps. No force-stop work.

---

## 2) Pausa general de notificaciones — **GO-with-limits**

| Pieza | Veredicto |
|-------|-----------|
| Flag local `notificationsPaused` (DataStore), sobrevive reboot | **GO** |
| Implementación = rama NLS (cancel in-scope), **no** DND sistema | **GO** |
| `NotificationManager.setInterruptionFilter` / Notification Policy Access | **NO-GO** (pelea con DND del user) |
| Mientras on: cancel personal (salvo spare CALL/ALARM/NAV/Media/sistema) | **GO** |
| Work bajo pausa general: cancel work **solo si** `!workSectionPaused` ya no… simplificar: pausa general también cancela work (si workSectionPaused, ya cancelaba) | **GO** — ambos flags independientes; NLS `suppress = workPaused \|\| notifPaused` para work; personal solo `notifPaused` (o allowlist policy si !notifPaused) |
| Listener conectado + flags off → policy normal | **GO** |
| Copy: “Pausa de Foco ≠ No molestar del teléfono” | **GO** (honesto) |

**Stack con DND user:** OK coexistir (ambos silencian por caminos distintos). Nunca mutar interruption filter.  
**Reboot:** leer flags de DataStore en `onListenerConnected`; sin WorkManager obligatorio.

---

## 3) Drag-to-group — **GO** (UI; arch mínimo)

| Pieza | Veredicto |
|-------|-----------|
| Groups en DataStore junto whitelist personal (v0.6) | **GO** |
| Drag reorder / agrupar **solo personal** | **GO** |
| Agrupar / ocultar apps **work** vía groups | **NO-GO** (rompe A1 / LauncherApps contract) |
| Persistir `groupId` + `order` en entradas personal | **GO** |
| Multi-user: groups scoped al user personal | **GO** |

Camila/Nico dueños de UX; arch no bloquea si personal-only + A1 work intacto.

---

## Resumen GO/NO-GO

1. Pausa Trabajo (flag+UI+NLS gate) — **GO** · quiet/DPM sistema — **NO-GO**  
2. Pausa general notifs (flag+NLS) — **GO** · DND API — **NO-GO**  
3. Drag-to-group personal — **GO** · groups que filtren work — **NO-GO**
