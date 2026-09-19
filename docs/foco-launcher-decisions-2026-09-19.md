# Foco Launcher — decisiones de producto (2026-09-19)

## Identidad
- Nombre provisorio: **Foco Launcher**
- Package: `com.foco.launcher`
- Plataforma: Android nativo only (Kotlin + Jetpack Compose)
- Distribución v1: sideload APK
- Monetización v1: ninguna (herramienta personal)
- Backend: ninguno (local-first / offline)

## Principio
> Construir el teléfono que quiero usar, no el teléfono que intenta que lo use.

## Kill personal
Si después de 30 días Agus no quiere seguir usándolo como launcher principal → archivar, no comercializar ni “buscar PMF”.

## Whitelist
- Cero apps visibles por defecto
- Selección manual
- Bio **activada por defecto** en cada app nueva a la whitelist; excepción explícita por app (ej. Teléfono)
- Setup **antes** de setear como default: obligar a elegir apps mínimas (sugerir Teléfono, Ajustes, Cámara, Mensajes — usuario confirma)
- **Ajustes siempre con vía segura** para no quedar afuera

## MVP 0.1 (2 semanas)
Semana 1: APK launcher default + whitelist + home + apertura rápida  
Semana 2: biometría + settings + estabilidad uso diario  
Después: notificaciones, performance, hardening, intents indirectos

## Fuera de v0.1
Notificaciones avanzadas, bloqueo intents indirectos, recomendaciones, monetización, Play Store, Accessibility (salvo demostración de necesidad), modo niños, cloud, gamificación, MELI vertical

## Código
De cero. Filosofía de Foco/Freno reutilizable; no arquitectura vieja.

## Restricciones fábrica
Sin marketplace/empleados/consultoría; lejos MELI; gasto USD 0–50 (ideal 0); pocas hs/semana
