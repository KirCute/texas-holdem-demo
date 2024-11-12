cd frontend
npm install
npm run build
cd ..
mv frontend/dist/* backend/src/main/resources/static/
cd backend
mvn package
cd ..
docker build -t texas-holdem-demo:v1.1 .
