# SendGPSPotrobus - GPS Tracker for PotroBus

Este es un cliente de Android diseñado para rastrear la ubicación en tiempo real de las unidades de transporte "PotroBus" y enviar los datos a una API de Flask. La aplicación está optimizada para funcionar en segundo plano, incluso cuando el dispositivo está en reposo o la pantalla está apagada.

## Características

- **Rastreo en Segundo Plano:** Utiliza un `Foreground Service` para mantener el proceso vivo y evitar que el sistema operativo lo detenga.
- **Alta Precisión:** Implementa `FusedLocationProviderClient` de Google Play Services para obtener coordenadas exactas.
- **Comunicación Segura:** Envío de datos mediante `OkHttp` con soporte para autenticación JWT (Bearer Token).
- **Persistencia de Sesión:** Almacena el ID de la unidad y el token de acceso localmente mediante `SharedPreferences`.
- **Notificación Persistente:** Muestra el estado del rastreo directamente en la barra de notificaciones.

## Tecnologías y Dependencias

- **Lenguaje:** Kotlin
- **Mínimo SDK:** 24 (Android 7.0)
- **Servicios de Ubicación:** `com.google.android.gms:play-services-location:21.0.1`
- **Networking:** `com.squareup.okhttp3:okhttp:4.12.0`
- **UI:** Material Components & ConstraintLayout

## Requisitos y Permisos

Para que el rastreo funcione correctamente en versiones modernas de Android (10+), la aplicación solicita y requiere los siguientes permisos:

- `ACCESS_FINE_LOCATION`: Ubicación precisa por GPS.
- `ACCESS_BACKGROUND_LOCATION`: Permite obtener la ubicación cuando la app no está en uso (debe seleccionarse "Permitir siempre" en los ajustes).
- `FOREGROUND_SERVICE`: Necesario para ejecutar el servicio de rastreo.
- `FOREGROUND_SERVICE_LOCATION`: Declaración específica para servicios de ubicación en Android 14+.
- `INTERNET`: Para enviar los datos al servidor.

## Configuración

### 1. Servidor (Flask API)
Asegúrate de tener un endpoint que reciba peticiones POST en `/api/gps/position`. El formato esperado del JSON es:

```json
{
    "id_unidad": 123,
    "lat": 27.486,
    "lng": -109.940,
    "timestamp": "14:30:05"
}
```

### 2. Android (Constants)
Modifica la URL base en el archivo de constantes del proyecto (`mx.itson.sendgpspotrobus.utils.Constants`) para que apunte a la IP de tu servidor:

```kotlin
object Constants {
    const val BASE_URL = "http://TU_DIRECCION_IP:5000"
}
```
### 3. Network Exceptions
Posiblemente sea necesario configurar el archivo network exceptions con la configuración IP del servidor.

## Optimización de Batería (Importante)

Debido a las políticas agresivas de ahorro de energía en Android, se recomienda:
1. Ir a **Ajustes del dispositivo** > **Aplicaciones**.
2. Buscar **SendGPSPotrobus**.
3. En la sección **Batería**, seleccionar **"Sin restricciones"** o **"No optimizar"**.

## Licencia
Este proyecto es de uso interno para el sistema PotroBus.
