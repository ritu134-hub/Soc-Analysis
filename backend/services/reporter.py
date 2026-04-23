"""
HTML Report Generator using Jinja2
"""

import os
from datetime import datetime
from pathlib import Path
from jinja2 import Environment, FileSystemLoader

from models.db import get_connection

REPORTS_DIR = Path(__file__).parent.parent / "reports"
REPORTS_DIR.mkdir(exist_ok=True)

TEMPLATES_DIR = Path(__file__).parent.parent / "templates"


def generate_report() -> dict:
    conn = get_connection()
    cur = conn.cursor()

    # Fetch stats
    cur.execute("SELECT COUNT(*) as c FROM logs")
    log_count = cur.fetchone()["c"]

    cur.execute("SELECT COUNT(*) as c FROM alerts")
    alert_count = cur.fetchone()["c"]

    cur.execute("SELECT severity, COUNT(*) as c FROM logs GROUP BY severity")
    severity_dist = {row["severity"]: row["c"] for row in cur.fetchall()}

    cur.execute("""
        SELECT source_ip, COUNT(*) as c FROM logs
        WHERE source_ip != 'unknown'
        GROUP BY source_ip ORDER BY c DESC LIMIT 10
    """)
    top_ips = [dict(r) for r in cur.fetchall()]

    cur.execute("""
        SELECT category, COUNT(*) as c FROM logs
        GROUP BY category ORDER BY c DESC
    """)
    categories = [dict(r) for r in cur.fetchall()]

    cur.execute("SELECT * FROM alerts ORDER BY detected_at DESC")
    alerts = [dict(r) for r in cur.fetchall()]

    cur.execute("""
        SELECT * FROM logs ORDER BY timestamp DESC LIMIT 50
    """)
    recent_logs = [dict(r) for r in cur.fetchall()]

    conn.close()

    # Render template
    env = Environment(loader=FileSystemLoader(str(TEMPLATES_DIR)))
    template = env.get_template("report.html")

    generated_at = datetime.now()
    html_content = template.render(
        generated_at=generated_at.strftime("%Y-%m-%d %H:%M:%S"),
        log_count=log_count,
        alert_count=alert_count,
        severity_dist=severity_dist,
        top_ips=top_ips,
        categories=categories,
        alerts=alerts,
        recent_logs=recent_logs,
    )

    filename = f"report_{generated_at.strftime('%Y%m%d_%H%M%S')}.html"
    filepath = REPORTS_DIR / filename
    filepath.write_text(html_content, encoding="utf-8")

    # Record in DB
    conn = get_connection()
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO reports (filename, log_count, alert_count) VALUES (?, ?, ?)",
        (filename, log_count, alert_count)
    )
    conn.commit()
    conn.close()

    return {
        "filename":     filename,
        "path":         str(filepath),
        "log_count":    log_count,
        "alert_count":  alert_count,
        "generated_at": generated_at.isoformat(),
    }


def list_reports() -> list:
    conn = get_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM reports ORDER BY generated_at DESC")
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    return rows


def get_report_path(report_id: int) -> str | None:
    conn = get_connection()
    cur = conn.cursor()
    cur.execute("SELECT filename FROM reports WHERE id = ?", (report_id,))
    row = cur.fetchone()
    conn.close()
    if not row:
        return None
    return str(REPORTS_DIR / row["filename"])
