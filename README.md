# Sparkgram

Inoffizieller Telegram-Client für Android mit zusätzlichen, einzeln aktivierbaren Funktionen.
Basiert auf dem offiziellen Quellcode von [Telegram für Android](https://github.com/DrKLO/Telegram)
(Version 13.1.1) und steht wie dieser unter der GNU GPL v2 oder später (siehe `LICENSE`).

Sparkgram ist **kein offizielles Telegram-Produkt**.

## Branches
- `entwicklung` – laufende Entwicklung, jeder Push baut eine Test-APK (Release `build-entwicklung`)
- `main` – geprüfte, funktionierende Stände (Release `build-main`)

## Bauen
Der Workflow `.github/workflows/build.yml` baut die APK automatisch. Benötigte Repository-Secrets:

| Secret | Inhalt |
|---|---|
| `TELEGRAM_API_ID` | api_id von my.telegram.org |
| `TELEGRAM_API_HASH` | api_hash von my.telegram.org |
| `SPARK_KEYSTORE_PASSWORD` | Passwort für `.github/signing/sparkgram.keystore.enc` |

Die ursprüngliche Telegram-Anleitung liegt in `README.telegram.md`.
