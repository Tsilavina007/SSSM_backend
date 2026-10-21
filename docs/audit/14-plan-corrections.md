# 14 — Plan de correction

**Objectif :** proposer un ordre de correction réaliste, avec dépendances, lots de travail, migrations et critères de sortie. Ce plan n'est pas exécuté : aucune correction n'a été appliquée pendant l'audit.

**Principe directeur :** ne pas ajouter d'écran sur un contrat faux. La stabilité de la tenancy, des transactions et du stock précède le frontend.

---

## 1. Décisions à prendre avant le code

Certaines corrections ne sont pas des tâches techniques : elles engagent le produit. Elles doivent être tranchées explicitement.

| ID | Décision | Options | Impact |
|---|---|---|---|
| D-01 | Modèle de stock | Journal calculé / stock déclaratif assumé / hybride | Détermine les tables, le dashboard et les écrans |
| D-02 | Multi-entreprise | Mono-entreprise définitive / multi-entreprise réelle | Détermine l'effort de tenancy |
| D-03 | Inscription | Fermée / invitation / publique avec validation | Détermine le flux d'identité |
| D-04 | Suppression | Physique / archivage logique / les deux | Détermine les contraintes et l'UI |
| D-05 | QR Code | Immuable / versionné / modifiable avec historique | Détermine l'impression et le mobile |
| D-06 | Affectation | Un seul module / deux modules spécialisés | Détermine le contrat |
| D-07 | API | Versionner `/api/v1` / garder `/api` | Détermine la compatibilité |
| D-08 | Portée admin | Admin plateforme / admin entreprise | Détermine le RBAC |

**Sortie :** un court document de décisions signé. Sans D-01 et D-02, les lots 3 et 4 ne peuvent pas être planifiés correctement.

---

## 2. Vue d'ensemble des lots

| Lot | Thème | Priorité | Dépend de | Taille indicative |
|---|---|---|---|---|
| L0 | Décisions et cadrage | P0 | — | S |
| L1 | Identité, tenancy, RBAC | P0 | L0 | XL |
| L2 | Accès aux données et transactions | P0 | L1 | L |
| L3 | Modèle de stock | P0 | L0, L2 | XL |
| L4 | Affectations et mouvements | P0 | L3 | L |
| L5 | QR, scan, étiquettes | P1 | L1, L3, L4 | L |
| L6 | Comptages et réconciliation | P1 | L3 | L |
| L7 | Contrat HTTP, erreurs, pagination | P1 | L1, L3 | M |
| L8 | Tests, CI, performance | P1 | L1-L7 | M |
| L9 | Préparation frontend | P2 | L7, L8 | M |

Les tailles sont relatives : S = quelques jours, M = quelques semaines, L = plusieurs semaines, XL = chantier transversal. Elles ne remplacent pas une estimation d'équipe.

---

## 3. Lot 0 — Décisions et cadrage

### Tâches

1. Trancher D-01 à D-08.
2. Définir la définition de « prêt pour la production ».
3. Identifier les données existantes à préserver.
4. Définir la politique de sauvegarde et de rollback.
5. Geler les évolutions de contrat pendant la migration.

### Livrables

- document de décisions ;
- inventaire des données de démonstration et de production ;
- fenêtre de migration ;
- matrice de compatibilité des anciennes routes.

### Critère de sortie

- D-01 et D-02 ont une réponse explicite ;
- aucune correction de stock n'est lancée sur un modèle non choisi.

---

## 4. Lot 1 — Identité, tenancy et RBAC

### 4.1. Modèle de tenancy

**Cible :**

- `app_user.company_id` obligatoire pour les comptes métier ;
- les comptes plateforme ont un indicateur explicite et une permission dédiée ;
- `company_id` est ajouté aux tables d'inventaire qui ne l'ont pas : `stock_movement`, `item_assignment`, `inventory_count_scan` (et vérification des autres) ;
- les contraintes composites `(id, company_id)` sont ajoutées sur les tables référencées ;
- les FK composites remplacent les FK simples lorsque la cohérence de tenant doit être garantie ;
- les requêtes filtrent par `company_id` issu du jeton ;
- option de défense en profondeur : PostgreSQL RLS avec un paramètre de session, si l'équipe accepte la complexité.

**Migration :**

1. ajouter les colonnes nullable ;
2. backfiller depuis `item`, `warehouse`, `employee`, `security_post` ;
3. vérifier les orphelins ;
4. ajouter les index ;
5. ajouter les contraintes `NOT NULL` et composites ;
6. basculer les lectures ;
7. retirer les anciennes requêtes globales.

**Risque :** les données historiques sans entreprise doivent être traitées avant le `NOT NULL`. Aucune suppression automatique.

### 4.2. Authentification

| Tâche | Détail |
|---|---|
| Inscription | invitation ou validation ; plus de compte sans entreprise |
| Login | 401 pour identifiants invalides, 403 pour compte inactif |
| Refresh | refresh token distinct, rotation, révocation, rôles rechargés |
| Logout | révocation du refresh et du `jti` d'accès |
| JWT | ajouter `company_id`, `jti`, `iss`, `aud` ; permissions ou rechargement |
| Mots de passe | politique, changement, reset, pas de log |
| Limitation | rate limiting login/register/refresh |
| Audit | journal des connexions et révocations |

### 4.3. RBAC

- Définir les permissions dans un catalogue unique.
- Les mapper sur les routes et les méthodes.
- `ADMIN` d'entreprise ne voit que son entreprise.
- `ADMIN` plateforme explicite, avec audit.
- Les permissions en base deviennent la source de vérité, pas seulement le rôle.
- Tests : `USER`, `INVENTORY`, `ADMIN`, sans rôle, deux entreprises.

### 4.4. Durcissement

- CORS par environnement, origines explicites.
- HTTPS obligatoire en production.
- En-têtes de sécurité (HSTS, CSP selon le frontend).
- Masquage des SQLSTATE en production.
- Journal d'audit sans secrets.

### 4.5. Critères de sortie

- un utilisateur de A ne peut lire, créer, modifier ou supprimer aucune donnée de B ;
- un utilisateur sans entreprise ne peut pas accéder à l'inventaire ;
- un `USER` ne peut pas appeler une route `ADMIN` ;
- un rôle retiré ne peut pas être rafraîchi ;
- un refresh rejoué est rejeté ;
- les tests de tenancy et RBAC passent sur deux entreprises.

---

## 5. Lot 2 — Accès aux données et transactions

### 5.1. Remplacer `DriverManager`

- Utiliser le `DataSource` Spring et son pool (HikariCP), ou en configurer un explicitement.
- Injecter ce `DataSource` dans les DAO ou passer par `JdbcTemplate`.
- Supprimer les ouvertures de connexion manuelles dans les DAO.
- Faire lire aux DAO le `DataSource` Spring (mêmes variables `DB_URL`/`DB_USER`/`DB_PASSWORD`) et supprimer la double configuration `DriverManager`.
- Vérifier TLS, timeouts, taille de pool, validation des connexions.

### 5.2. Transactions

- Délimiter les transactions au niveau service.
- Vérifier que chaque DAO utilise la connexion transactionnelle.
- Rendre atomiques :
  - mouvement + article ;
  - affectation + article ;
  - retour + article ;
  - opération + lignes + mouvements ;
  - comptage + ajustements.
- Ajouter des tests d'échec après la première écriture.

### 5.3. Concurrence

- Ajouter `version` ou `updated_at` avec `If-Match`.
- Utiliser `SELECT ... FOR UPDATE` sur les articles lors des transitions.
- Traduire les conflits en 409.
- Ajouter `Idempotency-Key` sur les créations sensibles.

### 5.4. Corrections de defaults

- `ItemTypeDAOImpl.createItemType` : ne pas insérer `NULL` explicite pour `unit`, `quantity_total`, `minimum_stock`.
- `EmployeeDAOImpl.create` : même correction pour `status`.
- Revoir tous les DAO qui insèrent des colonnes nullables avec `DEFAULT`.
- Ajouter des tests d'omission.

### 5.5. Critères de sortie

- aucune connexion `DriverManager` dans les DAO ;
- un échec forcé après la première écriture ne laisse aucune ligne partielle ;
- la configuration de production est celle réellement utilisée ;
- les doublons et FK produisent 409/422, pas 500.

---

## 6. Lot 3 — Modèle de stock

### 6.1. Si le stock est calculé

**Tables / colonnes cibles :**

- `stock_movement` : `quantity NUMERIC(12,2)`, `source_warehouse_id`, `destination_warehouse_id`, `operation_id`, `performed_by`, `idempotency_key`, `reversed_by`, `reversed_at`, `occurred_at`.
- `stock_balance` : `(company_id, warehouse_id, item_type_id, quantity, version)` avec clé unique.
- `inventory_operation_item` : `(operation_id, item_id, quantity, source_warehouse_id, destination_warehouse_id)`.
- `inventory_count_scan` : `expected_quantity`, `counted_quantity`, `difference`, `expected_warehouse_id`, `counted_warehouse_id`.
- `item.last_inventory_at` : alimenté à la clôture.
- `warehouse_stock_config` : consommé par les alertes.
- `item_type.quantity_total` : déprécié ou supprimé après migration.

**Règles :**

- quantité > 0 ;
- pas de stock négatif ;
- mouvement validé immuable ;
- correction par mouvement inverse ;
- solde mis à jour dans la même transaction ;
- toute opération multi-lignes est atomique ;
- pas de `quantity_total` modifiable à la main.

### 6.2. Si le stock reste déclaratif

- Le documenter comme tel.
- Supprimer les promesses de dashboard « stock réel ».
- Renommer les champs pour éviter la confusion (`declared_quantity`).
- Ne pas construire d'écran de stock par entrepôt.
- Conserver les mouvements comme journal d'événements, avec quantité et acteur.

### 6.3. Migration

1. choisir la source de vérité ;
2. créer les nouvelles tables/colonnes ;
3. backfiller depuis les données existantes ;
4. double-écriture temporaire si nécessaire ;
5. vérifier les soldes par rapport aux articles ;
6. basculer les lectures ;
7. retirer l'ancien champ ;
8. conserver une période de réconciliation.

### 6.4. Critères de sortie

- une réponse unique et testée à « stock disponible par entrepôt et type » ;
- aucun mouvement sans quantité, source/destination ou acteur ;
- aucun stock négatif possible ;
- un mouvement ne peut plus être modifié ou supprimé ;
- un comptage clôturé met à jour `last_inventory_at` et les soldes ;
- le dashboard et les alertes utilisent la source de vérité.

---

## 7. Lot 4 — Affectations et mouvements

### 7.1. Un seul contrat

- Choisir `/api/v1/item-assignments`.
- Déprécier `/api/inventory/assignments`.
- Supprimer le JSON libre.
- Conserver une période de compatibilité en lecture si nécessaire.

### 7.2. Machine à états

```text
AVAILABLE --assign--> ASSIGNED --return--> AVAILABLE
AVAILABLE --OUT--> ASSIGNED (avec affectation obligatoire)
ASSIGNED --RETURN--> AVAILABLE (clôt l'affectation)
ASSIGNED --DESTRUCTION--> RETIRED (interdit si actif)
```

Règles :

- `ASSIGNED` implique exactement une affectation active ;
- `AVAILABLE` implique aucune affectation active ;
- `RETIRED` est terminal ;
- `DAMAGED`, `LOST`, `REPAIR` ont des transitions documentées ;
- toute transition est atomique et auditée.

### 7.3. Retour

- `returned_at` obligatoire ou positionné par le serveur ;
- `returned_by` déduit du jeton ;
- entrepôt restauré ;
- idempotent ;
- 409 si déjà retourné.

### 7.4. Suppression

- plus de `DELETE` d'affectation active ;
- `void` avec motif, acteur, date ;
- conservation de l'historique ;
- correction de l'état de l'article dans la même transaction.

### 7.5. Critères de sortie

- aucun article `ASSIGNED` sans affectation active ;
- aucun article `AVAILABLE` avec affectation active ;
- aucun retour sans date ;
- aucun état modifié par un simple `PATCH` de notes ;
- les tests de concurrence passent.

---

## 8. Lot 5 — QR, scan et étiquettes

### 8.1. Code stable

- Table `item_qr_code` : `code`, `item_id`, `company_id`, `version`, `created_at`, `revoked_at`.
- Un seul code actif par article.
- `item.qr_code` conservé comme cache de compatibilité.
- Rotation avec période de grâce.
- Plus de modification libre par `PATCH` ; rotation dédiée.

### 8.2. Scan enrichi

- résolution tenant-scoped ;
- réponse avec type, catégorie, entrepôt, affectation, détenteur, actions autorisées ;
- 404 si inconnu, pas de liste globale ;
- limitation de débit ;
- journal `qr_scan` : utilisateur, entreprise, appareil, action, résultat, horodatage.

### 8.3. Étiquettes

- PDF et planche ;
- texte lisible ;
- taille configurable ;
- cache ;
- génération asynchrone pour les lots ;
- contrôle des droits.

### 8.4. Critères de sortie

- deux entreprises peuvent avoir le même code sans ambiguïté ;
- un code ne change plus silencieusement ;
- un scan renvoie tout ce qu'il faut pour agir ;
- chaque scan est traçable ;
- une planche de 100 étiquettes est générable sans 100 appels manuels.

---

## 9. Lot 6 — Comptages et réconciliation

### Tâches

- Ajouter `expected_quantity` et `counted_quantity`.
- Figer l'attendu à l'ouverture.
- Calculer l'écart côté serveur.
- Refuser un article hors périmètre.
- Clôturer avec `completed_at`, `closed_by`.
- Appliquer les ajustements de manière atomique.
- Mettre à jour `item.last_inventory_at`.
- Ajouter `GET /inventory-counts/{id}` et les lignes paginées.
- Rendre la clôture idempotente.

### Critères de sortie

- un comptage vide ne peut pas être clôturé comme complet ;
- un écart est calculé, pas fourni par le client ;
- la clôture met à jour les soldes et `last_inventory_at` ;
- un comptage inconnu renvoie 404 ;
- un comptage d'une autre entreprise est inaccessible.

---

## 10. Lot 7 — Contrat HTTP

### Tâches

1. `/api/v1`.
2. `PageResponse<T>` avec total.
3. `application/problem+json`.
4. Codes stables.
5. 201 + `Location`.
6. 204 pour les suppressions/retours sans corps.
7. 409 conflits, 422 règles, 404 inconnus.
8. Dates ISO-8601 UTC.
9. Idempotence et ETag.
10. OpenAPI à jour, exemples et erreurs.

### Critères de sortie

- aucun endpoint métier ne renvoie 200 avec `errors` ;
- aucun endpoint ne renvoie 500 pour une erreur de saisie ;
- aucun SQLSTATE en production ;
- le frontend peut paginer sans charger toute la table ;
- les créations renvoient les valeurs relues.

---

## 11. Lot 8 — Tests, CI et performance

### Tâches

- Testcontainers PostgreSQL.
- Fixtures entreprise A/B.
- Tests RBAC et tenancy.
- Tests de transaction et de concurrence.
- Tests de stock et de réconciliation.
- Tests de contrat (statuts, erreurs, pagination).
- Tests de sécurité (login, register, refresh, révocation).
- Tests de performance sur 1 000 articles.
- Étendre le workflow CI existant (`.github/workflows/ci.yml`), qui démarre déjà PostgreSQL 16, initialise le schéma et les données de démonstration, puis lance tests, lint OpenAPI et packaging.
- Ajouter au CI les tests de tenancy, RBAC, transaction, concurrence et réconciliation, ainsi que des seuils de couverture sur les modules critiques, pas sur le total.

### Critères de sortie

- la suite tourne sur une base vide ;
- aucun test ignoré silencieusement ;
- un test échoue si la tenancy est cassée ;
- un test échoue si une transaction partielle est possible ;
- un test échoue si un retour laisse l'article `ASSIGNED` ;
- le pipeline est bloquant sur `main`.

---

## 12. Lot 9 — Préparation frontend

Ce lot ne commence pas le frontend. Il produit les éléments nécessaires pour qu'il puisse démarrer sans être réécrit.

### Livrables

- contrat OpenAPI validé ;
- environnements et URLs ;
- stratégie de stockage du jeton ;
- stratégie de rafraîchissement et de logout ;
- catalogue des erreurs ;
- pagination et filtres ;
- maquettes de scan et d'étiquette ;
- critères d'acceptation du rapport 15 ;
- jeu de données de démonstration multi-entreprise.

### Go / No-Go

Le frontend de production peut démarrer seulement si :

- les P0 du rapport 12 sont fermés ;
- les tests de tenancy et RBAC passent ;
- le stock est défini et testé ;
- les affectations ont une seule API ;
- le scan QR est enrichi et tenant-scoped ;
- le contrat d'erreur est stable.

---

## 13. Séquencement et dépendances

```text
L0 Décisions
  └── L1 Identité / tenancy / RBAC
        └── L2 DataSource / transactions
              └── L3 Stock
                    ├── L4 Affectations
                    ├── L5 QR
                    └── L6 Comptages
                          └── L7 Contrat HTTP
                                └── L8 Tests / CI
                                      └── L9 Go frontend
```

### Pourquoi cet ordre ?

- Corriger le stock avant la tenancy créerait des agrégats faux par entreprise.
- Corriger les affectations avant les transactions laisserait des états partiels.
- Construire le scan avant le stock et les affectations donnerait une interface sans action fiable.
- Construire le frontend avant le contrat ferait porter la dette à l'interface.

---

## 14. Migration : règles de sécurité

1. **Sauvegarde complète** avant toute migration.
2. **Base de recette** identique à la production.
3. **Migrations additives** d'abord : nouvelles colonnes nullables, nouvelles tables.
4. **Backfill** contrôlé, avec requêtes de vérification.
5. **Double écriture** temporaire si nécessaire.
6. **Bascule des lectures** après validation.
7. **Contraintes** ajoutées seulement après nettoyage des données.
8. **Rollback** documenté et testé.
9. **Aucune suppression** de colonne avant une période d'observation.
10. **Aucune migration destructive** sans sauvegarde vérifiée.

### Points de contrôle

- nombre d'orphelins par FK ;
- nombre d'utilisateurs sans entreprise ;
- nombre d'articles sans type ou entrepôt ;
- nombre d'affectations actives incohérentes ;
- nombre de mouvements sans quantité/acteur ;
- nombre de comptages non clôturés ;
- nombre de QR dupliqués par entreprise.

---

## 15. Definition of Done globale

Une correction n'est terminée que si :

- le comportement est spécifié ;
- le code est revu ;
- les tests couvrent le cas nominal et les cas d'erreur ;
- la tenancy est testée ;
- la transaction est testée ;
- les erreurs sont stables ;
- l'OpenAPI est à jour ;
- la migration est réversible ;
- la documentation d'exploitation est à jour ;
- aucun secret ni SQLSTATE n'est exposé ;
- le frontend peut consommer le contrat sans contournement.

---

## 16. Ce qui ne doit pas être fait maintenant

- Commencer les écrans avant les P0.
- Ajouter des endpoints sans corriger la tenancy.
- Optimiser le N+1 avant de choisir le modèle de stock.
- Supprimer les anciennes routes avant d'avoir une période de compatibilité.
- Ajouter un PDF d'étiquette avant de stabiliser le code QR.
- Ajouter des tests sans base isolée.
- Migrer les données sans sauvegarde.

---

## 17. Conclusion

Le plan le plus court n'est pas « corriger les contrôleurs ». C'est :

1. fermer l'accès ;
2. rendre les écritures atomiques ;
3. choisir et implémenter une source de vérité pour le stock ;
4. unifier les affectations ;
5. stabiliser le contrat ;
6. seulement ensuite, construire le frontend.

Les critères de validation sont dans le rapport 15.
