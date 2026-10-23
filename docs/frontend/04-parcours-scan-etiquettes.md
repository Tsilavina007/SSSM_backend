# Parcours de scan et d'étiquetage

Ces maquettes décrivent les écrans à produire, pas le code. Elles s'appuient sur
le contrat des Lots 5 et 7.

## 1. Scan QR

### Route

```http
POST /api/v1/inventory/scans/resolve
Authorization: Bearer <access token>
Content-Type: application/json

{ "qrCode": "QR-AB12CD34", "deviceId": "mobile-tana-01" }
```

- `qrCode` est obligatoire (100 caractères maximum).
- `deviceId` est facultatif ; il alimente le journal des scans.
- Un code inconnu, archivé ou appartenant à une autre entreprise renvoie `404`.
- Chaque tentative est journalisée, y compris les `404`.
- Limite : 120 résolutions par minute et par utilisateur/entreprise
  (`QR_SCAN_MAX_PER_MINUTE`, fenêtre réglable) → `429 RATE_LIMITED`.

### Réponse `200`

```json
{
  "id": "6f1c…",
  "qr_code": "QR-AB12CD34",
  "description": "Gilet pare-balles taille M",
  "status": "ASSIGNED",
  "item_type": { "id": "…", "code": "GPB", "name": "Gilet pare-balles" },
  "category": { "id": "…", "name": "Protection" },
  "warehouse": { "id": "…", "name": "Entrepôt principal" },
  "active_assignment": {
    "id": "…",
    "assigned_at": "2026-10-20T08:15:00Z",
    "expected_return_at": "2026-10-27T08:15:00Z",
    "employee": {
      "id": "…",
      "employee_number": "EMP-014",
      "first_name": "Tahina",
      "last_name": "Razafindrabe"
    },
    "post": { "id": "…", "code": "POST-03", "name": "Poste nuit — Ivandry" }
  }
}
```

`active_assignment` vaut `null` si l'article n'est pas affecté ; `warehouse` vaut
`null` si l'article n'est dans aucun entrepôt.

### Maquette mobile — résultat

```text
┌──────────────────────────────────────┐
│ ←  Scan                             │
├──────────────────────────────────────┤
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  │        [ viseur caméra ]       │  │
│  │                                │  │
│  └────────────────────────────────┘  │
│   Saisir le code : [ QR-________ ]   │
│   [ Scanner ]                        │
├──────────────────────────────────────┤
│  QR-AB12CD34            ASSIGNED     │
│  Gilet pare-balles taille M          │
│  Protection · Gilet pare-balles      │
│  Entrepôt principal                  │
│  ────────────────────────────────    │
│  Détenteur : Tahina Razafindrabe     │
│  Matricule : EMP-014                 │
│  Poste     : Poste nuit — Ivandry    │
│  Depuis    : 20/10/2026 11:15        │
│  Retour prévu : 27/10/2026 11:15     │
│  ────────────────────────────────    │
│  [ Voir l'article ] [ Retourner ]    │
└──────────────────────────────────────┘
```

### États de l'écran

| État | Déclencheur | Affichage |
|---|---|---|
| Prêt | Écran ouvert | Viseur + saisie manuelle |
| Chargement | Appel en cours | Désactiver le bouton, libellé « Résolution… » |
| Trouvé, disponible | `status = AVAILABLE` | Détail + action « Affecter » |
| Trouvé, affecté | `status = ASSIGNED` et `active_assignment` non nul | Détenteur, poste, action « Retourner » |
| Trouvé, autre statut | `DAMAGED`, `LOST`, `REPAIR`, `RETIRED` | Badge d'état + action selon les droits, pas de retour direct |
| Inconnu | `404 NOT_FOUND` | « Code inconnu ou non autorisé » + « Réessayer » |
| Limite atteinte | `429 RATE_LIMITED` | « Trop de scans, patientez un instant » |
| Réseau | Pas de réponse | « Connexion indisponible », bouton « Réessayer » |
| Session expirée | `401` | Refresh unique puis login si échec |

### Règles d'interface

- Ne pas déduire les actions du seul `status` : croiser avec
  `active_assignment` ; un article `ASSIGNED` sans affectation est une donnée
  incohérente à signaler.
- Le scan ne modifie rien : les actions « Affecter », « Retourner » appellent
  les routes dédiées et affichent leurs erreurs (`409`, `422`).
- Ne pas relancer automatiquement un scan après `429` ou après une création :
  pas d'idempotence (`P1-14`).
- Le champ de saisie manuelle doit accepter les codes qui ressemblent à un UUID ;
  le backend ne les interprète plus comme des identifiants.
- Conserver le `deviceId` stable par terminal pour que le journal soit
  exploitable.
- Aucun code d'une autre entreprise ne doit être révélé : le `404` est volontaire.

## 2. Étiquettes

### Routes

| Route | Réponse | Usage |
|---|---|---|
| `GET /api/v1/inventory/items/{itemId}/label` | HTML `text/html` | Une étiquette |
| `POST /api/v1/inventory/labels/batch` | HTML `text/html` | Planche de 1 à 100 articles |

Corps du lot :

```json
{ "item_ids": ["uuid-1", "uuid-2"], "size": 300 }
```

`size` est borné à 100–1000 px (défaut 300). Les articles inconnus ou d'une autre
entreprise sont ignorés silencieusement.

### Contrainte d'authentification

Ces routes exigent `Authorization: Bearer`. Une navigation directe du navigateur
vers l'URL ne porte pas cet en-tête. Le frontend doit :

1. appeler la route avec `fetch` et le jeton ;
2. récupérer un `Blob` HTML ;
3. créer une URL objet (`URL.createObjectURL`) ;
4. l'ouvrir dans un onglet ou une `iframe` d'impression ;
5. libérer l'URL objet après impression.

Pour une grande planche, préférer une fenêtre dédiée avec une `iframe` et
`window.print()`.

### Maquette — écran d'impression

```text
┌─────────────────────────────────────────────────────┐
│ Étiquettes                                          │
├─────────────────────────────────────────────────────┤
│ Sélection : 12 articles          Taille QR : [300]  │
│ [ Sélectionner tout ]  [ Tout désélectionner ]      │
│                                                     │
│ ☑ QR-AB12CD34  Gilet pare-balles — Protection       │
│ ☑ QR-AB12CD35  Gilet pare-balles — Protection       │
│ ☐ QR-AB12CD36  Radio — Communication                │
│ …                                                   │
│                                                     │
│ [ Aperçu (12) ]   [ Imprimer la planche ]           │
└─────────────────────────────────────────────────────┘
```

Aperçu imprimable produit par le backend :

```text
┌───────────────────────────────┐  ┌───────────────────────────────┐
│ ███  QR-AB12CD34              │  │ ███  QR-AB12CD35              │
│ ███  GPB — Gilet pare-balles  │  │ ███  GPB — Gilet pare-balles  │
│      Gilet taille M           │  │      Gilet taille L           │
│      Protection               │  │      Protection               │
│      Entrepôt principal       │  │      Entrepôt principal       │
│      SSSM Madagascar          │  │      SSSM Madagascar          │
└───────────────────────────────┘  └───────────────────────────────┘
```

### Règles d'interface

- Le code QR est **immuable** : ne pas proposer d'édition après impression.
- Afficher le code en texte lisible à côté de l'image : une étiquette sans image
  reste utilisable.
- Limiter la sélection à 100 articles par planche (validation backend `400`
  au-delà).
- Pas de PDF généré par le serveur : l'utilisateur passe par l'impression du
  navigateur, avec `@page A4` déjà défini.
- Vérifier les droits d'accès avant d'afficher les boutons ; le backend reste
  l'autorité (`401`/`403`).
- Ne pas mettre l'URL d'étiquette dans un lien partageable : elle exige un jeton.

## 3. Accessibilité et impression

- Contraste suffisant sur les badges d'état ; ne pas coder l'état par la seule
  couleur.
- Taille de cible tactile ≥ 44 px pour les boutons du scan.
- `aria-live` sur le résultat du scan.
- Zone d'impression sans en-tête ni navigation, marges A4, saut de page contrôlé.
- Gérer le mode portrait pour la planche, paysage possible pour les grandes
  étiquettes.
