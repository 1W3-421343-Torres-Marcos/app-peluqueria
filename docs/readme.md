# Metanoia — App de gestión para peluquería

**Metanoia Hair Salon** es una app Android de gestión integral para una peluquería, usada por el dueño (y eventualmente sus empleados) en su propio dispositivo.

## Propósito
Centralizar la operación diaria del salón — turnos, clientes, cobros, stock y reportes — en una app rápida, simple y **100 % offline**: sin servidores, sin cuentas en la nube, sin publicación en tiendas. Se instala por APK.

## Características principales
- Agenda diaria/semanal por profesional, con validación de solapamientos, bloqueos y turnos recurrentes.
- Clientes con historial, notas, ausencias y **ficha de color** reutilizable.
- Catálogo de servicios, profesionales con horario y comisión.
- Recordatorios y saludos de cumpleaños por WhatsApp (enlace wa.me, sin API).
- Lista de espera con sugerencias al liberarse un turno.
- Caja con cobros, ventas, gastos, cierre diario y auditoría.
- Stock con alertas de mínimo, comisiones automáticas y reportes PDF/CSV.
- **Backup automático** a una carpeta elegida por el dueño (Drive sincronizado, SD, pendrive) y restauración validada.

## Restricciones clave
- Android, Java 17, XML + ViewBinding, MVVM, Room, WorkManager.
- Sin permiso de Internet. Datos sensibles solo salen por acción explícita del dueño.
- Español (Argentina), pesos argentinos, `dd/MM/yyyy`, 24 hs.
- Modo claro y oscuro, estética monocromática alineada al logo.

## Estructura del repositorio
```
peluqueriaAntigravity/
├── docs/
│   ├── agents.md          Reglas globales, stack, arquitectura (leer primero)
│   ├── readme.md          Este resumen
│   ├── plan.md            Hoja de ruta y tablero de progreso (F1…F12)
│   ├── design.md          Guía de estilo visual y componentes
│   ├── changelog.md       Historial de versiones (una entrada por fase)
│   ├── instalacion.md     (F12) Instalación del APK y resguardo de la keystore
│   └── modules/           Una carpeta por módulo con su skills.md
│       ├── agenda/          ├── caja/
│       ├── clientes/        ├── comisiones/
│       ├── servicios/       ├── stock/
│       ├── profesionales/   ├── reportes/
│       ├── ficha-color/     ├── backup/
│       ├── recordatorios/   └── configuracion/
│       └── lista-espera/
├── assets/
│   ├── branding/          metanoia.png (solo referencia) · logo.svg (versión limpia, pendiente)
│   └── fonts/             Montserrat (Bold 700, SemiBold 600) e Inter (Regular 400, Medium 500, Light 300) TTF estáticas · licenses/ (SIL OFL)
├── domain/                (F1) Módulo Java puro: modelos, reglas, casos de uso
├── app/                   (F1) Módulo Android: data (Room, backup) + ui
└── build.gradle · settings.gradle · gradle/libs.versions.toml · .gitignore   (F1)
```

Control de versiones con Git: rama `fase/F<n>` por fase, tags `fase-F<n>` y `v<versionName>` (ver `agents.md` §11).

## Cómo trabajar
1. Leer `docs/agents.md`.
2. Abrir `docs/plan.md` y tomar el siguiente paso pendiente.
3. Consultar el `skills.md` del módulo y `docs/design.md` si hay UI.
4. Al terminar cada paso, actualizar `plan.md`.

## Compilar (a partir de F1)
```
./gradlew assembleDebug      # APK de desarrollo
./gradlew test               # tests unitarios
```
