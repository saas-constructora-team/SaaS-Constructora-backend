# Variables de Entorno (`.env`)

Este documento especifica todas las variables de entorno requeridas para ejecutar el proyecto en desarrollo local y en entornos de despliegue.

---

## Configuración Rápida Local

1. Crea tu archivo de entorno local a partir de la plantilla:
   ```bash
   cp .env.example .env

2. Configura las variables de Entorno

Crea un archivo llamado `.env` en la raíz de tu proyecto y copia la siguiente plantilla completando los valores necesarios:

```env
RESEND_API_KEY=re_H6SyppM2_52cGt1VkgvNdp1M1TeDZkwAo
RESEND_FROM=saasconstructora.correounivalle.edu.co
APP_BASE_URL=http://localhost:4200
DB_URL=jdbc:postgresql://localhost:5432/saas_constructora
DB_USERNAME=saas
DB_PASSWORD=saas
GOOGLE_CLIENT_ID=40468898476-lqrag5pjpe4bnb1tu98hpmskttudb8nb.apps.googleusercontent.com
# Correo por SMTP (Gmail). Completa MAIL_PASSWORD con la contraseña de aplicación.
EMAIL_PROVIDER=smtp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=santicarrillo314@gmail.com
MAIL_PASSWORD=cpwusjqbbzsecjit
MAIL_FROM=santicarrillo314@gmail.com
```
