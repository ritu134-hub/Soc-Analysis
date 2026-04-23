"""
Logs API Routes
POST /logs/upload  — upload a log file
GET  /logs/search  — search & filter logs
GET  /logs/stats   — summary statistics
DELETE /logs/clear — clear all logs
"""

from fastapi import APIRouter, UploadFile, File, HTTPException, Query
from fastapi.responses import JSONResponse
from typing import Optional

from models.db import get_connection
from services.log_parser import parse_logs

router = APIRouter(prefix="/logs", tags=["Logs"])


@router.post("/upload")
async def upload_logs(file: UploadFile = File(...)):
    content = (await file.read()).decode("utf-8", errors="replace")
    records = parse_logs(file.filename, content)

    if not records:
        raise HTTPException(status_code=400, detail="No valid log entries found in the file.")

    conn = get_connection()
    cur = conn.cursor()
    cur.executemany(
        """INSERT INTO logs (timestamp, source_ip, severity, category, message, raw)
           VALUES (:timestamp, :source_ip, :severity, :category, :message, :raw)""",
        records
    )
    conn.commit()
    conn.close()

    return {"status": "ok", "inserted": len(records)}


@router.get("/search")
def search_logs(
    q:        Optional[str] = Query(None, description="Keyword search"),
    severity: Optional[str] = Query(None, description="CRITICAL|HIGH|MEDIUM|LOW|INFO"),
    category: Optional[str] = Query(None),
    from_ts:  Optional[str] = Query(None, alias="from"),
    to_ts:    Optional[str] = Query(None, alias="to"),
    page:     int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=500),
):
    conn = get_connection()
    cur = conn.cursor()

    sql = "SELECT * FROM logs WHERE 1=1"
    params = []

    if q:
        sql += " AND (message LIKE ? OR source_ip LIKE ?)"
        params += [f"%{q}%", f"%{q}%"]
    if severity:
        sql += " AND severity = ?"
        params.append(severity.upper())
    if category:
        sql += " AND category = ?"
        params.append(category)
    if from_ts:
        sql += " AND timestamp >= ?"
        params.append(from_ts)
    if to_ts:
        sql += " AND timestamp <= ?"
        params.append(to_ts)

    # Total count
    count_sql = sql.replace("SELECT *", "SELECT COUNT(*) as c")
    cur.execute(count_sql, params)
    total = cur.fetchone()["c"]

    sql += " ORDER BY timestamp DESC LIMIT ? OFFSET ?"
    params += [page_size, (page - 1) * page_size]
    cur.execute(sql, params)
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()

    return {"total": total, "page": page, "page_size": page_size, "logs": rows}


@router.get("/stats")
def get_stats():
    conn = get_connection()
    cur = conn.cursor()

    cur.execute("SELECT COUNT(*) as c FROM logs")
    total_logs = cur.fetchone()["c"]

    cur.execute("SELECT severity, COUNT(*) as c FROM logs GROUP BY severity")
    by_severity = {r["severity"]: r["c"] for r in cur.fetchall()}

    cur.execute("SELECT category, COUNT(*) as c FROM logs GROUP BY category ORDER BY c DESC LIMIT 5")
    by_category = [dict(r) for r in cur.fetchall()]

    cur.execute("""
        SELECT source_ip, COUNT(*) as c FROM logs
        WHERE source_ip != 'unknown'
        GROUP BY source_ip ORDER BY c DESC LIMIT 10
    """)
    top_ips = [dict(r) for r in cur.fetchall()]

    cur.execute("SELECT COUNT(*) as c FROM alerts")
    total_alerts = cur.fetchone()["c"]

    cur.execute("SELECT COUNT(*) as c FROM reports")
    total_reports = cur.fetchone()["c"]

    conn.close()

    return {
        "total_logs":    total_logs,
        "total_alerts":  total_alerts,
        "total_reports": total_reports,
        "by_severity":   by_severity,
        "by_category":   by_category,
        "top_ips":       top_ips,
    }


@router.delete("/clear")
def clear_logs():
    conn = get_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM logs")
    cur.execute("DELETE FROM alerts")
    conn.commit()
    conn.close()
    return {"status": "ok", "message": "All logs and alerts cleared."}
