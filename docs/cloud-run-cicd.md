# ShopLite Cloud Run CI/CD

This repository deploys the backend automatically with GitHub Actions:

`git push -> GitHub Actions -> Docker build -> Artifact Registry -> Cloud Run`

Frontend deploy flow:

`git push -> GitHub Actions -> npm build -> Firebase Hosting`

Workflow file:

`.github/workflows/ci.yml`

## Required GitHub Secrets

Create these in GitHub:

`Settings -> Secrets and variables -> Actions -> New repository secret`

Required:

- `GCP_SA_KEY`: Google service account JSON key.
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET_KEY`
- `SEPAY_WEBHOOK_SECRET`
- `MOMO_PARTNER_CODE`
- `MOMO_ACCESS_KEY`
- `MOMO_SECRET_KEY`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `TWILIO_FROM_NUMBER`
- `FIREBASE_CREDENTIALS_JSON`
- `FIREBASE_SERVICE_ACCOUNT_SHOPLITE_36F6C`: Firebase Hosting deploy service account JSON.

## Google Service Account Permissions

The service account used by `GCP_SA_KEY` needs:

- Artifact Registry Writer
- Cloud Run Admin
- Service Account User

## Trigger

CI runs on pushes and pull requests to `main`, `master`, and `develop`.

CD runs only on push to `main` or `master`.

- Backend deploy runs after both backend and frontend build jobs pass.
- Frontend deploy runs after the frontend build job passes.

You can also run it manually from:

`GitHub -> Actions -> ShopLite CI`

## Image

Images are pushed to:

`asia-southeast1-docker.pkg.dev/shoplite-36f6c/shoplite/shoplite-api`

Each deploy uses the commit SHA tag and also updates `latest`.
