# FoodYou + Foto IA (Yui via Tailscale)

## Que hace
Fork de FoodYou (GPL-3.0) con boton **Foto IA** en "Quick Add".
- Camara / Galeria -> POST http://100.66.224.38:8765/analyze-food (Tailscale) -> Yui (hermes --image) -> JSON {alimento,kcal,proteinas,grasas,carbos} -> rellena formulario.

## Servidor
`voice_server.py` :8765
- GET /health, GET /analyze-food (info)
- POST /analyze-food multipart image=file (jpg/png/webp, 10MB) -> usa `hermes chat --image foto -Q` como VLM.

## Archivos modificados
- app/src/commonMain/kotlin/com/maksimowiczm/foodyou/photo/PhotoAnalyzer.kt
- app/src/commonMain/kotlin/com/maksimowiczm/foodyou/photo/KtorPhotoAnalyzer.kt (fallback URLs: 100.66.224.38 -> 192.168.10.214 -> 10.0.0.1)
- app/src/commonMain/kotlin/com/maksimowiczm/foodyou/photo/PhotoModule.kt
- app/src/commonMain/kotlin/com/maksimowiczm/foodyou/photo/PhotoAnalyzeButton.kt (expect)
- app/src/androidMain/kotlin/com/maksimowiczm/foodyou/photo/PhotoAnalyzeButton.kt (actual, camara+galeria)
- app/src/iosMain/kotlin/com/maksimowiczm/foodyou/photo/PhotoAnalyzeButton.kt (stub)
- app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/di/InitKoin.kt (+photoModule)
- app/src/commonMain/kotlin/com/maksimowiczm/foodyou/app/ui/food/diary/quickadd/QuickAddScreen.kt (boton)
- app/src/androidMain/AndroidManifest.xml (+CAMERA + networkSecurityConfig)
- app/src/androidMain/res/xml/network_security_config.xml

## Compilar
./gradlew :app:assembleDebug
APK: app/build/outputs/apk/debug/app-debug.apk

## Probar
curl -F "image=@foto.jpg" http://100.66.224.38:8765/analyze-food
curl -F "image=@foto.jpg" http://127.0.0.1:8765/analyze-food
