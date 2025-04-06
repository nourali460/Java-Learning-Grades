@echo off
set MESSAGE=%1
if "%MESSAGE%"=="" set MESSAGE=Deploy changes

echo 📦 Committing backend to GitHub (parent folder, branch: main)...
cd ..
git add .
git commit -m "%MESSAGE%" || echo ⚠️ No new changes to commit.
git push origin main
cd java-learning-backend

echo 🚀 Pushing to Heroku (current folder, branch: master)...
git add .
git commit -m "%MESSAGE%" || echo ⚠️ No new changes to commit.
git push heroku master

echo ✅ All done!
pause
