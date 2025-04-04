@echo off
set MESSAGE=%1
if "%MESSAGE%"=="" set MESSAGE=Deploy changes

echo 📦 Committing your code...
git add .
git commit -m "%MESSAGE%" || echo ⚠️ No new changes to commit.

echo ⬆️ Pushing to GitHub...
git push origin main

echo 🚀 Pushing to Heroku...
git push heroku main

echo ✅ Done!
pause
