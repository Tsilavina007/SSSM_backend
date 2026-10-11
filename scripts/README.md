# Vérification avant push

Le script `check-before-push.ps1` vérifie les éléments les plus importants avant
un push :

- déclaration `package` cohérente avec le chemin du fichier Java ;
- imports dupliqués ;
- imports wildcard ;
- espaces en fin de ligne ;
- validation Maven ;
- compilation Java et tests.

Depuis la racine du projet, exécuter :

```powershell
.\scripts\check-before-push.ps1
```

Ou depuis l'invite de commandes Windows :

```cmd
scripts\check-before-push.cmd
```

Depuis Linux ou macOS :

```bash
./scripts/check-before-push.sh
```

Pour vérifier uniquement la compilation sans exécuter les tests :

```powershell
.\scripts\check-before-push.ps1 -SkipTests
```

Sous Linux ou macOS :

```bash
./scripts/check-before-push.sh --skip-tests
```

Le script retourne le code `0` si tout est correct et le code `1` si une
erreur est détectée. Il peut donc être appelé par un hook Git `pre-push` ou
par une pipeline CI.

Sur Linux ou macOS, rendre le script exécutable une première fois si
nécessaire :

```bash
chmod +x scripts/check-before-push.sh scripts/clean-before-push.sh
```

## Nettoyage automatique

Les scripts `clean-before-push.ps1` et `clean-before-push.sh` corrigent les
les problèmes de formatage et de style avant de relancer automatiquement le
checker :

- indentation et sauts de ligne Java avec `google-java-format` ;
- espaces en fin de ligne ;
- imports Java strictement dupliqués ;
- imports Java explicites non utilisés ;
- plusieurs lignes vides consécutives ;
- lignes vides à la fin des fichiers.

Sous Windows PowerShell :

```powershell
.\scripts\clean-before-push.ps1
```

Sous Linux ou macOS :

```bash
./scripts/clean-before-push.sh
```

Pour nettoyer puis compiler sans exécuter les tests :

```powershell
.\scripts\clean-before-push.ps1 -SkipTests
```

```bash
./scripts/clean-before-push.sh --skip-tests
```

Le nettoyage ne modifie pas automatiquement les imports wildcard (`.*`) ni
les espaces à l'intérieur du code ou des chaînes de caractères, car une
modification générale pourrait changer le comportement du programme. Ces
éléments restent listés par le checker pour être corrigés manuellement.
