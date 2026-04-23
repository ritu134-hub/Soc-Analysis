# 🛡️ SOC Analysis Tool

A professional-grade Security Operations Center (SOC) Analysis Platform designed for real-time monitoring, log analysis, and automated threat detection. This tool provides a seamless integration between a JavaFX-based dashboard and a high-performance FastAPI backend.

## 🚀 Overview

The SOC Analysis Tool empowers security professionals to monitor Windows systems and networks, ingest logs, and detect malicious activities like brute force attacks and malware patterns through automated analysis.

### ✨ Key Features

-   **Real-time Monitoring**: Continuous tracking of Windows system metrics and network traffic.
-   **Automated Threat Detection**: Built-in rules for identifying common security threats (Brute Force, Malware signatures).
-   **Log Ingestion**: Effortlessly import and parse security logs for centralized analysis.
-   **Security Reporting**: Generate professional PDF/HTML reports summarizing findings and system health.
-   **Modern Dashboard**: A sleek JavaFX interface for clear visualization of security events.
-   **One-Click Launch**: Integrated setup and run scripts for easy deployment.

---

## 🛠️ Tech Stack

-   **Frontend**: JavaFX (JDK 17+)
-   **Backend**: Python FastAPI (Python 3.11+)
-   **Build Tool**: Apache Maven 3.x
-   **Database**: SQLite (Local persistent storage)
-   **Protocol**: HTTP/REST communication between Frontend and Backend

---

## 📥 Prerequisites

Before running the project, ensure you have the following installed:

1.  **Java Development Kit (JDK) 17 or higher**: [Download JDK](https://www.oracle.com/java/technologies/downloads/)
2.  **Python 3.11 or higher**: [Download Python](https://www.python.org/downloads/)
3.  **Maven**: Included in the repository (under `apache-maven-3.9.15/`), or you can use your system Maven.

---

## ⚙️ Installation & Setup

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/ritu134-hub/Soc-Analysis.git
    cd Soc-Analysis
    ```

2.  **Environment Configuration**:
    -   Copy `.env.example` to `.env`.
    -   Update the values in `.env` if necessary (e.g., ports, paths).

3.  **Run Setup Script**:
    The project includes a `setup.bat` script that automatically installs all necessary Python dependencies.
    -   Double-click `setup.bat` or run it via terminal:
        ```cmd
        setup.bat
        ```

---

## 🏃 Running the Application

To start both the backend and frontend, simply run the launcher script:

-   Double-click `run.bat` or run it via terminal:
    ```cmd
    run.bat
    ```

> [!NOTE]
> The first run might take 10-20 seconds as it initializes the local SQLite database and starts the background processes.

---

## 📂 Project Structure

```text
Soc-Analysis/
├── backend/            # FastAPI Python server (Analysis logic)
├── frontend/           # JavaFX Maven project (Dashboard UI)
├── sample_logs/        # Example log files for testing
├── .env                # Configuration file
├── setup.bat           # Dependency installation script
└── run.bat             # Application launcher
```

---

## 🔒 Security & Privacy

This tool is designed for security analysis. Please ensure you have the necessary permissions before monitoring systems or analyzing logs on any network.

---

## 🤝 Contributing

Contributions are welcome! If you have ideas for new detection rules or UI improvements:
1. Fork the repository.
2. Create a new feature branch.
3. Submit a Pull Request.

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
