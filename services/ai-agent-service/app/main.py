
from fastapi import FastAPI, HTTPException
import logging

from fastapi import Depends, HTTPException

from app.config import settings
from app.dto.investigation_request import InvestigationRequest
from app.security import verify_internal_api_key
from app.services.gemini_service import GeminiService
from app.services.groq_service import GroqService
from app.services.investigation_service import summarize_history
from app.tools.history import get_historical_metrics
from app.tools.metrics import TARGETS
from app.services.gemini_service import GeminiService
from app.dto.investigation_request import InvestigationRequest
from app.tools.history import get_historical_metrics
from app.services.investigation_service import summarize_history
from app.dto.investigation_request import InvestigationRequest
from app.tools.history import get_historical_metrics
from app.services.investigation_service import summarize_history
from app.services.gemini_service import GeminiService
from app.tools.metrics import TARGETS
from fastapi import Depends
from app.services.groq_service import GroqService
from app.security import verify_internal_api_key
from app.config import settings
import logging
from app.tools.metrics import (
    get_cpu_usage,
    get_memory_usage,
    get_disk_usage,
    get_load_average
)
from app.tools.prometheus import PrometheusClient


app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    description="AI-powered incident investigation service for IncidentOps"
)

prometheus = PrometheusClient()


# =====================================
# HEALTH CHECK
# =====================================

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "ai-agent-service",
        "message": "IncidentOps AI Agent is running"
    }


# =====================================
# CURRENT PROMETHEUS ALERTS
# =====================================

@app.get("/monitoring/alerts/current")
async def get_current_alerts():

    try:
        alerts = await prometheus.get_alerts()

        return {
            "alerts": alerts
        }

    except Exception:
        raise HTTPException(
            status_code=502,
            detail="Unable to retrieve Prometheus alerts"
        )


# =====================================
# SERVER MONITORING METRICS
# =====================================

@app.get("/monitoring/{team}")
async def get_monitoring_data(team: str):

    if team not in ("unix", "dba"):
        raise HTTPException(
            status_code=400,
            detail="Team must be 'unix' or 'dba'"
        )

    try:
        return {
            "team": team,
            "cpu": await get_cpu_usage(team),
            "memory": await get_memory_usage(team),
            "disk": await get_disk_usage(team),
            "load": await get_load_average(team)
        }

    except Exception:
        raise HTTPException(
            status_code=502,
            detail="Unable to retrieve monitoring data"
        )

@app.get("/ai/test")
async def test_gemini():

    try:
        gemini = GeminiService()
        response = await gemini.test_connection()

        return {
            "status": "success",
            "provider": "Google Gemini",
            "model": settings.gemini_model,
            "response": response
        }

    except Exception as error:
        # Log the exception server-side for debugging.
        # Do not expose provider errors or secrets to API callers.
        import logging
        logging.getLogger(__name__).exception(
            "Gemini connection test failed"
        )
        raise HTTPException(
            status_code=502,
            detail="Gemini connection test failed; check server logs"
        )


@app.post("/ai/investigation/metrics")
async def get_incident_history(request: InvestigationRequest):

    if request.team not in ("unix", "dba"):
        raise HTTPException(
            status_code=400,
            detail="Unsupported monitoring team"
        )

    try:
        history = await get_historical_metrics(
        team=request.team,
        incident_time=request.starts_at
)

        evidence = summarize_history(history)

        return {
    "incident_number": request.incident_number,
    "alert_name": request.alert_name,
    "instance": request.instance,
    "history": history,
    "evidence": evidence
}

    except ValueError as error:
        raise HTTPException(
            status_code=400,
            detail=str(error)
        )

    except Exception:
        import logging
        logging.getLogger(__name__).exception(
            "Historical metric retrieval failed"
        )

        raise HTTPException(
            status_code=502,
            detail="Unable to retrieve historical monitoring data"
        )


@app.post(
    "/ai/investigate",
    dependencies=[Depends(verify_internal_api_key)]
)
async def investigate_incident(request: InvestigationRequest):
    """
    Investigate an incident using historical Prometheus
    evidence and the configured AI provider.

    Supported providers:
        - groq
        - gemini

    The provider is selected through LLM_PROVIDER
    in the environment configuration.
    """

    logger = logging.getLogger(__name__)

    # --------------------------------------------------
    # 1. Validate the monitoring team.
    # --------------------------------------------------

    if request.team not in TARGETS:
        raise HTTPException(
            status_code=400,
            detail="Unsupported monitoring team"
        )

    # --------------------------------------------------
    # 2. Validate the server identity.
    # --------------------------------------------------

    expected_instance = TARGETS[request.team]["instance"]

    if request.instance != expected_instance:
        raise HTTPException(
            status_code=400,
            detail=(
                "Incident instance does not match "
                "monitoring team"
            )
        )

    # --------------------------------------------------
    # 3. Retrieve historical Prometheus metrics.
    # --------------------------------------------------

    try:
        history = await get_historical_metrics(
            team=request.team,
            incident_time=request.starts_at
        )

    except ValueError as error:
        raise HTTPException(
            status_code=400,
            detail=str(error)
        )

    except Exception:
        logger.exception(
            "Prometheus history retrieval failed"
        )

        raise HTTPException(
            status_code=502,
            detail="Unable to retrieve Prometheus evidence"
        )

    # --------------------------------------------------
    # 4. Summarize historical monitoring evidence.
    # --------------------------------------------------

    evidence = summarize_history(history)

    # --------------------------------------------------
    # 5. Verify that usable monitoring samples exist.
    # --------------------------------------------------

    has_samples = any(
        series.get("sample_count", 0) > 0
        for series_list in evidence["metrics"].values()
        for series in series_list
    )

    if not has_samples:
        raise HTTPException(
            status_code=422,
            detail="No historical monitoring samples available"
        )

    # --------------------------------------------------
    # 6. Prepare incident context for AI investigation.
    # --------------------------------------------------

    incident = {
        "incident_number": request.incident_number,
        "alert_name": request.alert_name,
        "team": request.team,
        "instance": request.instance,
        "starts_at": request.starts_at.isoformat()
    }

    # --------------------------------------------------
    # 7. Select the configured AI provider.
    # --------------------------------------------------

    provider = settings.llm_provider.strip().lower()

    if provider not in ("groq", "gemini"):
        logger.error(
            "Unsupported LLM provider configured: %s",
            provider
        )

        raise HTTPException(
            status_code=500,
            detail="Unsupported AI provider configuration"
        )

    # --------------------------------------------------
    # 8. Generate the AI investigation report.
    # --------------------------------------------------

    try:
        if provider == "groq":

            logger.info(
                "Starting Groq investigation for incident %s",
                request.incident_number
            )

            ai_service = GroqService()

        else:

            logger.info(
                "Starting Gemini investigation for incident %s",
                request.incident_number
            )

            ai_service = GeminiService()

        report = await ai_service.generate_investigation(
            incident=incident,
            evidence=evidence
        )

        logger.info(
            "AI investigation completed for incident %s "
            "using provider %s",
            request.incident_number,
            provider
        )

    except Exception:
        logger.exception(
            "AI investigation generation failed "
            "for incident %s using provider %s",
            request.incident_number,
            provider
        )

        raise HTTPException(
            status_code=502,
            detail="AI investigation generation failed"
        )

    # --------------------------------------------------
    # 9. Return validated report and monitoring evidence.
    # --------------------------------------------------

    return {
        "incident_number": request.incident_number,
        "team": request.team,
        "instance": request.instance,
        "evidence": evidence,
        "report": report.model_dump()
    }
