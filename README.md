# Kofia Trans

[![CI](https://github.com/Sheddy00/kofia-trans/actions/workflows/ci.yml/badge.svg)](https://github.com/Sheddy00/kofia-trans/actions/workflows/ci.yml)

API REST de gestion des courses, chauffeurs et revenus de la coopérative de transport Kofia Trans.
Construite avec Spring Boot 4 (Java 21) et PostgreSQL, accès aux données en JDBC.

## Prérequis

- Java 21
- PostgreSQL (écoutant sur le port **5432**)
- Maven n'est pas nécessaire : le wrapper `./mvnw` est fourni.

## Configuration

L'application lit sa configuration dans les variables d'environnement suivantes :

| Variable            | Rôle                                              | Exemple       |
|---------------------|---------------------------------------------------|---------------|
| `DATABASE_HOST`     | Hôte PostgreSQL                                   | `localhost`   |
| `DATABASE_NAME`     | Nom de la base                                    | `kofia_trans` |
| `DATABASE_USER`     | Utilisateur PostgreSQL                            | `postgres`    |
| `DATABASE_PASSWORD` | Mot de passe PostgreSQL                           | `postgres`    |
| `JWT_SECRET`        | Clé de signature des jetons (**32 caractères minimum**, l'API refuse de démarrer sinon) | `openssl rand -base64 48` |
| `JWT_EXPIRATION`    | Durée de validité d'un jeton, format ISO-8601 (défaut : `PT8H`) | `PT8H` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Compte administrateur créé au démarrage s'il n'existe pas | `admin` / *secret* |
| `DEMO_USERNAME` / `DEMO_PASSWORD`   | Compte de démonstration en lecture seule (optionnel) | `demo` / `demo` |
| `FLYWAY_LOCATIONS` | Emplacements des migrations (défaut : `classpath:db/migration`) ; ajouter `classpath:db/demo` pour les données de démo | `classpath:db/migration,classpath:db/demo` |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées pour le frontend, séparées par des virgules (défaut : `http://localhost:5173`) | `https://kofia-trans-ui.onrender.com` |

> Les comptes sont créés au démarrage à partir de ces variables, avec un mot de passe haché (BCrypt).
> Un compte existant n'est jamais modifié : changer `ADMIN_PASSWORD` après coup n'a pas d'effet.

## Lancement

1. Créer une base vide (les tables sont créées par l'application, voir [Base de données](#base-de-données)) :

   ```bash
   createdb -U postgres kofia_trans
   ```

2. Exporter les variables d'environnement :

   ```bash
   export DATABASE_HOST=localhost
   export DATABASE_NAME=kofia_trans
   export DATABASE_USER=postgres
   export DATABASE_PASSWORD=postgres
   export JWT_SECRET="$(openssl rand -base64 48)"
   export ADMIN_USERNAME=admin
   export ADMIN_PASSWORD='choisir-un-mot-de-passe'
   export DEMO_USERNAME=demo
   export DEMO_PASSWORD=demo
   export FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/demo   # optionnel : données de démonstration
   ```

3. Démarrer l'application :

   ```bash
   ./mvnw spring-boot:run
   ```

   Ou construire puis lancer le jar :

   ```bash
   ./mvnw clean package -Dtest='!KofiaTransApplicationTests'
   java -jar target/kofia-trans-0.0.1-SNAPSHOT.jar
   ```

L'API écoute sur **http://localhost:8081**.

## Base de données

Le schéma est géré par **Flyway** : au démarrage, l'application applique automatiquement les migrations
versionnées de `src/main/resources/db/migration` qui ne l'ont pas encore été. Aucune commande `psql` n'est nécessaire.

| Migration | Contenu |
|-----------|---------|
| `V1__create_fleet_tables.sql` | Tables `driver`, `vehicle`, `trip` |
| `V2__create_app_user.sql`     | Table `app_user` (comptes et mots de passe BCrypt) |
| `db/demo/R__demo_data.sql`    | Données de démonstration (optionnelles, rejouables, n'écrasent rien) |

- Une base créée **avant Flyway** est automatiquement enregistrée à la version 1 : seules les migrations suivantes
  s'appliquent, sans toucher aux données existantes.
- Les données de démonstration ne sont chargées que si `FLYWAY_LOCATIONS` contient `classpath:db/demo`.
- Pour faire évoluer le schéma, ajoutez un nouveau fichier `V3__description.sql` ; ne modifiez jamais une migration déjà appliquée.

## Utilisation

### Authentification

L'API utilise des jetons **JWT** (signés en HS256, valables 8 h par défaut) :

1. `POST /auth/login` avec un identifiant et un mot de passe renvoie un jeton ;
2. chaque requête envoie ce jeton dans l'en-tête `Authorization: Bearer <jeton>`.

| Rôle     | Droits                                   |
|----------|------------------------------------------|
| `ADMIN`  | Lecture et écriture (création, modification, changement de statut) |
| `VIEWER` | Lecture seule (compte de démonstration)  |

Sans jeton valide, l'API répond `401` ; une écriture avec le rôle `VIEWER` répond `403`.

```bash
TOKEN=$(curl -s -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"choisir-un-mot-de-passe"}' \
  http://localhost:8081/auth/login | python3 -c "import json,sys; print(json.load(sys.stdin)['accessToken'])")

curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/trips
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8081/drivers/d-001/revenue?from=2026-04-01&to=2026-04-30"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/auth/me
```

### Routes

| Méthode | Route                              | Description                                    |
|---------|------------------------------------|------------------------------------------------|
| POST    | `/auth/login`                      | Se connecter (public), renvoie un jeton JWT    |
| GET     | `/auth/me`                         | Utilisateur associé au jeton                   |
| GET     | `/trips`                           | Lister les courses (filtres `driverId`, `from`, `to`) |
| PUT     | `/trips/{tripId}`                  | Créer ou remplacer une course                  |
| PUT     | `/trips/{tripId}/status`           | Modifier le statut (`COMPLETED`, `CANCELLED`)  |
| GET     | `/drivers/{driverId}/revenue`      | Revenu et frais de coopérative sur une période |
| GET     | `/drivers/{driverId}/statistics`   | Statistiques d'un chauffeur                    |
| GET     | `/statistics/top-earning-driver`   | Chauffeur au plus gros revenu sur une période  |

Les dates sont au format `AAAA-MM-JJ`. La spécification complète est dans [`docs/openapi.yaml`](docs/openapi.yaml)
(lisible par exemple sur [editor.swagger.io](https://editor.swagger.io)).

## Tests

```bash
# Tests unitaires et de contrôleurs (sans base de données)
./mvnw test -Dtest='!KofiaTransApplicationTests'

# Tous les tests, y compris le chargement du contexte (nécessite PostgreSQL et les variables ci-dessus)
./mvnw test
```

La CI GitHub Actions (`.github/workflows/ci.yml`) lance les tests sans `KofiaTransApplicationTests` et valide la spécification OpenAPI
à chaque push et pull request sur `main`.

## Déploiement (Render)

À chaque push sur `main`, une fois les tests, la spec OpenAPI et l'image Docker validés, la CI déclenche
un déploiement sur [Render](https://render.com) via un *Deploy Hook*.

Mise en place (une seule fois) :

1. Sur Render, créer une base **PostgreSQL** (les tables seront créées par Flyway au premier démarrage ;
   ajouter `FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/demo` pour y charger les données de démonstration).
2. Créer un **Web Service** à partir de ce dépôt, runtime **Docker** (le `Dockerfile` est à la racine).
3. Dans *Environment* du Web Service, définir `DATABASE_HOST` (hostname interne de la base),
   `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`
   (et éventuellement `DEMO_USERNAME`, `DEMO_PASSWORD`, `CORS_ALLOWED_ORIGINS`). Render fournit `PORT` automatiquement.
4. Dans *Settings*, mettre **Auto-Deploy** sur `No` (c'est la CI qui déploie, après les tests),
   puis copier l'URL du **Deploy Hook**.
5. Sur GitHub : *Settings → Secrets and variables → Actions*, ajouter le secret `RENDER_DEPLOY_HOOK_URL`
   avec cette URL.

Tant que le secret n'est pas défini, l'étape de déploiement est ignorée (avec un avertissement) sans faire échouer la CI.

Pour lancer l'image en local :

```bash
docker build -t kofia-trans .
docker run -p 8081:8081 --add-host=host.docker.internal:host-gateway -e DATABASE_HOST=host.docker.internal -e DATABASE_NAME=kofia_trans \
  -e DATABASE_USER=postgres -e DATABASE_PASSWORD=postgres -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e ADMIN_USERNAME=admin -e ADMIN_PASSWORD='choisir-un-mot-de-passe' kofia-trans
```

## Structure

```
src/main/java/hei/school/kofiatrans/
├── config/       # Déclaration des beans
├── controller/   # Contrôleurs REST, DTOs, gestion des erreurs
├── db/           # Connexion JDBC et requêtes
├── exception/    # Exceptions client / serveur
├── model/        # Modèles métier et enums
├── security/     # Spring Security, jetons JWT, CORS
└── service/      # Calculs de revenus, authentification
src/main/resources/
└── db/
    ├── migration/  # Migrations Flyway versionnées (schéma)
    └── demo/       # Données de démonstration optionnelles
```
