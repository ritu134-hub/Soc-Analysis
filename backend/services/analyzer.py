"""
Threat Detection / Analysis Service
5 built-in detection rules:
  1. Brute Force Login
  2. Port Scan
  3. Privilege Escalation
  4. Off-Hours Access
  5. Suspicious Keyword Match
"""

import sqlite3
from collections import defaultdict
from datetime import datetime
from typing import List, Dict, Any

from models.db import get_connection


def run_analysis() -> List[Dict[str, Any]]:
    conn = get_connection()
    cur = conn.cursor()

    # Clear previous alerts
    cur.execute("DELETE FROM alerts")
    conn.commit()

    cur.execute("SELECT * FROM logs ORDER BY timestamp")
    logs = [dict(row) for row in cur.fetchall()]
    conn.close()

    alerts = []
    alerts.extend(_brute_force(logs))
    alerts.extend(_privilege_escalation(logs))
    alerts.extend(_off_hours_access(logs))
    alerts.extend(_suspicious_keywords(logs))
    alerts.extend(_repeated_denials(logs))

    # Save alerts to DB
    conn = get_connection()
    cur = conn.cursor()
    for a in alerts:
        cur.execute(
            """INSERT INTO alerts (rule_name, description, severity, source_ip, count)
               VALUES (?, ?, ?, ?, ?)""",
            (a["rule_name"], a["description"], a["severity"], a["source_ip"], a["count"])
        )
    conn.commit()
    conn.close()

    return alerts


def _brute_force(logs: List[Dict]) -> List[Dict]:
    """Rule 1: 5+ failed logins from the same IP."""
    fail_keywords = ["failed password", "authentication failure", "invalid user", "failed login"]
    ip_counts = defaultdict(int)

    for log in logs:
        msg = (log.get("message") or "").lower()
        if any(k in msg for k in fail_keywords):
            ip = log.get("source_ip", "unknown")
            if ip and ip != "unknown":
                ip_counts[ip] += 1

    alerts = []
    for ip, count in ip_counts.items():
        if count >= 5:
            alerts.append({
                "rule_name":   "Brute Force Attack",
                "description": f"IP {ip} had {count} failed login attempts — possible brute force.",
                "severity":    "CRITICAL",
                "source_ip":   ip,
                "count":       count,
            })
    return alerts


def _privilege_escalation(logs: List[Dict]) -> List[Dict]:
    """Rule 2: sudo / su commands in logs."""
    priv_keywords = ["sudo", "su root", "sudo su", "privilege", "escalat", "runas"]
    ip_counts = defaultdict(int)

    for log in logs:
        msg = (log.get("message") or "").lower()
        if any(k in msg for k in priv_keywords):
            ip = log.get("source_ip", "unknown")
            ip_counts[ip] += 1

    alerts = []
    for ip, count in ip_counts.items():
        if count >= 1:
            alerts.append({
                "rule_name":   "Privilege Escalation",
                "description": f"IP {ip} triggered {count} privilege escalation event(s).",
                "severity":    "HIGH",
                "source_ip":   ip,
                "count":       count,
            })
    return alerts


def _off_hours_access(logs: List[Dict]) -> List[Dict]:
    """Rule 3: Logins between 10 PM and 6 AM."""
    off_keywords = ["accepted password", "session opened", "logged in", "login successful"]
    ip_counts = defaultdict(int)

    for log in logs:
        msg = (log.get("message") or "").lower()
        ts_str = log.get("timestamp") or ""
        if not any(k in msg for k in off_keywords):
            continue
        try:
            ts = datetime.fromisoformat(ts_str)
            if ts.hour >= 22 or ts.hour < 6:
                ip = log.get("source_ip", "unknown")
                ip_counts[ip] += 1
        except Exception:
            continue

    alerts = []
    for ip, count in ip_counts.items():
        if count >= 1:
            alerts.append({
                "rule_name":   "Off-Hours Access",
                "description": f"IP {ip} had {count} login(s) between 10 PM–6 AM.",
                "severity":    "MEDIUM",
                "source_ip":   ip,
                "count":       count,
            })
    return alerts


def _suspicious_keywords(logs: List[Dict]) -> List[Dict]:
    """Rule 4: Known malicious keywords in log messages."""
    keywords = ["malware", "ransomware", "exploit", "backdoor", "rootkit",
                "sql injection", "xss", "shellcode", "payload", "c2", "botnet"]
    ip_counts = defaultdict(int)

    for log in logs:
        msg = (log.get("message") or "").lower()
        if any(k in msg for k in keywords):
            ip = log.get("source_ip", "unknown")
            ip_counts[ip] += 1

    alerts = []
    for ip, count in ip_counts.items():
        alerts.append({
            "rule_name":   "Malware / Exploit Indicator",
            "description": f"IP {ip} triggered {count} suspicious keyword(s) in logs.",
            "severity":    "CRITICAL",
            "source_ip":   ip,
            "count":       count,
        })
    return alerts


def _repeated_denials(logs: List[Dict]) -> List[Dict]:
    """Rule 5: 10+ firewall / access denials from same IP."""
    deny_keywords = ["denied", "blocked", "reject", "drop", "forbidden", "access denied"]
    ip_counts = defaultdict(int)

    for log in logs:
        msg = (log.get("message") or "").lower()
        if any(k in msg for k in deny_keywords):
            ip = log.get("source_ip", "unknown")
            if ip and ip != "unknown":
                ip_counts[ip] += 1

    alerts = []
    for ip, count in ip_counts.items():
        if count >= 10:
            alerts.append({
                "rule_name":   "Repeated Access Denials",
                "description": f"IP {ip} was denied {count} times — possible reconnaissance.",
                "severity":    "HIGH",
                "source_ip":   ip,
                "count":       count,
            })
    return alerts


def get_alerts() -> List[Dict]:
    conn = get_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM alerts ORDER BY detected_at DESC")
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    return rows
