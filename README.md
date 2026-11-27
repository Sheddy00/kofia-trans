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
| `API_KEY`           | Clé attendue dans l'en-tête `x-api-key`           | `ma-cle`      |

> Si `API_KEY` n'est pas définie, **toutes** les requêtes sont refusées avec `403`.

## Lancement

1. Créer la base puis l'initialiser (tables + données de démonstration) :

   ```bash
   createdb -U postgres kofia_trans
   psql -U postgres -d kofia_trans -f src/main/resources/schema.sql
   psql -U postgres -d kofia_trans -f src/main/resources/data.sql
   ```

   > ⚠️ `schema.sql` commence par supprimer les tables existantes : ne le relancez que pour repartir d'une base vide.

2. Exporter les variables d'environnement :

   ```bash
   export DATABASE_HOST=localhost
   export DATABASE_NAME=kofia_trans
   export DATABASE_USER=postgres
   export DATABASE_PASSWORD=postgres
   export API_KEY=ma-cle
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

> L'application n'exécute pas les scripts SQL au démarrage (`spring.sql.init.mode=never`) :
> les données saisies via l'API sont conservées entre deux redémarrages.

## Utilisation

Toutes les requêtes doivent envoyer l'en-tête `x-api-key` :

```bash
curl -H "x-api-key: ma-cle" http://localhost:8081/trips
curl -H "x-api-key: ma-cle" "http://localhost:8081/drivers/d-001/revenue?from=2026-04-01&to=2026-04-30"
curl -H "x-api-key: ma-cle" "http://localhost:8081/statistics/top-earning-driver?from=2026-04-01&to=2026-04-30"
```

| Méthode | Route                              | Description                                    |
|---------|------------------------------------|------------------------------------------------|
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

## Structure

```
src/main/java/hei/school/kofiatrans/
├── config/       # Déclaration des beans
├── controller/   # Contrôleurs REST, DTOs, gestion des erreurs
├── db/           # Connexion JDBC et requêtes
├── exception/    # Exceptions client / serveur
├── model/        # Modèles métier et enums
├── security/     # Filtre de clé API
└── service/      # Calculs de revenus et statistiques
src/main/resources/
├── schema.sql    # Création des tables
└── data.sql      # Données de démonstration
```
