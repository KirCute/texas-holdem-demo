cd fronted
npm install
npm run build
cd ..
mv frontend/dist/* backend/main/resources/static/
cd backend
mvn package
cd ..
docker build -t texas-holdem-demo .