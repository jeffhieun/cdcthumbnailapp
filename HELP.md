Got it 👍 Here’s a **shorter, clean README** that’s easy to follow:

---

# 🚀 CDC Thumbnail App — Quick Deploy Guide

## 1. Authenticate

```bash
gcloud auth login
gcloud config set project plasma-galaxy-472907-c7
```

---

## 2. Artifact Registry

Check or create repo:

```bash
gcloud artifacts repositories list --location=asia-southeast1
gcloud artifacts repositories create jeff-repo \
  --repository-format=docker \
  --location=asia-southeast1 \
  --description="CDC Thumbnail App Repo"
```

---

## 3. Build & Push (Gradle Jib)

```bash
./gradlew clean jib --no-daemon \
  -Djib.to.image=asia-southeast1-docker.pkg.dev/plasma-galaxy-472907-c7/jeff-repo/cdc-thumbnail-app \
  -Djib.to.credHelper=gcloud
```

---

## 4. Deploy to Cloud Run

```bash
gcloud run deploy cdc-thumbnail-app \
  --image asia-southeast1-docker.pkg.dev/plasma-galaxy-472907-c7/jeff-repo/cdc-thumbnail-app:latest \
  --region asia-southeast1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8099 \
  --set-env-vars SPRING_DATASOURCE_USERNAME=thumbnail_admin \
  --set-env-vars SPRING_DATASOURCE_PASSWORD=Aadmin@1234 \
  --set-env-vars SPRING_DATASOURCE_URL=jdbc:postgresql://10.83.224.7:5432/thumbnail-dev
```

---

## 5. Local Test

```bash
./gradlew clean build
PORT=8099 java -jar build/libs/thumbnail-poc-0.0.1-SNAPSHOT.jar
```

Run Cloud SQL Proxy:

```bash
./cloud-sql-proxy plasma-galaxy-472907-c7:asia-southeast1:cdc-thumbnail --port 5432
```

---

## 6. Workflow Test Inputs

**JSON 1**

```json
{
  "event": { "data": { "bucket": "thumbnail-workflow-bucket-demo12345", "name": "katinat1.jpg" }},
  "cloud_run_service_url": "https://clrun-thumbnail-processor-693024508611.asia-southeast1.run.app"
}
```

**JSON 2**

```json
{
  "event": { "data": { "bucket": "thumbnail-workflow-bucket-demo12345", "name": "System_Design_Note.html" }},
  "cloud_run_service_url": "https://clrun-thumbnail-processor-693024508611.asia-southeast1.run.app"
}
```

---
