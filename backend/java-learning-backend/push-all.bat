@echo off
set MESSAGE=%1
if "%MESSAGE%"=="" set MESSAGE=Deploy changes

REM Go to the root project directory (assuming this script is inside /backend/java-learning-backend)
cd ../..

echo 📦 Committing frontend + backend to GitHub...
git add .
git commit -m "%MESSAGE%" || echo ⚠️ No new changes to commit.
git push origin main

echo 🚀 Pushing backend to Heroku...
cd backend/java-learning-backend
git add .
git commit -m "%MESSAGE%" || echo ⚠️ No new changes to commit.
git push heroku main

echo ✅ All done!
pause
