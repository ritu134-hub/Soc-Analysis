"""
SOC Analysis — FastAPI Backend Entry Point
"""
import sys
import os
from dotenv import load_dotenv

# Load .env from project root
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "..", ".env"))

# Ensure the backend directory is in the path so imports work
sys.path.insert(0, os.path.dirname(__file__))

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from models.db import init_db
from routes.logs import router as logs_router
from routes.analysis import router as analysis_router
from routes.reports import router as reports_router
from routes.live import router as live_router


# ── App ──────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="SOC Analysis API",
    description="Splunk-inspired Security Operations Center log analysis backend.",
    version="1.0.0",
)

# Allow Java frontend (any localhost origin) to call the API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Database ─────────────────────────────────────────────────────────────────
@app.on_event("startup")
def on_startup():
    init_db()
    print("[SOC] Database initialized.")

# ── Routers ──────────────────────────────────────────────────────────────────
app.include_router(logs_router)
app.include_router(analysis_router)
app.include_router(reports_router)
app.include_router(live_router)


# ── Health ───────────────────────────────────────────────────────────────────
@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok", "service": "SOC Analysis API"}

@app.get("/", tags=["Health"])
def root():
    return {"message": "SOC Analysis API is running. Visit /docs for API explorer."}


if __name__ == "__main__":
    import uvicorn
    host = os.getenv("BACKEND_HOST", "0.0.0.0")
    port = int(os.getenv("BACKEND_PORT", 8000))
    uvicorn.run("main:app", host=host, port=port, reload=False)

