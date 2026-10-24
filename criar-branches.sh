#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
command -v git >/dev/null || { echo 'Git necessario.' >&2; exit 1; }
if [[ ! -e .git ]]; then
    git init -b main
fi
[[ -z "$(git rev-parse --show-prefix)" ]] || { echo 'Execute na raiz do projeto.' >&2; exit 1; }
git var GIT_AUTHOR_IDENT >/dev/null
git var GIT_COMMITTER_IDENT >/dev/null
if ! git rev-parse --verify HEAD >/dev/null 2>&1; then
    [[ "$(git symbolic-ref --short HEAD)" == main ]] || { echo 'Repositorio vazio deve estar na main.' >&2; exit 1; }
    git add -- .
    git commit -m 'Base de treino Object Calisthenics'
fi
git show-ref --verify --quiet refs/heads/main || { echo 'main ausente.' >&2; exit 1; }
[[ -z "$(git status --porcelain)" ]] || { echo 'Salve ou guarde as alteracoes antes de executar.' >&2; exit 1; }
original=$(git symbolic-ref --short HEAD) || { echo 'HEAD deve estar em uma branch.' >&2; exit 1; }
trap 'git switch --quiet "$original"' EXIT
branches=(
    regra-01-um-nivel-indentacao
    regra-02-sem-else
    regra-03-encapsular-primitivos-strings
    regra-04-colecoes-primeira-classe
    regra-05-um-ponto-por-linha
    regra-06-sem-abreviacoes
    regra-07-entidades-pequenas
    regra-08-dois-atributos-por-classe
    regra-09-sem-getters-setters
    todas-as-regras
)
for branch in "${branches[@]}"; do
    if git show-ref --verify --quiet "refs/heads/$branch"; then
        echo "Preservada: $branch"
        continue
    fi
    git switch --quiet -c "$branch" main
    [[ ! -e NOTAS.md ]] || { echo 'NOTAS.md ja existe na base; nao sera sobrescrito.' >&2; exit 1; }
    cat > NOTAS.md <<EOF
# Notas — $branch

## O que mudou

## O que foi dificil

## Onde a regra pareceu exagerada

## O que faria diferente num projeto real

## Verificacoes executadas
EOF
    git add -- NOTAS.md
    git commit --quiet -m "Preparar notas: $branch"
    echo "Criada: $branch"
done
