# PROMPTS.md · Fase 2 con IA

Mejoras de CampusGPS en la rama `mejora-ia`, trabajadas con **Claude** (IA autorizada por el docente). Cada fila resume una instrucción que le di a la IA durante la conversación, lo que propuso, lo que ajusté y mi criterio técnico.

| Prompt que escribí | Qué respondió Claude | Qué cambié o descarté | Por qué |
|---|---|---|---|
| «Implementa la mejora Punto azul nativo: activa `isMyLocationEnabled` y el botón de mi ubicación del mapa, solo si el permiso fue concedido, sin romper MVVM» | Agregar un parámetro `permisoConcedido` a `MapaUbicacion` y pasar a `GoogleMap` un `MapProperties(isMyLocationEnabled = permisoConcedido)` y un `MapUiSettings(myLocationButtonEnabled = permisoConcedido)`, guardados con `remember(permisoConcedido)`. El permiso se revisa en `PantallaMapa` con `ContextCompat.checkSelfPermission` en una función `tienePermiso()` que también usa el botón | Descarté guardar el estado del permiso en el ViewModel. Reutilicé la misma verificación del Paso 2 en una sola función en lugar de duplicarla | Revisar un permiso necesita un `Context`, y el ViewModel no debe recibir `Context`. Es lógica de la View, igual que pedir el permiso. Si se activa `isMyLocationEnabled` sin permiso, el mapa lanza `SecurityException`, por eso va condicionado |
