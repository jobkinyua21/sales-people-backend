#!/bin/bash
git stash
git stash clear
echo "Enter your GitHub credentials below"

git pull || { echo "Git pull failed! Check your credentials."; exit 1; }

rm -r target/*
mvn clean install || { echo "Maven build failed! Fix issues and retry."; exit 1; }

echo "Killing running application"
pkill -f "pos-system"
sleep 2  # Allow process to fully stop

echo "Starting sales-people application..."
nohup java -jar target/pos-system-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

sleep 2  # Give the app time to start

if pgrep -f "pos-system-0.0.1-SNAPSHOT.jar" > /dev/null; then
    sleep 10
    echo "Application started successfully."
else
    echo "Failed to start application!"
    exit 1
fi
tail -100f app.log
