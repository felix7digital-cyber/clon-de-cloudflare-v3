# Clon de Cloudflare (Android) · V3.1

Cliente Android nativo (Kotlin + Jetpack Compose) para la API de Cloudflare.

## Estado
- Login real contra Cloudflare
- Token cifrado con fallback seguro
- Pages: listar/crear/eliminar + bindings D1/R2/KV
- **D1: lista, detalle, editor SQL, tablas, bindings, eliminar**
- Bottom bar, FAB, recientes/fijadas, toasts
- Crash handler

## Build
GitHub Actions con workflow_dispatch. Artifact: app-release-v3
