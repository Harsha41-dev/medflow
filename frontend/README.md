# MedFlow Frontend

React + TypeScript frontend for the MedFlow Spring Boot backend.

## Local Setup

```powershell
cd frontend
npm install
npm run dev
```

Set the backend URL in `.env`:

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

This portfolio frontend stores the JWT in `localStorage` to keep authentication simple and easy to explain. Production systems may choose a stronger token-handling strategy depending on the final architecture.
