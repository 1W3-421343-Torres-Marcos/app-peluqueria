# design.md — Guía de estilo visual Metanoia

> Minimalista, monocromática y coherente con el logo. Toda pantalla usa estos tokens vía tema (`?attr/…`, `@dimen/…`, `@style/…`). Prohibidos valores sueltos.

## 1. Principios
- Blanco y negro primero; el acento es la excepción.
- Bordes finos en lugar de sombras (estética del doble borde del logo).
- Mucho espacio en blanco, jerarquía por tipografía y peso.
- El estado nunca se comunica solo con color: siempre ícono + texto.

## 2. Paleta

### Neutros
| Token | Hex | Uso |
|---|---|---|
| `ink` | `#111111` | Texto principal (claro), fondo (oscuro) |
| `white` | `#FFFFFF` | Fondo (claro), texto principal (oscuro) |
| `gray50` | `#F5F5F5` | Fondo de secciones / inputs (claro) |
| `gray100` | `#EBEBEB` | Divisores suaves |
| `gray200` | `#D6D6D6` | Bordes de tarjetas e inputs (claro) |
| `gray400` | `#9E9E9E` | Texto deshabilitado, íconos inactivos |
| `gray600` | `#616161` | Texto secundario (claro) |
| `gray800` | `#2B2B2B` | Bordes / superficies elevadas (oscuro) |
| `gray900` | `#1C1C1C` | Superficie de tarjetas (oscuro) |

### Acento (único): **Bordó**
| Token | Claro | Oscuro | Uso exclusivo |
|---|---|---|---|
| `accent` | `#8C2F39` | `#D9727D` | Botón primario, estados (confirmado, cancelado), alertas (stock bajo, backup vencido) |
| `onAccent` | `#FFFFFF` | `#111111` | Texto sobre acento |
| `accentContainer` | `#F3E3E5` | `#3A1E22` | Fondo de chips/banners de alerta |

Contraste verificado ≥ 4.5:1 para texto sobre fondo en ambos modos.

### Mapeo de tema (Material 3, `Theme.Metanoia` DayNight)
| Atributo | Claro | Oscuro |
|---|---|---|
| `colorPrimary` | `ink` | `white` |
| `colorOnPrimary` | `white` | `ink` |
| `colorSecondary` | `accent` | `accent` |
| `android:colorBackground` / `colorSurface` | `white` | `ink` |
| `colorSurfaceVariant` | `gray50` | `gray900` |
| `colorOutline` | `gray200` | `gray800` |
| `colorOnSurface` | `ink` | `white` |
| `colorOnSurfaceVariant` | `gray600` | `#BDBDBD` |
| `colorError` | `accent` | `accent` |

Tema seleccionable: Sistema (por defecto) / Claro / Oscuro.

## 3. Tipografía
Fuentes empaquetadas en `res/font/` (licencia SIL OFL):
- **Montserrat** (Bold 700, SemiBold 600) → títulos, botones, logo placeholder.
- **Inter** (Regular 400, Medium 500, Light 300) → texto, montos, formularios.

| Estilo (`TextAppearance.Metanoia.*`) | Fuente | Tamaño | Uso |
|---|---|---|---|
| `Display` | Montserrat Bold | 32 sp | Totales de caja, splash |
| `Headline` | Montserrat Bold | 24 sp | Título de pantalla |
| `Title` | Montserrat SemiBold | 20 sp | Encabezado de sección |
| `Subtitle` | Montserrat SemiBold | 16 sp | Nombre en tarjetas |
| `Body` | Inter Regular | 16 sp | Texto general |
| `BodySmall` | Inter Regular | 14 sp | Texto secundario |
| `Label` | Inter Medium | 14 sp | Chips, labels de inputs |
| `Caption` | Inter Regular | 12 sp | Metadatos (mínimo permitido) |
| `Button` | Montserrat SemiBold | 16 sp, letterSpacing 0.02 | Botones |
| `Money` | Inter Medium, `fontFeatureSettings="tnum"` | según contexto | Montos y horas (cifras tabulares) |

Mayúsculas solo en el logo y en etiquetas cortas de sección (`Label` con letterSpacing 0.08).

## 4. Espaciado y forma
- Escala base 4 dp: `space_xs 4` · `space_s 8` · `space_m 16` · `space_l 24` · `space_xl 32` · `space_xxl 48`.
- Margen lateral de pantalla: 16 dp. Separación entre tarjetas: 8 dp.
- Radios: `radius_s 8` (botones, inputs) · `radius_m 12` (tarjetas, bottom sheets) · `radius_pill 999` (chips).
- Bordes: 1 dp `colorOutline`. Elevación 0 salvo FAB y bottom sheets (máx. 3 dp).
- Objetivos táctiles ≥ 48 dp. Botón primario 56 dp de alto.

## 5. Iconografía
- Material Symbols **Outlined** (peso 300–400) como VectorDrawables locales, 24 dp, color `colorOnSurface`.
- Motivo tijera (`ic_scissors`, trazo de línea): solo en splash, ítem Agenda de la navegación y estados vacíos. No usar como decoración en otras partes.

## 6. Logo
- **Referencia visual:** `assets/branding/metanoia.png` (foto de vidriera). **Nunca** se usa en la app.
- Descripción del original: círculo con doble borde (exterior grueso, interior fino), una tijera arriba y otra abajo cruzando el borde, banda central entre dos líneas horizontales con "METANOIA" (sans-serif negrita) y "Hair Salon" (fino) debajo. Negro sobre blanco.
- **Recurso reemplazable:** `assets/branding/logo.svg` (maestro) → `res/drawable/logo_metanoia.xml` (positivo) y `logo_metanoia_inverse.xml` (blanco, para modo oscuro).
- **Placeholder provisional** (`ui/common/LogoView`): "METANOIA" Montserrat Bold, letterSpacing 0.12 + "Hair Salon" Inter Light 0.35× del tamaño, centrado. Sin círculo ni tijeras dibujadas (no inventar logo). `LogoView` muestra el vector si existe; si no, el placeholder.
- Usos: splash (ancho 160 dp, centrado), encabezado de Inicio (alto 32 dp), cabecera de PDF y recibos.
- Reglas: área de respeto ≥ 25 % del ancho alrededor; tamaño mínimo 24 dp de alto; solo negro sobre claro o blanco sobre oscuro; prohibido deformar, rotar, recolorear con el acento, agregar sombras o efectos.

## 7. Componentes reutilizables (`ui/common/`)

| Componente | Especificación |
|---|---|
| **Botón primario** `Widget.Metanoia.Button.Primary` | Fondo `accent`, texto `onAccent`, 56 dp, radio 8, ancho completo en formularios. Uno solo por pantalla. |
| **Botón secundario** `.Button.Secondary` | Borde 1 dp `colorPrimary`, texto `colorPrimary`, fondo transparente, 48 dp. |
| **Botón texto** `.Button.Text` | Sin borde, texto `colorPrimary`. Acciones terciarias. |
| **FAB extendido** | Fondo `colorPrimary`, ícono + texto ("Nuevo turno"), esquina inferior derecha. |
| **Tarjeta de turno** `AppointmentCardView` | Borde 1 dp, radio 12, padding 16. Izquierda: hora inicio–fin (`Money`, 16 sp). Centro: cliente (`Subtitle`), servicios (`BodySmall`, 1 línea, elipsis), profesional (`Caption`). Derecha: `StatusChip`. Barra vertical de 4 dp a la izquierda según estado (ver tabla de variantes). Toque → detalle; deslizar → acciones rápidas. |
| **Chip de estado** `StatusChip` | Píldora, 28 dp alto, `Label`, ícono 16 dp + texto. Variantes abajo. |
| **Banner de alerta** `AlertBanner` | Fondo `accentContainer`, ícono + texto `accent`, acción opcional. Para backup vencido y stock bajo. |
| **Ítem de lista** `ListRowView` | 64 dp mín., título + subtítulo + acción derecha, divisor `gray100`. |
| **Buscador** `SearchField` | Input con ícono lupa, radio 8, fondo `colorSurfaceVariant`, búsqueda con debounce 250 ms. |
| **Monto** `MoneyTextView` | Formatea `long` centavos a `$ 12.345,50`, estilo `Money`. |
| **Estado vacío** `EmptyStateView` | Ícono tijera de línea 48 dp `gray400`, texto `Body`, botón secundario opcional. |
| **Selector de horario** `TimeSlotGrid` | Grilla de bloques (según duración mínima), libre = borde, ocupado = `gray50` con texto, seleccionado = `colorPrimary`. |
| **Bottom sheet de acciones** | Radio superior 12, filas de 56 dp con ícono. Para acciones de turno/cliente. |
| **Diálogo de confirmación** | Título `Title`, texto `Body`, acción destructiva con botón texto `accent`. |

### Variantes de `StatusChip` y barra lateral de `AppointmentCardView`
Cada estado se distingue por **ícono + texto**, nunca solo por color.

| Estado | Chip | Ícono | Barra lateral (4 dp) |
|---|---|---|---|
| Pendiente | Borde `gray400`, texto `colorOnSurfaceVariant` | reloj | `gray400` |
| Confirmado | Relleno `accent`, texto `onAccent` | check | `accent` |
| Completado (pendiente de cobro) | Borde `colorPrimary`, texto `colorPrimary` | check | `colorPrimary` con patrón a rayas (saldo pendiente) |
| Pagado | Relleno `colorPrimary`, texto `colorOnPrimary` | pago (`$`) | `colorPrimary` sólida |
| Cancelado | Borde `accent`, texto `accent`, tachado | cruz | `accent` al 40 % de opacidad |
| Ausente | Relleno `gray100`/`gray800`, texto `colorOnSurface` | persona tachada | `gray200`/`gray800` |
| Stock bajo | Relleno `accentContainer`, texto `accent` | alerta | — |

La tarjeta de un turno Completado muestra además el saldo adeudado (`MoneyTextView`, `Caption`) bajo el chip.

## 8. Navegación y pantallas
- Splash (`core-splashscreen`): fondo `colorBackground`, logo centrado, sin animaciones largas (< 1 s).
- Barra superior: logo (Inicio) o título (`Headline`) a la izquierda, acciones con íconos a la derecha.
- Bottom navigation (4 destinos): **Agenda · Clientes · Caja · Más** (Más agrupa servicios, profesionales, stock, reportes, backup, configuración).
- Formularios: un campo por fila, labels arriba, botón primario fijo abajo.

## 9. Salidas externas
- **PDF (reportes A4 / recibos):** solo blanco y negro; cabecera con logo + "Metanoia Hair Salon", fecha `dd/MM/yyyy HH:mm`; tablas con bordes `gray200`; pie con número de página.
- **CSV:** UTF-8 con BOM, separador `;`, decimales con coma, fechas `dd/MM/yyyy`.
- **WhatsApp:** texto plano, sin emojis obligatorios, firma final "— Metanoia Hair Salon".
