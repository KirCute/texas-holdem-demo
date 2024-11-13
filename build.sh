git pull
cd frontend
npm install
npm run build
cd ..
mv backend/src/main/resources/static/README.md static_README.md.bak
rm -rf backend/src/main/resources/static
mkdir backend/src/main/resources/static
mv static_README.md.bak backend/src/main/resources/static/README.md
mv frontend/dist/* backend/src/main/resources/static/
cd backend
mvn package
cd ..
docker rmi texas-holdem-demo:v1.1
docker build -t texas-holdem-demo:v1.1 .
