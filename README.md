# Task Management API

API REST de gestion de projets et de tâches en équipe : espaces de travail, projets, tâches
(priorités, échéances, assignation), commentaires, notifications par e-mail et permissions par rôle.

**Stack :** Java 17 · Spring Boot 4 · Spring Security + JWT · Spring Data JPA · PostgreSQL · Java Mail · JUnit 5 + Mockito · Docker

## Démarrage rapide

```bash
# 1. Base de données + serveur mail de test (Mailpit)
docker compose up -d

# 2. Lancer l'API (http://localhost:8080)
./mvnw spring-boot:run

# 3. Tests unitaires
./mvnw test
```

- Interface Mailpit (e-mails reçus) : http://localhost:8025
- Tout lancer dans Docker (API comprise) : `JWT_SECRET=$(openssl rand -base64 32) docker compose --profile full up --build`
  (`JWT_SECRET` est obligatoire : sans lui, le conteneur `api` refuse de démarrer)
- Requêtes prêtes à l'emploi : `docs/requests.http` (extension VS Code « REST Client »)

## Configuration (variables d'environnement)

| Variable | Défaut (dev) | Rôle |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL du `docker-compose.yml` | Base de données |
| `JWT_SECRET` | clé de développement | **À changer en production** : `openssl rand -base64 32` |
| `JWT_EXPIRATION` | `86400000` (24 h) | Durée de vie du token JWT, en millisecondes |
| `MAIL_HOST`, `MAIL_PORT` | `localhost`, `1025` | Serveur SMTP |
| `REMINDERS_CRON` | `0 0 8 * * *` | Heure des rappels d'échéance (essai rapide : `0 * * * * *` = chaque minute) |

En production, activer le profil `prod` (`SPRING_PROFILES_ACTIVE=prod`) : `JWT_SECRET` et `DB_PASSWORD` deviennent
obligatoires (plus aucune valeur par défaut) et le schéma n'est plus créé par Hibernate (`ddl-auto=validate` : à créer
par vos propres migrations).

## Permissions

| Action | OWNER | ADMIN | MEMBER |
|---|:-:|:-:|:-:|
| Voir workspace, projets, tâches, commentaires | ✅ | ✅ | ✅ |
| Créer des tâches et des commentaires | ✅ | ✅ | ✅ |
| Modifier une tâche / changer son statut | ✅ | ✅ | seulement si créateur ou assigné |
| Supprimer une tâche | ✅ | ✅ | seulement si créateur |
| Supprimer un commentaire | ✅ | ✅ | seulement le sien |
| Créer / modifier / supprimer un projet | ✅ | ✅ | ❌ |
| Inviter des membres, modifier le workspace | ✅ | ✅ | ❌ |
| Retirer un membre | ✅ | MEMBER uniquement | ❌ |
| Changer les rôles, supprimer le workspace | ✅ | ❌ | ❌ |

## Endpoints

Toutes les routes sauf `/api/auth/**` demandent l'en-tête `Authorization: Bearer <token>`.

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/auth/register` · `/api/auth/login` | Inscription / connexion → token JWT |
| POST / GET | `/api/workspaces` | Créer / lister mes workspaces |
| GET / PUT / DELETE | `/api/workspaces/{id}` | Détail / modifier / supprimer |
| POST / GET | `/api/workspaces/{id}/members` | Inviter (par e-mail + rôle) / lister |
| PATCH / DELETE | `/api/workspaces/{id}/members/{memberId}` | Changer le rôle / retirer |
| POST / GET | `/api/workspaces/{id}/projects` | Créer / lister les projets |
| GET / PUT / DELETE | `/api/projects/{id}` | Détail / modifier / supprimer |
| POST / GET | `/api/projects/{id}/tasks` | Créer / lister (filtres `?status=&priority=&assigneeId=`) |
| GET | `/api/tasks/mine` | Mes tâches assignées, tous workspaces confondus |
| GET / PUT / DELETE | `/api/tasks/{id}` | Détail / modifier / supprimer |
| PATCH | `/api/tasks/{id}/status` | `TODO`, `IN_PROGRESS`, `REVIEW`, `DONE` |
| POST / GET | `/api/tasks/{id}/comments` | Commenter / lister |
| DELETE | `/api/comments/{id}` | Supprimer un commentaire |

Priorités : `LOW`, `MEDIUM` (défaut), `HIGH`, `URGENT`.
Erreurs au format `{ "timestamp": "...", "status": 403, "message": "..." }`.

Les tentatives sur `/api/auth/login` sont limitées à 10 par adresse et par quart d'heure, celles sur
`/api/auth/register` à 5 par heure (au-delà : `429`). Les compteurs sont en mémoire : redémarrer l'application
les remet à zéro.

## Notifications e-mail

- Tâche assignée (ou réassignée) → e-mail à la personne assignée.
- Nouveau commentaire → e-mail à la personne assignée et au créateur de la tâche (sauf l'auteur).
- Rappel d'échéance → tâche non terminée à rendre aujourd'hui ou demain, une seule fois (tâche planifiée).

Les envois sont asynchrones : une panne SMTP ne fait jamais échouer une requête.

## Structure

```
config/      sécurité, erreurs JSON, activation @Async / @Scheduled
controller/  routes REST (Auth, Workspace, Project, Task, Comment)
service/     logique métier + AccessService (permissions centralisées)
scheduler/   rappels d'échéance
repository/  Spring Data JPA
entity/ dto/ security/ (JWT)
```
