"""
Log Parser Service
Supports: syslog (.log), CSV, JSON formats
"""

import re
import csv
import json
import io
from datetime import datetime
from typing import List, Dict, Any


SEVERITY_KEYWORDS = {
    "CRITICAL": ["critical", "emergency", "emerg", "alert", "panic"],
    "HIGH":     ["error", "err", "fail", "failed", "failure", "denied", "attack", "malware", "exploit"],
    "MEDIUM":   ["warning", "warn", "unauthorized", "invalid", "reject", "blocked"],
    "LOW":      ["notice", "info", "information", "accepted", "success", "started", "stopped"],
    "INFO":     ["debug", "verbose", "trace"],
}

IP_PATTERN = re.compile(r'\b(?:\d{1,3}\.){3}\d{1,3}\b')
SYSLOG_PATTERN = re.compile(
    r'^(?P<month>\w+)\s+(?P<day>\d+)\s+(?P<time>\d+:\d+:\d+)\s+'
    r'(?P<host>\S+)\s+(?P<process>\S+?)(?:\[(?P<pid>\d+)\])?\s*:\s+(?P<message>.+)$'
)


def detect_severity(text: str) -> str:
    lower = text.lower()
    for severity, keywords in SEVERITY_KEYWORDS.items():
        if any(k in lower for k in keywords):
            return severity
    return "INFO"


def detect_category(text: str) -> str:
    lower = text.lower()
    if any(k in lower for k in ["ssh", "login", "password", "auth", "pam", "su", "sudo"]):
        return "Authentication"
    if any(k in lower for k in ["firewall", "iptables", "blocked", "deny", "port", "connection"]):
        return "Network"
    if any(k in lower for k in ["malware", "virus", "ransomware", "trojan", "exploit"]):
        return "Malware"
    if any(k in lower for k in ["file", "disk", "permission", "chmod", "chown", "rm ", "delete"]):
        return "File System"
    if any(k in lower for k in ["cpu", "memory", "load", "swap", "oom"]):
        return "System"
    return "General"


def extract_ip(text: str) -> str:
    match = IP_PATTERN.search(text)
    return match.group(0) if match else "unknown"


def parse_syslog(content: str) -> List[Dict[str, Any]]:
    records = []
    year = datetime.now().year
    for line in content.splitlines():
        line = line.strip()
        if not line:
            continue
        m = SYSLOG_PATTERN.match(line)
        if m:
            ts_str = f"{m.group('month')} {m.group('day')} {year} {m.group('time')}"
            try:
                ts = datetime.strptime(ts_str, "%b %d %Y %H:%M:%S").isoformat()
            except ValueError:
                ts = datetime.now().isoformat()
            message = m.group("message")
        else:
            ts = datetime.now().isoformat()
            message = line

        records.append({
            "timestamp":  ts,
            "source_ip":  extract_ip(line),
            "severity":   detect_severity(line),
            "category":   detect_category(line),
            "message":    message[:500],
            "raw":        line[:1000],
        })
    return records


def parse_csv(content: str) -> List[Dict[str, Any]]:
    records = []
    reader = csv.DictReader(io.StringIO(content))
    for row in reader:
        row = {k.strip().lower(): v.strip() for k, v in row.items() if k}
        message = (
            row.get("message") or
            row.get("msg") or
            row.get("description") or
            str(row)
        )
        ts = (
            row.get("timestamp") or
            row.get("time") or
            row.get("date") or
            datetime.now().isoformat()
        )
        ip = row.get("source_ip") or row.get("ip") or row.get("src_ip") or extract_ip(message)
        sev = row.get("severity") or row.get("level") or detect_severity(message)
        records.append({
            "timestamp":  ts,
            "source_ip":  ip,
            "severity":   sev.upper() if sev else "INFO",
            "category":   detect_category(message),
            "message":    message[:500],
            "raw":        str(row)[:1000],
        })
    return records


def parse_json(content: str) -> List[Dict[str, Any]]:
    records = []
    data = json.loads(content)
    if isinstance(data, dict):
        data = [data]
    for entry in data:
        message = (
            entry.get("message") or
            entry.get("msg") or
            entry.get("description") or
            str(entry)
        )
        ts = (
            entry.get("timestamp") or
            entry.get("time") or
            entry.get("@timestamp") or
            datetime.now().isoformat()
        )
        ip = (
            entry.get("source_ip") or
            entry.get("src_ip") or
            entry.get("ip") or
            extract_ip(str(entry))
        )
        sev = entry.get("severity") or entry.get("level") or detect_severity(message)
        records.append({
            "timestamp":  str(ts),
            "source_ip":  str(ip),
            "severity":   str(sev).upper() if sev else "INFO",
            "category":   detect_category(message),
            "message":    str(message)[:500],
            "raw":        str(entry)[:1000],
        })
    return records


def parse_logs(filename: str, content: str) -> List[Dict[str, Any]]:
    ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else "log"
    if ext == "csv":
        return parse_csv(content)
    elif ext == "json":
        return parse_json(content)
    else:
        return parse_syslog(content)
