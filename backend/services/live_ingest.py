"""
Live Ingestion Service for Windows
Monitors:
1. Windows Event Logs (System, Security)
2. Network Connections (Active TCP/UDP)
"""

import threading
import time
import psutil
from datetime import datetime
import win32evtlog

from models.db import get_connection
from services.log_parser import detect_severity, detect_category

class LiveMonitor:
    def __init__(self):
        self.running = False
        self.thread = None
        self.last_event_id = None

    def start(self):
        if not self.running:
            self.running = True
            self.thread = threading.Thread(target=self._run, daemon=True)
            self.thread.start()
            print("[Live Ingest] Monitoring started.")

    def stop(self):
        self.running = False
        print("[Live Ingest] Monitoring stopped.")

    def _run(self):
        while self.running:
            try:
                self._poll_windows_events("System")
                self._poll_network_connections()
            except Exception as e:
                print(f"[Live Ingest] Error: {e}")
            time.sleep(10)  # Poll every 10 seconds

    def _poll_windows_events(self, log_type):
        """Read recent Windows Event Logs."""
        server = 'localhost'
        handle = win32evtlog.OpenEventLog(server, log_type)
        flags = win32evtlog.EVENTLOG_BACKWARDS_READ | win32evtlog.EVENTLOG_SEQUENTIAL_READ
        
        events = win32evtlog.ReadEventLog(handle, flags, 0)
        if not events:
            return

        conn = get_connection()
        cur = conn.cursor()
        
        # Only take the 5 most recent new events to avoid flooding
        for event in events[:5]:
            ts = event.TimeGenerated.isoformat()
            msg = str(event.StringInserts) if event.StringInserts else "Windows Event"
            source = event.SourceName
            full_text = f"[{source}] {msg}"
            
            # Detect severity from text or EventType
            severity = detect_severity(full_text)
            if event.EventType == win32evtlog.EVENTLOG_ERROR_TYPE: severity = "CRITICAL"
            elif event.EventType == win32evtlog.EVENTLOG_WARNING_TYPE: severity = "MEDIUM"

            # Simple deduplication
            cur.execute("SELECT id FROM logs WHERE timestamp = ? AND message LIKE ? LIMIT 1", (ts, f"%{source}%"))
            if cur.fetchone():
                continue

            cur.execute(
                """INSERT INTO logs (timestamp, source_ip, severity, category, message, raw)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                (ts, "127.0.0.1", severity, "System", full_text[:500], f"WinEvent ID: {event.EventID}")
            )

        
        conn.commit()
        conn.close()

    def _poll_network_connections(self):
        """Monitor active network connections."""
        connections = psutil.net_connections(kind='inet')
        conn = get_connection()
        cur = conn.cursor()

        for c in connections:
            if c.status == 'ESTABLISHED':
                remote_ip = c.raddr.ip if c.raddr else "unknown"
                if remote_ip == "127.0.0.1" or remote_ip == "::1":
                    continue

                ts = datetime.now().isoformat()
                msg = f"Active Connection: {c.laddr.ip}:{c.laddr.port} -> {remote_ip}:{c.raddr.port} [{c.type}]"
                
                # Deduplicate: only log if we haven't seen this IP in the last 5 minutes
                cur.execute("SELECT id FROM logs WHERE source_ip = ? AND category = 'Network' AND uploaded_at > datetime('now', '-5 minutes') LIMIT 1", (remote_ip,))
                if cur.fetchone():
                    continue

                cur.execute(
                    """INSERT INTO logs (timestamp, source_ip, severity, category, message, raw)
                       VALUES (?, ?, ?, ?, ?, ?)""",
                    (ts, remote_ip, "INFO", "Network", msg, str(c))
                )
        
        conn.commit()
        conn.close()

# Global monitor instance
monitor = LiveMonitor()
