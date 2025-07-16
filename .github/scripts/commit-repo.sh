#!/bin/bash
set -e

rsync -a --delete --exclude .git --exclude .gitignore --exclude repo.json ../master/repo/ .
git config --global user.email "Jacek-Bot@gihub.com"
git config --global user.name "Jacek-Bot"
git status
if [ -n "$(git status --porcelain)" ]; then
    git add .
    git commit -S -m "Update extensions repo"
    git push

    # Purge cached index on jsDelivr
    curl https://purge.jsdelivr.net/gh/Hayanek/aniyomi-extensions-pl@repo/index.min.json
else
    echo "No changes to commit"
fi