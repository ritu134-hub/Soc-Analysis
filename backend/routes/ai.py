from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from services.ai_service import explain_log_entry, get_mitigation_plan

router = APIRouter(prefix="/api/ai", tags=["AI"])

class ExplainRequest(BaseModel):
    log_message: str

class MitigateRequest(BaseModel):
    alert_name: str
    description: str

@router.post("/explain")
async def explain_log(request: ExplainRequest):
    """
    Analyzes a log entry using AI and returns a human-friendly explanation.
    """
    if not request.log_message or len(request.log_message.strip()) < 5:
        raise HTTPException(status_code=400, detail="Log message too short to analyze.")
    
    explanation = explain_log_entry(request.log_message)
    return {"explanation": explanation}

@router.post("/mitigate")
async def mitigate_alert(request: MitigateRequest):
    """
    Generates a mitigation plan for a detected alert.
    """
    plan = get_mitigation_plan(request.alert_name, request.description)
    return {"plan": plan}
