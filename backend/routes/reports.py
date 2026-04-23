"""
Reports API Routes
POST /reports/generate  — generate a new HTML report
GET  /reports/list      — list all past reports
GET  /reports/{id}/path — get file path of a specific report
"""

from fastapi import APIRouter, HTTPException
from fastapi.responses import FileResponse
from services.reporter import generate_report, list_reports, get_report_path

router = APIRouter(prefix="/reports", tags=["Reports"])


@router.post("/generate")
def generate():
    result = generate_report()
    return result


@router.get("/list")
def list_all():
    reports = list_reports()
    return {"total": len(reports), "reports": reports}


@router.get("/{report_id}/download")
def download(report_id: int):
    path = get_report_path(report_id)
    if not path:
        raise HTTPException(status_code=404, detail="Report not found.")
    return FileResponse(path, media_type="text/html", filename=path.split("\\")[-1])


@router.get("/{report_id}/path")
def get_path(report_id: int):
    path = get_report_path(report_id)
    if not path:
        raise HTTPException(status_code=404, detail="Report not found.")
    return {"path": path}
