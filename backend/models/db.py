"""
SQLite database setup using Python's built-in sqlite3.
Provides connection helpers and schema initialization.
"""

import sqlite3
import os
from pathlib import Path

DB_PATH = Path(__file__).parent.parent / "soc.db"


def get_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(str(DB_PATH), check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    """Create all tables if they don't exist."""
    conn = get_connection()
    cur = conn.cursor()

    cur.executescript("""
        CREATE TABLE IF NOT EXISTS logs (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp   TEXT,
            source_ip   TEXT,
            severity    TEXT,
            category    TEXT,
            message     TEXT,
            raw         TEXT,
            uploaded_at TEXT DEFAULT (datetime('now'))
        );

        CREATE TABLE IF NOT EXISTS alerts (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            rule_name   TEXT NOT NULL,
            description TEXT,
            severity    TEXT NOT NULL,
            source_ip   TEXT,
            count       INTEGER DEFAULT 1,
            detected_at TEXT DEFAULT (datetime('now'))
        );

        CREATE TABLE IF NOT EXISTS reports (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            filename     TEXT NOT NULL,
            generated_at TEXT DEFAULT (datetime('now')),
            log_count    INTEGER,
            alert_count  INTEGER
        );
    """)

    conn.commit()
    conn.close()
