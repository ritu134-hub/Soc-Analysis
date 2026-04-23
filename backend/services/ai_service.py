import os
from groq import Groq
from dotenv import load_dotenv

# Load .env file
load_dotenv()

API_KEY = os.getenv("GROQ_API_KEY")

if API_KEY:
    client = Groq(api_key=API_KEY)
else:
    client = None

def explain_log_entry(log_message: str):
    """
    Sends a log message to Groq (Llama 3) for explanation.
    """
    if not client:
        return "Groq API key not configured. Please add GROQ_API_KEY to your .env file."

    prompt = f"""
    You are a professional Security Operations Center (SOC) Analyst. 
    Explain the following system/security log message in simple, clear language for a junior analyst.
    
    LOG MESSAGE:
    "{log_message}"
    
    Please provide:
    1. A clear explanation of what happened.
    2. The severity (Low, Medium, High, or Critical).
    3. Recommended action steps if any risk is detected.
    
    Format the response with clear headers and bullet points.
    """
    
    try:
        completion = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": "You are a professional SOC Analyst assistant."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.5,
            max_tokens=1024,
            top_p=1,
            stream=False,
        )
        return completion.choices[0].message.content
    except Exception as e:
        # Fallback to a different model if versatile is busy
        try:
            completion = client.chat.completions.create(
                model="llama3-70b-8192",
                messages=[
                    {"role": "system", "content": "You are a professional SOC Analyst assistant."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.5,
                max_tokens=1024,
            )
            return completion.choices[0].message.content
        except Exception as e2:
            return f"Error communicating with Groq: {str(e2)}"

def get_mitigation_plan(alert_name: str, description: str):
    """
    Generates a 3-step mitigation plan for a detected alert.
    """
    if not client:
        return "Groq API key not configured."

    prompt = f"""
    You are a Senior SOC Analyst. A security alert has been triggered:
    ALERT: {alert_name}
    DETAILS: {description}
    
    Provide exactly 3 immediate, practical action steps for a SOC analyst to mitigate this specific threat.
    Keep the steps concise and professional.
    """
    
    try:
        completion = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": "You are a senior security consultant."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.3,
            max_tokens=500,
        )
        return completion.choices[0].message.content
    except Exception as e:
        # Fallback for mitigation plan too
        try:
            completion = client.chat.completions.create(
                model="llama3-70b-8192",
                messages=[
                    {"role": "system", "content": "You are a senior security consultant."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.3,
                max_tokens=500,
            )
            return completion.choices[0].message.content
        except Exception as e2:
            return f"Error generating plan: {str(e2)}"
