# Om Launcher

Launcher personal para Android (Samsung Galaxy A06), hecho en Kotlin con Jetpack Compose.

## Qué hace

- **Inicio:** saludo grande, hora · fecha · batería, próxima actividad del calendario, tiempo en pantalla del día, favoritas en texto y una fila de cuatro apps abajo.
- **Paisaje según la hora:** el fondo se dibuja en la app y cambia de colores entre amanecer, mañana, mediodía, tarde y noche. Se puede apagar para usar tu propio fondo.
- **Agenda (deslizar a la izquierda):** tira de la semana, actividades de hoy y los próximos días, y espacio para widgets de otras apps.
- **Todas las apps (deslizar hacia arriba):** lista alfabética con buscador. Mantén presionada una app para agregarla a favoritas, renombrarla u ocultarla.
- **Ajustes (mantener presionado en Inicio):** saludo, color del texto, favoritas, fila de abajo, apps ocultas y permisos.
- **Nota de voz:** mantén presionado el micrófono, habla y suelta. El texto se abre en el app de Claude.
- **Deslizar hacia abajo:** abre las notificaciones.

## Cómo instalarlo

1. Cada cambio en `main` compila el APK automáticamente en GitHub Actions.
2. Abre la sección **Releases** del repositorio desde el teléfono y descarga el último `OmLauncher-1.N.apk`.
3. Ábrelo y permite instalar apps desde el navegador cuando Android lo pida.
4. Presiona el botón de inicio y elige **Om Launcher** como launcher predeterminado.

Las versiones nuevas se instalan encima de la anterior sin perder tus ajustes, porque todas se firman con la misma llave (`app/omlauncher.jks`).

## Permisos

- **Calendario:** para la próxima actividad y la Agenda.
- **Acceso a datos de uso:** para el tiempo en pantalla. Se activa en Ajustes de Android › Acceso a datos de uso › Om Launcher.
- **Micrófono:** para las notas de voz.

## Créditos

Tipografía [Outfit](https://github.com/Outfitio/Outfit-Fonts), con licencia SIL Open Font License (ver `FONT-LICENSE-OFL.txt`).
