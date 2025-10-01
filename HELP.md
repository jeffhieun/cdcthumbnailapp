Got it 👍 Since your **Cloud Run service is already built and running**, we can simplify the README by skipping **Step 1 (Build & Push Docker Image)** and starting directly from authentication + deploy/test steps.

Here’s the updated **clean README** 👇

---

# 📦 CDC Thumbnail App Deployment Guide

This guide covers how to manage and deploy the **CDC Thumbnail App** to **Google Cloud Run** using **Artifact Registry** and **Cloud SQL Proxy**.

---

## 1. Authenticate gcloud

```bash
gcloud auth login
gcloud config set project plasma-galaxy-472907-c7
gcloud auth list
```

---

## 2. Manage Artifact Registry

### 🔍 Check existing repositories

```bash
gcloud artifacts repositories list --location=asia-southeast1
```

### ➕ Create a repository (if not exists)

```bash
gcloud artifacts repositories create jeff-repo \
    --repository-format=docker \
    --location=asia-southeast1 \
    --description="Repository for CDC Thumbnail App"
```

### 📖 Describe repository

```bash
gcloud artifacts repositories describe jeff-repo --location=asia-southeast1
```

---

## 3. Build & Push with Gradle Jib (when updating image)

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

## 5. Optional: Clean Up Artifact Registry

```bash
# Delete repository
gcloud artifacts repositories delete jeff-repo \
    --location=asia-southeast1 \
    --quiet

# List repositories
gcloud artifacts repositories list --location=asia-southeast1
```

---

## 6. Local Testing

### 🏃 Run app locally
```bash
./gradlew clean build
```

```bash
PORT=8099 java -jar build/libs/thumbnail-poc-0.0.1-SNAPSHOT.jar
```

### 🔑 Start Cloud SQL Proxy

```bash
hieunguyen@Hieus-MacBook-Pro ~ % ./cloud-sql-proxy plasma-galaxy-472907-c7:asia-southeast1:cdc-thumbnail --port 5432

2025/10/01 16:37:34 Authorizing with Application Default Credentials
2025/10/01 16:37:34 [plasma-galaxy-472907-c7:asia-southeast1:cdc-thumbnail] Listening on 127.0.0.1:5432
2025/10/01 16:37:34 The proxy has started successfully and is ready for new connections!
```

---

✅ This version assumes the **Cloud Run service already exists**, and you’ll only need to rebuild/redeploy when updating your code.

Do you want me to also add a **short "Update flow" section** (like: *Edit code → Run Jib → Deploy → Test*) so your team knows the minimal steps for future updates?
