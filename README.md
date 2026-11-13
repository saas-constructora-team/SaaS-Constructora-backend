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

### Invitaciones de trabajadores

El flujo de invitación (`invite-and-claim`) permite vincular a un trabajador sin
que el administrador conozca su contraseña:

1. `POST /api/personnel/invite` crea/reutiliza la identidad y envía un enlace.
2. La persona define su contraseña o activa su membresía en
   `POST /api/auth/invitations/accept`.
3. `GET /api/auth/invitations/{token}` permite previsualizar la invitación antes
   de aceptar.

Variables opcionales:

```env
# Interruptor del flujo de invitaciones (true por defecto en desarrollo).
INVITATION_ENABLED=true
# Base del enlace enviado por correo (por defecto APP_BASE_URL).
APP_INVITATION_BASE_URL=http://localhost:4200
# Vigencia del enlace en horas (1 a 720).
INVITATION_TTL_HOURS=72
```

Si el correo ya pertenece a una cuenta existente no se crea otro usuario: se
crea una nueva membresía y, al aceptar, se obtiene una sesión del tenant que
invita. Cada enlace es de un solo uso y reenviar la invitación invalida el
anterior.
