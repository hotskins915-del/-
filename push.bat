@echo off
cd /d "%~dp0"
git init
git add .
git commit -m "update"
git branch -M main
git remote remove origin 2>nul
git remote add origin https://github.com/hotskins915-del/-.git
git push origin main --force
echo.
echo Готово! Открой GitHub Actions чтобы следить за сборкой.
pause
