from fastapi import APIRouter
from services.live_ingest import monitor

router = APIRouter(prefix="/live", tags=["Live Ingestion"])

@router.post("/start")
def start_monitor():
    monitor.start()
    return {"status": "ok", "message": "Live monitoring started."}

@router.post("/stop")
def stop_monitor():
    monitor.stop()
    return {"status": "ok", "message": "Live monitoring stopped."}

@router.get("/status")
def get_status():
    return {"running": monitor.running}
