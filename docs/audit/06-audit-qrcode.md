# 06 — Audit des QR Codes

**Objectif :** évaluer la génération, la lecture, l'impression et la résolution des QR Codes, ainsi que leur utilisabilité pour un frontend web ou mobile. Les rapports 04 et 05 traitent du catalogue et du stock ; ce rapport se concentre sur le contrat QR.

**État de référence :** HEAD `7e3f196`, qui corrige la livraison des erreurs JSON sur l'image QR. Les constats de l'audit initial antérieurs à ce commit sont signalés comme tels.

**Mise à jour du 22 octobre 2026 (Lot 2) :** un doublon de QR/code renvoie désormais 409 (`23505`) et les violations de FK/`CHECK` renvoient 422, sans SQLSTATE. Les mentions de « 500 » dans les sections 3 et 4 décrivent l'état initial, sauf les 500 fonctionnels `QR_CODE_MISSING` et `QR_CODE_GENERATION_FAILED` qui restent volontaires.

---

## 1. Ce qui existe

| Fonction | Route / code | Statut |
|---|---|---|
| Génération automatique à la création d'un article | `ItemDAOImpl.createItem` | **IMPLÉMENTÉ** |
| Code fourni par le client | `ItemCreateRequest.qr_code` | **IMPLÉMENTÉ** |
| Lecture des métadonnées du QR | `GET /api/inventory/items/{itemId}/qr-code` | **IMPLÉMENTÉ** |
| Image PNG | `GET /api/inventory/items/{itemId}/qr-code/image` | **IMPLÉMENTÉ** |
| Résolution d'un code scanné | `POST /api/inventory/scans/resolve` | **PARTIEL** |
| Étiquette PDF / planche d'impression | aucune route | **ABSENT** |
| Journal des scans | aucune table, aucune route | **ABSENT** |
| Révocation / régénération contrôlée | aucune route dédiée | **ABSENT** |

---

## 2. Génération du code

### 2.1. Règle réelle

Dans `ItemDAOImpl.createItem` :

```java
String qrCode = value.getQr_code();
if (qrCode == null || qrCode.isBlank()) {
  qrCode = "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}
```

- Si `qr_code` est absent, vide ou composé d'espaces, un code `QR-XXXXXXXX` est généré.
- `XXXXXXXX` correspond aux 8 premiers caractères hexadécimaux d'un UUID, en majuscules.
- Si `qr_code` est fourni, il est stocké tel quel, sans normalisation.

### 2.2. Unicité

La base impose :

```sql
UNIQUE(company_id, qr_code)
```

Conséquences :

- un même code peut exister dans deux entreprises différentes ;
- un doublon dans la même entreprise produit `23505`, converti en **500** ;
- aucun service ne pré-vérifie l'unicité ;
- aucun endpoint ne recherche un code de façon tenant-scoped.

### 2.3. Format

Aucune validation de format n'est appliquée :

- un code fourni peut être une URL, un UUID, un texte libre, un code-barres ou une chaîne vide ;
- `@Size(max = 100)` limite seulement la longueur ;
- le code généré fait 11 caractères (`QR-` + 8), mais rien n'impose ce format aux codes fournis ;
- un code fourni en minuscules reste en minuscules ;
- un code contenant des espaces est accepté.

### 2.4. Code modifiable

`PATCH /api/inventory/items/{itemId}` accepte `qr_code`. Le DAO l'écrit si la valeur n'est pas `null`.

- Le code peut être changé après impression.
- Une étiquette déjà collée devient invalide sans trace.
- Un code vide (`""`) est accepté par le DTO de mise à jour (pas de `@NotBlank`) et écrit en base.
- Un doublon produit 500.
- Aucun historique de changement de code n'existe.

**Impact :** un QR Code n'est pas un identifiant stable. Le mobile ne peut pas s'y fier sans version ni contrôle d'immutabilité.

---

## 3. Image PNG

### 3.1. Implémentation

`QrCodeService.generatePng` utilise ZXing `QRCodeWriter` :

- format `QR_CODE` ;
- correction d'erreur `M` ;
- marge `1` ;
- taille par défaut `300 × 300` ;
- sortie PNG dans un `ByteArrayOutputStream`.

La route :

```http
GET /api/inventory/items/{itemId}/qr-code/image
Accept: image/png
```

renvoie `Content-Type: image/png` et le tableau d'octets.

### 3.2. Ce qui fonctionne

- L'image est générée à la demande.
- Le contenu encode exactement la valeur `qr_code`.
- Le test `ItemApiTests.qrCodeImageReturnsPng` vérifie le type et la présence d'octets.
- Le test `ItemApiTests.qrCodeImageUnknownReturnsNotFound` vérifie le 404 sur un identifiant inconnu.
- La classe `ItemControllerQrCodeImageTest` (7 tests, 7/7 dans le rapport Surefire courant) vérifie le PNG exact, le 404, le 400 sur UUID invalide, le 500 `QR_CODE_MISSING`, les 500 `QR_CODE_GENERATION_FAILED` et la livraison d'un corps JSON même avec `Accept: image/png` strict.

### 3.3. Limites

- pas de cache HTTP (`Cache-Control`, `ETag`) ;
- pas de variante de taille, de marge ou de couleur ;
- pas de texte lisible sous le code ;
- pas de nom d'article, d'entrepôt ou de type dans l'image ;
- pas de planche d'étiquettes, de PDF ou d'impression en lot ;
- pas de stockage de l'image : elle est régénérée à chaque appel ;
- si le contenu est vide, la route renvoie désormais **500** avec l'enveloppe JSON `QR_CODE_MISSING` (et non plus un corps perdu) ; un UUID invalide renvoie **400** `BAD_REQUEST` et un article inconnu **404** `NOT_FOUND` ; un échec d'encodage ou de flux renvoie **500** `QR_CODE_GENERATION_FAILED` (correctif `7e3f196`) ;
- pas de contrôle d'accès autre que le JWT : toute personne authentifiée peut demander l'image de n'importe quel article.

---

## 4. Lecture des métadonnées

`GET /api/inventory/items/{itemId}/qr-code` renvoie `ItemQrCodeRest` :

- `id` ;
- `company_id` ;
- `category_id` ;
- `item_type_id` ;
- `current_warehouse_id` ;
- `qr_code`.

Ce n'est pas un contrat de scan : la route part de l'identifiant de l'article, pas du code. Elle expose des UUID, pas des libellés. Un opérateur ne peut pas lire « Perforateur Makita — entrepôt A » avec cette réponse.

---

## 5. Résolution d'un code scanné

### 5.1. Route

```http
POST /api/inventory/scans/resolve
Content-Type: application/json

{
  "qrCode": "QR-1A2B3C4D"
}
```

`QrScanRequest` impose `@NotBlank` sur `qrCode`.

### 5.2. Requête SQL

`QrScanDAOImpl.resolveQrCode` exécute :

```sql
SELECT * FROM security.item WHERE qr_code = ?
```

Sans filtre `company_id`, sans jointure, sans limite.

### 5.3. Réponse

Le mapper renvoie une liste de `QrScanItemRest` contenant la ligne brute de l'article :

- `id`, `company_id`, `category_id`, `item_type_id`, `qr_code`, `description`, `status`, `current_warehouse_id` ;
- `created_at`, `updated_at`, `archived_at`, `archived_by`, `last_inventory_at`.

La réponse ne contient pas :

- le nom du type ;
- le nom de la catégorie ;
- le nom de l'entrepôt ;
- l'affectation active ;
- l'employé détenteur ;
- le poste ;
- l'entreprise lisible.

### 5.4. Comportements

- Code inconnu : **200 `[]`**, pas 404.
- Code vide : **400** `VALIDATION_ERROR`.
- Code valide pour une entreprise : retourne l'article, même si l'appelant appartient à une autre entreprise.
- Code présent dans deux entreprises : retourne plusieurs lignes, sans indication de tenant.
- Code qui ressemble à un UUID : `id(qrCode)` tente `UUID.fromString`, puis lie un `UUID` à une colonne `VARCHAR`. Cela peut produire une erreur de type PostgreSQL `42883` et un **500** (non vérifié en exécution).
- Aucun journal : aucune table `qr_scan`, aucun `scanned_by`, aucun `scanned_at`, aucun résultat d'action.

### 5.5. Conséquences pour le mobile

Le mobile peut scanner et obtenir un article. Il ne peut pas :

- savoir dans quel entrepôt se trouve l'article de façon lisible ;
- savoir à qui il est affecté ;
- effectuer une action métier directement à partir du scan ;
- tracer le scan ;
- distinguer deux entreprises ;
- gérer un scan hors ligne puis synchroniser ;
- imprimer une étiquette depuis l'application.

Le scan est donc une **recherche d'article**, pas un **workflow mobile**.

---

## 6. Sécurité et confidentialité

| Risque | Détail |
|---|---|
| Absence de tenant | La requête de résolution est globale. |
| Énumération | Les codes générés font 8 caractères hexadécimaux (32 bits). Sans limitation de débit, une énumération est théoriquement possible. |
| Fuite de champs | La réponse renvoie `company_id`, `archived_by`, les UUID internes et les dates. |
| Code modifiable | Un attaquant autorisé peut changer un code et invalider les étiquettes. |
| Image accessible | Toute personne authentifiée peut générer l'image de n'importe quel article. |
| Absence de signature | Le code n'est ni signé, ni versionné, ni lié à un tenant. |

---

## 7. Ce qui manque pour un usage industriel

- Un code **stable et immuable** après impression, ou une version de code.
- Un scan **tenant-scoped**, avec le code d'entreprise dans le jeton ou dans l'URL.
- Une réponse **enrichie** : type, catégorie, entrepôt, affectation active, détenteur, statut lisible.
- Un **journal de scan** : qui, quand, où, sur quel appareil, pour quelle action.
- Des **actions idempotentes** : retour, sortie, transfert, comptage.
- Une **étiquette imprimable** : PDF, planche, taille, texte lisible.
- Une **gestion des codes dupliqués** entre entreprises : le scan doit demander le contexte ou filtrer.
- Une **limitation de débit** et une protection contre l'énumération.
- Un **contrôle d'accès** : un opérateur ne doit pas pouvoir scanner un article hors de son périmètre.

---

## 8. Tests existants

| Test | Ce qu'il vérifie | Ce qu'il ne vérifie pas |
|---|---|---|
| `QrScanApiTests.resolveKnownQrCodeReturnsItem` | Un code connu renvoie un article. | Le filtrage par entreprise, l'enrichissement, le journal. |
| `QrScanApiTests.resolveUnknownQrCodeReturnsEmptyArrayIn200` | Un code inconnu renvoie 200 `[]`. | Le 404, l'énumération, le rate limiting. |
| `QrScanApiTests.blankQrCodeReturnsBadRequest` | Le code vide est rejeté. | Le code trop long, le format, l'UUID. |
| `QrScanApiTests.malformedBodyReturnsBadRequest` | Le JSON invalide est rejeté. | Les doublons inter-entreprises. |
| `ItemApiTests.qrCodeEndpointReturnsCode` | La lecture des métadonnées. | L'immutabilité. |
| `ItemApiTests.qrCodeImageReturnsPng` | L'image est un PNG. | La taille, le contenu, le cache. |
| `ItemApiTests.qrCodeImageUnknownReturnsNotFound` | Le 404 sur un article inconnu. | Le filtrage par entreprise. |
| `ItemControllerQrCodeImageTest.returnsPngImageForKnownItem` | Les octets PNG exacts et le `Content-Type`. | Le cache, les variantes de taille. |
| `ItemControllerQrCodeImageTest.returnsNotFoundEnvelopeForUnknownItem` | Le 404 et l'enveloppe `NOT_FOUND`. | Le filtrage par entreprise. |
| `ItemControllerQrCodeImageTest.returnsBadRequestForInvalidItemId` | Le 400 `BAD_REQUEST` sur UUID invalide. | Les autres paramètres invalides. |
| `ItemControllerQrCodeImageTest.deliversJsonErrorBodyEvenWithStrictImagePngAccept` | Le corps JSON malgré `Accept: image/png`. | Les autres negotiateurs HTTP. |
| `ItemControllerQrCodeImageTest.returnsQrCodeMissingWhenQrCodeContentIsEmpty` | Le 500 `QR_CODE_MISSING`. | La régénération ou la correction du code. |
| `ItemControllerQrCodeImageTest.returnsGenerationFailedOnIoError` / `...OnEncodingError` | Les 500 `QR_CODE_GENERATION_FAILED`. | Les incidents matériels réels. |

Aucun test ne couvre :

- deux entreprises avec le même code ;
- un code modifié après création ;
- un code vide via `PATCH` ;
- l'impression en lot ;
- le scan avec action ;
- l'énumération ou le rate limiting.

---

## 9. Verdict

**La génération et l'image sont prêtes pour un prototype.** Le scan est **partiel** et ne constitue pas un contrat mobile.

Pour un frontend web, on peut afficher un QR Code et le télécharger. Pour un mobile, il faut d'abord :

1. stabiliser le code ;
2. filtrer par entreprise ;
3. enrichir la réponse ;
4. tracer les scans ;
5. définir les actions autorisées ;
6. produire une étiquette imprimable.

Les endpoints proposés sont dans le rapport 13, notamment la résolution tenant-scoped, le journal de scan et l'étiquette PDF.
