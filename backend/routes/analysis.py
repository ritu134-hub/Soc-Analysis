"""
Analysis API Routes
POST /analysis/run    — run threat detection
GET  /analysis/alerts — list all alerts
"""

from fastapi import APIRouter
from services.analyzer import run_analysis, get_alerts

router = APIRouter(prefix="/analysis", tags=["Analysis"])


@router.post("/run")
def run():
    alerts = run_analysis()
    return {"status": "ok", "alerts_found": len(alerts), "alerts": alerts}


@router.get("/alerts")
def list_alerts():
    alerts = get_alerts()
    return {"total": len(alerts), "alerts": alerts}
