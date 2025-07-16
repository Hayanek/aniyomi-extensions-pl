#!/bin/bash
set -e

rsync -a --delete --exclude .git --exclude --exclude repo.json ../master/repo/ .
git lfs install
git config --global user.email "Janek-bot@users.noreply.github.com"
git config --global user.name "Janek-bot"
git status
if [ -n "$(git status --porcelain)" ]; then
    git add .
    MSG="Update extensions repo"
    if [[ $(git log -1 --pretty=format:%s) =~ "$MSG" ]]; then
      git commit --amend --no-editQ
      git push --force
    else
      git commit -m "$MSG"
      git push
    fi
else
    echo "No changes to commit"
fi