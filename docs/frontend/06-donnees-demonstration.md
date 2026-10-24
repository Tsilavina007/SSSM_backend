# Données de démonstration pour le frontend

## 1. Périmètre retenu

**Une seule entreprise : `SSSM Madagascar`.** Le déploiement est **mono-entreprise
par conception** (décision du 24 octobre 2026) : le jeu multi-entreprise est
**hors périmètre**, pas reporté. Les DAO d'inventaire ne filtrent pas par
`company_id` (`P0-01`, écart accepté) : ajouter une deuxième entreprise
exposerait ses articles, entrepôts et mouvements à tous les utilisateurs
authentifiés.

Aucune donnée de démonstration n'est modifiée au Lot 9.

## 2. Contenu du jeu existant

Fichiers : `docs/database/mock_data/sssm_mock_data.sql` et ses dépendances
`02_organisation.sql` … `14_audit.sql`.

| Domaine | Contenu |
|---|---|
| Entreprise | SSSM Madagascar, 3 agences, 3 branches |
| Comptes | 7 utilisateurs, dont un administrateur, des rôles métier et un compte `LOCKED` |
| Personnel | 14 employés (actifs, suspendu, congé, parti) |
| Sites | 6 sites, 8 postes et exigences |
| Inventaire | 4 catégories, 6 types, 13 articles avec QR, entrepôts, opérations, mouvements, seuils |
| Affectations | Affectations de démonstration, dont un retour |
| Comptages | 2 comptages, dont un en cours avec écarts |
| Clients / contrats / planning / rondes / incidents / facturation | Jeux de démonstration cohérents |

Les identifiants sont générés par PostgreSQL et chaînés entre fichiers par des
variables `psql` (`RETURNING id \gset`). Le chargement se fait via le seul
orchestrateur :

```bash
psql -d sssm_db -f docs/database/mock_data/sssm_mock_data.sql -U <user>
```

## 3. Comptes

Mot de passe commun : **`Demo@2024`**.

| Login | Rôle principal | Usage frontend |
|---|---|---|
| `demo.admin` | `ADMIN` plateforme + `Administrateur entreprise` | Tous les écrans, y compris administration |
| `lalao.superviseur` | `Superviseur` | Planning, sites, incidents, rapports |
| `soan.agent` | `Superviseur` | Même périmètre que le superviseur |
| `tahina.agent` | `Agent de sécurité` | Suivi et déclaration d'incident ; pas d'inventaire |
| `tsiky.agent` | `Agent de sécurité` | Idem |
| `voahangy.compta` | `Comptable` | Clients, contrats, facturation |
| `soa.locked` | `Agent de sécurité`, compte `LOCKED` | Doit être refusé au login |

`demo.admin` est le seul compte à porter `ROLE_ADMIN` et les permissions
d'inventaire nécessaires (`inventory.manage`). Les autres comptes ne doivent pas
accéder aux écrans d'inventaire : c'est un bon test de `403`.

## 4. Parcours de démonstration

### Catalogue

1. Se connecter avec `demo.admin`.
2. Lister les articles : la page 1 contient 10 éléments par défaut ; vérifier
   `total`, `pageSize` et `totalPages`.
3. Ouvrir un article : type, catégorie, entrepôt, statut.
4. Filtrer par entrepôt et par statut.
5. Créer un article : le QR code est généré si absent.

### Stock et affectations

1. Consulter `GET /api/v1/inventory/stock-balances` (50 par défaut).
2. Créer un mouvement `IN` de 2 unités : le solde augmente.
3. Affecter un article `AVAILABLE` à un employé et un poste.
4. Vérifier que l'article passe `ASSIGNED`, que l'entrepôt est vidé et que le
   solde diminue.
5. Retourner l'affectation : l'article redevient `AVAILABLE` dans son entrepôt.
6. Tenter un second retour : `409 ASSIGNMENT_ALREADY_RETURNED`.

### Scan et étiquette

1. Scanner le code d'un article de démonstration : type, entrepôt, affectation
   et détenteur s'affichent.
2. Scanner un code inexistant : `404`, message neutre.
3. Imprimer l'étiquette d'un article, puis une planche de quelques articles.

### Comptage

1. Ouvrir un comptage sur un entrepôt.
2. Scanner un article attendu, saisir la quantité comptée.
3. Vérifier l'écart calculé.
4. Clôturer et consulter les soldes et l'historique de l'article.

## 5. Anomalies de données connues

- **Affectation active incohérente** : une affectation active de la base de
  démonstration pointe vers un article `AVAILABLE` (signalé dans les rapports 12
  et 15, `ASG-15`). Elle n'a pas été corrigée automatiquement. L'écran de détail
  doit afficher l'incohérence plutôt que la masquer.
- **Articles historiques** : certains mouvements antérieurs au Lot 3 n'ont ni
  source, ni destination, ni acteur. L'interface doit tolérer des champs nuls.
- **Comptage en cours** : un comptage de démonstration peut être antérieur à
  l'instantané des attendus (`22_lot6_inventory_counts.sql`) ; un scan y est
  refusé en `409 ITEM_OUT_OF_SCOPE`. Le clôturer ou l'annuler avant la recette.

## 6. Réinitialisation

`docs/database/mock_data/reset_data.sql` est **destructif** : il vide les tables
du schéma `security` (sauf `role` et `permission`) et supprime les rôles
d'entreprise. Il ne doit jamais être exécuté sur une base partagée, de recette ou
de production, ni sur la base de démonstration existante sans sauvegarde.

Pour une recette frontend, utiliser une base locale dédiée créée pour l'occasion :

```bash
createdb sssm_front_demo
psql -d sssm_front_demo -f docs/database/sssm_schema2_v01.sql
psql -d sssm_front_demo -f docs/database/mock_data/sssm_mock_data.sql
```

Aucun reset n'est exécuté par le Lot 9.

## 7. Prérequis si une deuxième entreprise était admise (hors périmètre)

Le mono-entreprise est une décision produit ; ce chapitre est conservé comme
spécification de reprise. Avant d'ajouter une entreprise B, il faudrait :

1. corriger `P0-01` : filtrer chaque lecture et écriture d'inventaire par le
   `company_id` du jeton, et refuser les références croisées ;
2. ajouter des contraintes composites ou une RLS ;
3. créer des tests A/B en lecture et en écriture (`S-01`) ;
4. définir des comptes et des données B sans collision de QR code ;
5. documenter la procédure de chargement et de nettoyage.

Tant que ces points ne sont pas faits, le frontend reste sur une seule entreprise
et aucun écran ne doit proposer de sélection ou de création d'entreprise.
